package com.jiclub.gongxiaohu.service;

import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.MockYktGateway;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.model.AuthModels.AuthCredential;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.AuthModels.LoginResult;
import com.jiclub.gongxiaohu.model.AuthModels.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
 * <p>session_key 仅存内存，不落库、不写日志（呼应博客第 7 节隐私防线）。
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long SESSION_TTL_SECONDS = 2 * 60 * 60; // 本地会话 2 小时
    /** 滑动续期阈值：剩余不足 1 小时即自动延长（配合前端无感续期）。 */
    private static final long RENEW_THRESHOLD_SECONDS = 60 * 60;

    private final YktGateway gateway;
    private final AppProperties properties;
    private final org.springframework.beans.factory.ObjectProvider<
            com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi> passwordLoginApi;
    private final Map<String, LocalSession> sessions = new ConcurrentHashMap<>();

    public AuthService(YktGateway gateway, AppProperties properties,
                       org.springframework.beans.factory.ObjectProvider<
                               com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi> passwordLoginApi) {
        this.gateway = gateway;
        this.properties = properties;
        this.passwordLoginApi = passwordLoginApi;
    }

    public LoginQr createLoginQr() {
        return gateway.createLoginQr();
    }

    /** 长轮询：聊天式挂起请求。扫码成功后建立本地会话并返回 token。 */
    public LoginResult pollAndLogin(String ticket) {
        AuthCredential credential = gateway.pollAuthStatus(ticket)
                .orElseThrow(() -> BizException.conflict("登录超时或二维码尚未确认，请重新扫码"));

        String token = "GXH-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli();
        sessions.put(token, new LocalSession(credential.userId(), credential.sessionKey(), expiresAt));
        log.info("用户登录成功 userId={} token={}",
                maskUserId(credential.userId()), maskToken(token));
        return new LoginResult(token, credential.userId(), credential.sessionKey(),
                expiresAt, properties.isMockMode());
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
        sessions.put(token, new LocalSession(uid, sessionId, expiresAt));
        log.info("已导入真实会话 token={} userId={}", maskToken(token), maskUserId(uid));
        return new LoginResult(token, uid, sessionId, expiresAt, false);
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
                throw BizException.badRequest(outcome.message());
            }
            userId = outcome.userId();
            sessionId = outcome.sessionId();
        }
        String token = "GXH-" + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli();
        String uid = userId.isBlank() ? "imported" : userId;
        sessions.put(token, new LocalSession(uid, sessionId, expiresAt));
        log.info("账号密码登录成功 userId={} token={}", maskUserId(uid), maskToken(token));
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
            sessions.remove(token);
            throw new BizException(401, "登录已过期，请重新扫码登录");
        }
        maybeRenew(token, session);
        return session.sessionKey();
    }

    /** 滑动续期：剩余不足阈值时自动延长 2 小时，用户无感。 */
    private void maybeRenew(String token, LocalSession session) {
        long remaining = session.expiresAt() - System.currentTimeMillis();
        if (remaining < RENEW_THRESHOLD_SECONDS * 1000) {
            LocalSession renewed = new LocalSession(session.userId(), session.sessionKey(),
                    Instant.now().plusSeconds(SESSION_TTL_SECONDS).toEpochMilli());
            sessions.put(token, renewed);
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
            sessions.remove(token);
            throw new BizException(401, "登录已过期，请重新扫码登录");
        }
        maybeRenew(token, session);
        return toSessionInfo(token, sessions.get(token));
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
        if (token != null) sessions.remove(token);
    }

    private static String maskUserId(String id) {
        return id == null || id.length() < 4 ? "****" : id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }

    private static String maskToken(String token) {
        return token.substring(0, 7) + "****";
    }

    private record LocalSession(String userId, String sessionKey, long expiresAt) {}
}
