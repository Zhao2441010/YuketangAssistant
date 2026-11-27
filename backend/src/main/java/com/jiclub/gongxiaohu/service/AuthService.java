package com.jiclub.gongxiaohu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.MockYktGateway;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.model.AuthModels.AuthCredential;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.AuthModels.LoginResult;
import com.jiclub.gongxiaohu.model.AuthModels.SessionInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫码登录与会话服务，打通「用户端 → 后端 → 雨课堂端」链路：
 *
 * <ol>
 *     <li>用户端请求二维码 → 后端经网关向雨课堂申请 ticket（mock 模式本地生成）</li>
 *     <li>用户微信扫码确认 → 后端长轮询拿到 user_id + session_key</li>
 *     <li>后端用本地随机 token 与 session_key 绑定，返回 token 给用户端</li>
 *     <li>用户端后续业务请求只带 token；后端解析出 session_key 再访问雨课堂</li>
 * </ol>
 *
 * <p>v1.2 起会话改为「30 天滑动有效期 + 本机加密落盘」：
 * 用户不主动退出就一直保持登录，后端重启后也能从 {@code gongxiaohu.session-store}
 * 指定的文件恢复（AES-GCM 加密，密钥由 join-signature.secret 派生）。
 * 日志仍然只出现脱敏后的学号 / token。
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    /** 本地会话 30 天；每次访问剩余不足 7 天会自动续到 30 天（滑动窗口）。 */
    private static final long SESSION_TTL_SECONDS = 30L * 24 * 60 * 60;
    /** 滑动续期阈值：剩余不足 7 天即自动延长。 */
    private static final long RENEW_THRESHOLD_SECONDS = 7L * 24 * 60 * 60;
    private static final int STORE_VERSION = 1;

    private final YktGateway gateway;
    private final AppProperties properties;
    private final org.springframework.beans.factory.ObjectProvider<
            com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi> passwordLoginApi;
    private final Map<String, LocalSession> sessions = new ConcurrentHashMap<>();

    /** 会话落盘文件；为空表示只存内存（例如测试环境）。 */
    private final Path sessionStoreFile;
    /** AES-GCM 密钥（SHA-256(join-signature.secret)）。 */
    private final byte[] storeKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Object storeLock = new Object();
    private final SecureRandom random = new SecureRandom();

    /**
     * ticket → 已签发的登录结果。用于「成功响应丢失」的补偿：同一 ticket 再轮询直接返回同一 token。
     * 只存内存（含 token 与 sessionid，属于凭据），TTL 10 分钟、上限 512 条。
     */
    private final Map<String, CachedLogin> pollResults = new ConcurrentHashMap<>();
    private static final long POLL_RESULT_TTL_MS = 10 * 60_000L;
    private static final int POLL_RESULT_MAX = 512;

    private record CachedLogin(LoginResult result, long issuedAt) {
    }

    public AuthService(YktGateway gateway, AppProperties properties,
                       org.springframework.beans.factory.ObjectProvider<
                               com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi> passwordLoginApi) {
        this.gateway = gateway;
        this.properties = properties;
        this.passwordLoginApi = passwordLoginApi;

        String store = properties.sessionStore();
        if (store != null && !store.isBlank()) {
            this.sessionStoreFile = Path.of(store).toAbsolutePath().normalize();
            String secret = properties.joinSignature() == null ? null : properties.joinSignature().secret();
            if (secret == null || secret.isBlank()) {
                log.warn("未配置 gongxiaohu.join-signature.secret，会话文件将使用默认密钥，建议尽快配置");
                secret = "gongxiaohu-session-store-default";
            }
            this.storeKey = sha256(secret);
        } else {
            this.sessionStoreFile = null;
            this.storeKey = null;
        }
    }

    /** 启动时恢复上次落盘的登录态（后端重启后用户无需重新扫码）。 */
    @PostConstruct
    void restoreSessions() {
        if (sessionStoreFile == null) return;
        synchronized (storeLock) {
            try {
                if (!Files.exists(sessionStoreFile)) return;
                String encoded = Files.readString(sessionStoreFile, StandardCharsets.UTF_8).trim();
                if (encoded.isEmpty()) return;
                StoreFile file = mapper.readValue(decrypt(encoded), StoreFile.class);
                if (file == null || file.sessions() == null) return;
                long now = System.currentTimeMillis();
                int restored = 0;
                int skipped = 0;
                for (Map.Entry<String, StoredSession> entry : file.sessions().entrySet()) {
                    StoredSession stored = entry.getValue();
                    if (stored == null || stored.expiresAt() <= now) {
                        skipped++;
                        continue;
                    }
                    sessions.put(entry.getKey(), stored.toLocal());
                    restored++;
                }
                log.info("已从会话文件恢复 {} 个登录态，跳过过期/无效 {} 个（{}）",
                        restored, skipped, sessionStoreFile);
            } catch (Exception e) {
                // 密钥变更、文件损坏都不应阻止启动；最坏情况是用户重新登录一次
                log.warn("会话文件恢复失败，将从空会话开始：{}", e.getMessage());
            }
        }
    }

    public LoginQr createLoginQr() {
        return gateway.createLoginQr();
    }

    /** 长轮询：聊天式挂起请求。扫码成功后建立本地会话并返回 token。 */
    public LoginResult pollAndLogin(String ticket) {
        return pollAndLogin(ticket, 45);
    }

    /**
     * 轮询扫码结果并按 ticket 幂等返回（2026-11-25）。
     *
     * <p>为什么必须幂等：扫码确认是"一次性事件"，而拿到 token 的那次 HTTP 响应可能根本到不了前端
     * （移动端长连接被掐断、WebView 页面重建、网络切换）。旧实现成功后立即 cleanup 掉流程，
     * 前端一旦没接住这次响应，后续轮询全是"尚未确认"，只能重新扫码——这正是
     * "后端 LOGIN_OK、手机停在二维码页"的成因。
     *
     * <p>现在把结果按 ticket 缓存 10 分钟：同一个 ticket 再轮询一次，直接返回同一个 token，
     * 前端只要还在轮询就一定能拿到登录态。
     *
     * @param waitSeconds 单次最长等待秒数，<= 0 表示短轮询（立即返回）
     */
    public LoginResult pollAndLogin(String ticket, long waitSeconds) {
        CachedLogin cached = pollResults.get(ticket);
        if (cached != null) {
            if (System.currentTimeMillis() - cached.issuedAt() < POLL_RESULT_TTL_MS) {
                log.info("扫码轮询命中缓存 ticket={} userId={}",
                        fingerprint(ticket), maskUserId(cached.result().userId()));
                return cached.result();
            }
            pollResults.remove(ticket);
        }

        long wait = Math.max(0, Math.min(waitSeconds, 45));
        AuthCredential credential = gateway.pollAuthStatus(ticket, wait)
                .orElseThrow(() -> BizException.conflict("登录超时或二维码尚未确认，请重新扫码"));

        String token = "GXH-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli();
        var ctx = credential.context();
        putSession(token, new LocalSession(credential.userId(),
                credential.sessionKey(), ctx, expiresAt));
        log.info("用户登录成功 userId={} token={} 有csrftoken={} wait={}s",
                maskUserId(credential.userId()), maskToken(token), !ctx.csrftoken().isBlank(), wait);
        com.jiclub.gongxiaohu.common.AuditLog.loginOk("qrcode", credential.userId());

        LoginResult result = new LoginResult(token, credential.userId(), credential.sessionKey(),
                expiresAt, properties.isMockMode());
        rememberLogin(ticket, result);
        return result;
    }

    /** 记住 ticket → 登录结果，供同 ticket 重复轮询（响应丢失后的补偿取回）。 */
    private void rememberLogin(String ticket, LoginResult result) {
        pollResults.put(ticket, new CachedLogin(result, System.currentTimeMillis()));
        if (pollResults.size() > POLL_RESULT_MAX) {
            long now = System.currentTimeMillis();
            pollResults.entrySet().removeIf(e -> now - e.getValue().issuedAt() >= POLL_RESULT_TTL_MS);
            // 仍超量则按最早签发时间淘汰，避免内存无界增长
            while (pollResults.size() > POLL_RESULT_MAX) {
                pollResults.entrySet().stream()
                        .min(java.util.Comparator.comparingLong(e -> e.getValue().issuedAt()))
                        .map(Map.Entry::getKey)
                        .ifPresent(pollResults::remove);
            }
        }
    }

    private static String fingerprint(String ticket) {
        if (ticket == null || ticket.isBlank()) return "-";
        return ticket.length() <= 8 ? ticket : ticket.substring(0, 8);
    }

    /**
     * 用已有的真实 sessionid 直接建立本地会话（对应开源 MCP 的 YKT_SESSIONID 能力）。
     * 适用于：已通过网页/小程序扫码登录、持有有效 sessionid 的用户，跳过重复扫码。
     *
     * @param sessionId 雨课堂 sessionid
     * @param userId    可选学号；为空时标记为 imported
     */
    public LoginResult importSession(String sessionId, String userId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw BizException.badRequest("sessionid 不能为空");
        }
        if (properties.isMockMode()) {
            throw BizException.badRequest("演示模式请使用模拟登录，无需导入 sessionid");
        }
        String token = "GXH-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli();
        String uid = userId == null || userId.isBlank() ? "imported" : userId;
        putSession(token, new LocalSession(uid, sessionId, expiresAt));
        log.info("已导入真实会话 token={} userId={}", maskToken(token), maskUserId(uid));
        com.jiclub.gongxiaohu.common.AuditLog.loginOk("import-session", uid);
        return new LoginResult(token, uid, sessionId, expiresAt, false);
    }

    /**
     * §6.2 ①：导入整串 Cookie，持久化完整 L1 登录态。
     *
     * <p>课堂凭证换取（checkin）需要 csrftoken / X-UID / university-id / uv-id，
     * 仅有 sessionid 会得到 401 / 50000。用户可从浏览器 DevTools 复制整串 Cookie 导入。
     * Cookie 不写日志、不回传前端；v1.2 起随会话一起加密落盘（仅本机文件）。
     */
    public LoginResult importCookie(String cookie, String userId) {
        if (cookie == null || cookie.isBlank()) {
            throw BizException.badRequest("Cookie 不能为空");
        }
        com.jiclub.gongxiaohu.model.AuthModels.SessionContext ctx =
                com.jiclub.gongxiaohu.model.AuthModels.SessionContext.fromCookie(cookie, userId);
        if (ctx.sessionId().isBlank()) {
            throw BizException.badRequest("Cookie 中未找到 sessionid，请确认复制完整");
        }
        String token = "GXH-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli();
        String uid = ctx.userId().isBlank() ? "imported" : ctx.userId();
        putSession(token, new LocalSession(uid, ctx.sessionId(), ctx, expiresAt));
        log.info("已导入完整 L1 会话 userId={} 有csrftoken={} 有uvId={}",
                maskUserId(uid), !ctx.csrftoken().isBlank(), !ctx.uvId().isBlank());
        com.jiclub.gongxiaohu.common.AuditLog.loginOk("import-cookie", uid);
        return new LoginResult(token, uid, ctx.sessionId(), expiresAt, false);
    }

    /** 账号密码登录：真实模式调官方接口，成功后建立本地会话。 */
    public LoginResult loginByPassword(String account, String password, String type,
                                       String ticket, String randstr) {
        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            throw BizException.badRequest("账号和密码不能为空");
        }
        String userId;
        String sessionId;
        if (properties.isMockMode()) {
            // 演示模式：任意账号密码均可，固定生成一个学号与会话
            userId = "20" + String.valueOf(Math.abs(account.hashCode())).substring(0, 8);
            sessionId = "mock-session-" + UUID.randomUUID().toString().substring(0, 8);
        } else {
            com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi api = passwordLoginApi.getIfAvailable();
            if (api == null) {
                throw BizException.badRequest("账号密码登录组件未就绪");
            }
            com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi.LoginOutcome outcome =
                    api.login(account, password, type, ticket, randstr);
            if (!outcome.ok()) {
                com.jiclub.gongxiaohu.common.AuditLog.loginFail("password", account, outcome.message());
                throw BizException.badRequest(outcome.message());
            }
            userId = outcome.userId();
            sessionId = outcome.sessionId();
        }
        String token = "GXH-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli();
        String uid = userId.isBlank() ? "imported" : userId;
        putSession(token, new LocalSession(uid, sessionId, expiresAt));
        log.info("账号密码登录成功 userId={} token={}", maskUserId(uid), maskToken(token));
        com.jiclub.gongxiaohu.common.AuditLog.loginOk("password", uid);
        return new LoginResult(token, uid, sessionId, expiresAt, properties.isMockMode());
    }

    /** 演示模式：模拟手机端点击「确认登录」，触发长轮询立即返回。 */
    public boolean mockConfirm(String ticket) {
        if (!properties.isMockMode() || !(gateway instanceof MockYktGateway mock)) {
            throw BizException.badRequest("仅演示模式（mock）支持模拟扫码确认");
        }
        return mock.confirmScan(ticket);
    }

    /** 用本地 token 换取雨课堂 session_key；token 无效或过期抛 401。成功时按需滑动续期。 */
    public String resolveSessionKey(String token) {
        if (token == null || token.isBlank()) {
            throw BizException.badRequest("缺少登录凭据，请先扫码登录");
        }
        LocalSession session = sessions.get(token);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) {
            removeSession(token);
            throw new BizException(401, "登录已过期，请重新扫码登录");
        }
        maybeRenew(token, session);
        return session.sessionKey();
    }

    /** 取完整 L1 上下文（课堂凭证换取用）；token 无效抛 401。 */
    public com.jiclub.gongxiaohu.model.AuthModels.SessionContext resolveContext(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException(401, "缺少登录凭据，请扫码登录");
        }
        LocalSession session = sessions.get(token);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) {
            removeSession(token);
            throw new BizException(401, "登录已过期，请重新扫码登录");
        }
        maybeRenew(token, session);
        com.jiclub.gongxiaohu.model.AuthModels.SessionContext ctx = session.context();
        // 老登录链路（账号密码 / import-session）只把 userId 存进 LocalSession，
        // context 仍是 minimal（userId 空）→ lesson_info_v2 发不出 X-UID → 换不出 userAuth。
        // 这里从 LocalSession 兜底补上，保证进班接口能带出 X-UID。
        if (ctx.userId().isBlank() && session.userId() != null && !session.userId().isBlank()
                && !"imported".equals(session.userId())) {
            ctx = new com.jiclub.gongxiaohu.model.AuthModels.SessionContext(
                    ctx.sessionId(), ctx.csrftoken(), session.userId(),
                    ctx.universityId(), ctx.uvId(), ctx.classroomId(), ctx.rawCookie());
        }
        return ctx;
    }

    /** 滑动续期：剩余不足 7 天时自动延长到 30 天，用户无感。 */
    private void maybeRenew(String token, LocalSession session) {
        long remaining = session.expiresAt() - System.currentTimeMillis();
        if (remaining < RENEW_THRESHOLD_SECONDS * 1000) {
            putSession(token, session.renewed(
                    Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli()));
            log.debug("会话自动续期 token={}", maskToken(token));
        }
    }

    /** 显式续期：返回续期后的会话信息（供前端定时调用）。 */
    public SessionInfo renew(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException(401, "缺少登录凭据，请重新扫码登录");
        }
        LocalSession session = sessions.get(token);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) {
            removeSession(token);
            throw new BizException(401, "登录已过期，请重新扫码登录");
        }
        maybeRenew(token, session);
        return toSessionInfo(token, sessions.get(token));
    }

    /**
     * 只读查询：按本地 token 反查学号，不校验续期、不抛异常。
     * 供访问日志标注"这次请求是谁发的"，token 无效时返回 null。
     */
    public String peekUserId(String token) {
        if (token == null || token.isBlank()) return null;
        LocalSession session = sessions.get(token);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) return null;
        return session.userId();
    }

    public SessionInfo sessionInfo(String token) {
        LocalSession session = token == null ? null : sessions.get(token);
        if (session == null) {
            return new SessionInfo(token, null, 0, false, 0, false);
        }
        return toSessionInfo(token, session);
    }

    private static SessionInfo toSessionInfo(String token, LocalSession session) {
        boolean valid = session.expiresAt() >= System.currentTimeMillis();
        long remaining = Math.max(0, (session.expiresAt() - System.currentTimeMillis()) / 1000);
        // 有 sessionKey 即可续期（本地滑动窗口）；真实 sessionid 失效由上游 401 时前端无感重导处理
        boolean renewable = session.sessionKey() != null && !session.sessionKey().isBlank();
        return new SessionInfo(token, session.userId(), session.expiresAt(), valid, remaining, renewable);
    }

    public void logout(String token) {
        removeSession(token);
    }

    // ==================== 会话落盘（v1.2 持久登录） ====================

    /** 所有新增/更新会话的唯一入口，保证内存与磁盘一致。 */
    private void putSession(String token, LocalSession session) {
        sessions.put(token, session);
        persistSessions();
    }

    /** 所有删除会话的唯一入口；token 不存在时不触发写盘。 */
    private void removeSession(String token) {
        if (token == null) return;
        if (sessions.remove(token) != null) {
            persistSessions();
        }
    }

    /** 把当前有效会话加密写入文件（先写临时文件再原子替换，避免半截文件）。 */
    private void persistSessions() {
        if (sessionStoreFile == null) return;
        synchronized (storeLock) {
            try {
                long now = System.currentTimeMillis();
                Map<String, StoredSession> data = new HashMap<>();
                for (Map.Entry<String, LocalSession> entry : sessions.entrySet()) {
                    LocalSession session = entry.getValue();
                    if (session != null && session.expiresAt() > now) {
                        data.put(entry.getKey(), StoredSession.from(session));
                    }
                }
                byte[] json = mapper.writeValueAsBytes(new StoreFile(STORE_VERSION, now, data));
                String encoded = encrypt(json);

                Path parent = sessionStoreFile.getParent();
                if (parent != null) Files.createDirectories(parent);
                Path tmp = sessionStoreFile.resolveSibling(sessionStoreFile.getFileName() + ".tmp");
                Files.writeString(tmp, encoded, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
                try {
                    Files.move(tmp, sessionStoreFile,
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp, sessionStoreFile, StandardCopyOption.REPLACE_EXISTING);
                }
                restrictPermissions(sessionStoreFile);
            } catch (Exception e) {
                log.warn("会话落盘失败（不影响本次使用）：{}", e.getMessage());
            }
        }
    }

    /** AES-GCM 加密：输出 base64(iv || ciphertext)。 */
    private String encrypt(byte[] plain) throws Exception {
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(storeKey, "AES"), new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plain);
        byte[] out = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
        return Base64.getEncoder().encodeToString(out);
    }

    private byte[] decrypt(String encoded) throws Exception {
        byte[] raw = Base64.getDecoder().decode(encoded);
        if (raw.length <= 12) throw new IllegalArgumentException("会话文件格式不正确");
        byte[] iv = Arrays.copyOfRange(raw, 0, 12);
        byte[] cipherText = Arrays.copyOfRange(raw, 12, raw.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(storeKey, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherText);
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化会话加密密钥", e);
        }
    }

    /** Linux 上把文件权限收紧到 600；Windows 不支持 POSIX 权限时静默跳过。 */
    private static void restrictPermissions(Path file) {
        try {
            if (Files.getFileStore(file).supportsFileAttributeView(PosixFileAttributeView.class)) {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            }
        } catch (Exception ignored) {
            // 权限收紧失败不影响功能
        }
    }

    /** 落盘用的扁平化会话结构（避免直接序列化内部 record）。 */
    public record StoredSession(
            String userId, String sessionKey, String sessionId, String csrftoken,
            String contextUserId, String universityId, String uvId,
            String classroomId, String rawCookie, long expiresAt) {

        static StoredSession from(LocalSession session) {
            var ctx = session.context();
            return new StoredSession(session.userId(), session.sessionKey(), ctx.sessionId(),
                    ctx.csrftoken(), ctx.userId(), ctx.universityId(), ctx.uvId(),
                    ctx.classroomId(), ctx.rawCookie(), session.expiresAt());
        }

        LocalSession toLocal() {
            return new LocalSession(userId, sessionKey,
                    new com.jiclub.gongxiaohu.model.AuthModels.SessionContext(
                            sessionId, csrftoken, contextUserId, universityId, uvId,
                            classroomId, rawCookie),
                    expiresAt);
        }
    }

    /** 会话文件格式。 */
    public record StoreFile(int version, long savedAt, Map<String, StoredSession> sessions) {
    }

    private static String maskUserId(String id) {
        return id == null || id.length() < 4 ? "****" : id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }

    private static String maskToken(String token) {
        return token.substring(0, 7) + "****";
    }

    /**
     * 本地会话（L2）。context 保存 L1 登录态的完整字段，供课堂凭证换取时使用；
     * 旧链路通过 3 参数构造器退化为仅 sessionid 的最小上下文。
     */
    private record LocalSession(String userId, String sessionKey,
                                com.jiclub.gongxiaohu.model.AuthModels.SessionContext context,
                                long expiresAt) {
        LocalSession(String userId, String sessionKey, long expiresAt) {
            this(userId, sessionKey,
                    com.jiclub.gongxiaohu.model.AuthModels.SessionContext.minimal(sessionKey),
                    expiresAt);
        }

        /** 续期时保留 context，避免丢失 csrftoken 等 L1 字段。 */
        LocalSession renewed(long newExpiresAt) {
            return new LocalSession(userId, sessionKey, context, newExpiresAt);
        }
    }
}
