package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.AuthModels.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自渲染增强的可选组件：课堂态凭证换取客户端（技术说明 §7 定位纠正）。
 *
 * <p><b>本类不是进课堂的前置条件</b>。官方在读学生凭 L1 Cookie 打开
 * {@code /lesson/fullscreen/v3/{lessonId}?source=5} 即可建立课堂会话，
 * 不经过 {@code /api/v3/lesson/checkin}。本类调用的 checkin 本质是<b>签到动作</b>，
 * 仅为「在 App 内自绘课件/题目」提供 pptJwt；动态码开启时返回 50070
 * 只表示这个签到未被接受，<b>不代表进不了课堂</b>。
 *
 * <p>换取的是每节课独立、短时效的「课堂态凭证」三元组：
 * <pre>
 *   POST /api/v3/lesson/checkin  (带完整 L1 会话头 + lessonId)
 *   → 响应头 Set-Auth: <b>pptJwt</b>        后续课堂 API 的 Authorization: Bearer
 *   → data.lessonToken: <b>socketJwt</b>   WebSocket 实时通道
 *   → data.identityId:  <b>identityId</b>  课堂内身份
 * </pre>
 *
 * <p>官方 pc.js 的 axios 响应拦截器把 set-auth 写入 window.Authorization，
 * 后续请求统一带 Bearer。本类在后端复刻该链路，凭证<b>只留在后端内存</b>，
 * 前端仅拿到 entryId，不接触任何 jwt / 登录态。
 *
 * <p><b>边界</b>：不触碰、不绕过签到动态码（官方防代签设计）；
 * 50070 只影响自渲染增强，上层仍可正常进课堂会话。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class LessonCredentialClient {
    private static final Logger log = LoggerFactory.getLogger(LessonCredentialClient.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";

    /** 凭证有效期：单节课内复用，保守设 2 小时；下课(50004)会主动清除。 */
    private static final long CREDENTIAL_TTL_MS = 2 * 60 * 60 * 1000L;

    /** checkin source 语义（§6.3 ②）：21二维码 / 23APP / 5公众号 / 22暗号。按顺序回退。 */
    private static final int[] SOURCE_FALLBACKS = {21, 23, 5, 22};

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final LessonDiagnostic diagnostic;
    /** key = sessionId#lessonId，value = 三元组凭证。仅内存，进程重启即失效。 */
    private final Map<String, LessonCredential> cache = new ConcurrentHashMap<>();

    public LessonCredentialClient(AppProperties properties, ObjectMapper mapper,
                                  LessonDiagnostic diagnostic) {
        this.mapper = mapper;
        this.diagnostic = diagnostic;
        String configured = properties.yktGateway() == null ? null : properties.yktGateway().baseUrl();
        this.baseUrl = (configured == null || configured.isBlank())
                ? "https://changjiang.yuketang.cn" : configured;
    }

    /** 课堂凭证三元组（L3）。pptJwt 是自渲染课件/题目的 Bearer。 */
    public record LessonCredential(
            String lessonId,
            String pptJwt,
            String socketJwt,
            String identityId,
            long expiresAt
    ) {
        public boolean valid() {
            return pptJwt != null && !pptJwt.isBlank() && expiresAt > System.currentTimeMillis();
        }
    }

    /**
     * 换取（或复用缓存）课堂凭证。
     *
     * @throws BizException 50070 动态码签到被拒（上层应降级看题）、401/50000 会话失效、50004 课堂结束
     */
    public LessonCredential enterLesson(SessionContext ctx, String lessonId) {
        if (lessonId == null || lessonId.isBlank()) {
            throw BizException.badRequest("lessonId 不能为空");
        }
        String key = ctx.sessionId() + "#" + lessonId;
        LessonCredential cached = cache.get(key);
        if (cached != null && cached.valid()) {
            log.debug("复用课堂凭证 lessonId={}", lessonId);
            return cached;
        }

        BizException lastFailure = null;
        for (int source : SOURCE_FALLBACKS) {
            try {
                LessonCredential credential = checkin(ctx, lessonId, source);
                cache.put(key, credential);
                log.info("课堂凭证换取成功 lessonId={} source={} 有socketJwt={} 有identityId={}",
                        lessonId, source, !credential.socketJwt().isBlank(),
                        !credential.identityId().isBlank());
                return credential;
            } catch (BizException e) {
                lastFailure = e;
                // 动态码签到拒绝：所有 source 一致，无需逐个试，直接交给上层降级
                if (e.getMessage() != null && e.getMessage().contains("50070")) throw e;
                // 会话失效 / 课堂结束：回退 source 无意义
                if (e.getCode() == 401 || (e.getMessage() != null
                        && (e.getMessage().contains("50000") || e.getMessage().contains("50004")))) {
                    throw e;
                }
                log.debug("checkin source={} 失败，尝试下一个：{}", source, e.getMessage());
            }
        }
        throw lastFailure != null ? lastFailure
                : BizException.conflict("课堂凭证换取失败（已尝试全部 source）");
    }

    /** 单次 checkin 请求，解析三元组、失败矩阵，并完整记录验证诊断。 */
    private LessonCredential checkin(SessionContext ctx, String lessonId, int source) {
        long started = System.currentTimeMillis();
        String url = baseUrl + "/api/v3/lesson/checkin";

        // 记录实际发出的头（脱敏：只记有无，敏感值只留长度指纹）
        Map<String, String> rawHeaders = new LinkedHashMap<>();
        rawHeaders.put("Cookie", ctx.cookieHeader());
        rawHeaders.put("sessionid", ctx.sessionId());
        rawHeaders.put("X-CSRFToken", ctx.csrftoken());
        rawHeaders.put("X-UID", ctx.userId());
        rawHeaders.put("classroom-id", ctx.classroomId());
        rawHeaders.put("university-id", ctx.universityId());
        rawHeaders.put("uv-id", ctx.uvId());
        rawHeaders.put("X-Client", "web");
        rawHeaders.put("xtbz", "ykt");
        List<String> missing = new ArrayList<>();
        Map<String, String> safeHeaders = LessonDiagnostic.headers(rawHeaders, missing);

        // 诊断记录的可变部分（record 不可变，用局部变量在结束时统一落库）
        final int[] httpStatus = {-1};
        final int[] bizCode = {-1};
        final String[] bizMsg = {""};
        final boolean[] setAuthPresent = {false};
        final String[] setAuthFp = {"无"};
        final boolean[] socketPresent = {false};
        final boolean[] identityPresent = {false};
        final String[] outcome = {"UNKNOWN"};

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("source", source);
            body.put("lessonId", lessonId);
            body.put("joinIfNotIn", true);

            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/json")
                    .header("Cookie", ctx.cookieHeader())
                    .header("sessionid", ctx.sessionId())          // Android 逆向：缺它即 401/50000
                    .header("X-Client", "web")
                    .header("xtbz", "ykt")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            putIfPresent(req, "X-CSRFToken", ctx.csrftoken());
            putIfPresent(req, "X-UID", ctx.userId());
            putIfPresent(req, "classroom-id", ctx.classroomId());
            putIfPresent(req, "university-id", ctx.universityId());
            putIfPresent(req, "uv-id", ctx.uvId());

            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            httpStatus[0] = resp.statusCode();
            JsonNode root = mapper.readTree(resp.body());
            int code = root.path("code").asInt(0);
            String msg = root.path("msg").asText(root.path("message").asText(""));
            bizCode[0] = code;
            bizMsg[0] = msg;

            String pptJwt = resp.headers().firstValue("Set-Auth")
                    .orElseGet(() -> resp.headers().firstValue("set-auth").orElse(""));
            setAuthPresent[0] = !pptJwt.isBlank();
            setAuthFp[0] = LessonDiagnostic.fingerprint(pptJwt);
            JsonNode data = root.path("data");
            String socketJwt = text(data, "lessonToken", "socketJwt", "socket_token");
            String identityId = text(data, "identityId", "identity_id");
            socketPresent[0] = !socketJwt.isBlank();
            identityPresent[0] = !identityId.isBlank();

            if (resp.statusCode() == 401 || code == 50000) {
                outcome[0] = "AUTH_EXPIRED";
                throw new BizException(401,
                        "会话失效(401/50000)：请重新导入完整 Cookie（含 csrftoken / X-UID / uv-id）");
            }
            if (code == 50070) {
                // 50070 在官方 pc.js 的 checkin（notkn/checkin 暗号加班级）里对应
                // lessonEndMsg「老师已经下课了，不能加入课堂」；在本接口实测为
                // DYNAMIC_QR_CHECK_IN_REFUSED。两者都只说明「这个签到动作没被接受」，
                // 不代表进不了课堂会话 —— 在读学生固定入口不经过此接口。
                outcome[0] = "CHECKIN_REFUSED";
                throw BizException.conflict(
                        "50070 签到动作未被接受（动态码签到或课堂已结束）；不影响进课堂会话，仅自渲染增强不可用");
            }
            if (code == 50004) {
                outcome[0] = "LESSON_END";
                cache.keySet().removeIf(k -> k.endsWith("#" + lessonId));
                throw BizException.conflict("50004 课堂已结束，凭证缓存已清除");
            }
            if (code == 50030 || code == 50080) {
                outcome[0] = "BAD_LESSON_ID";
                throw BizException.conflict("50030/50080 lessonId 类型错误：进行中课堂请用 on-lesson 返回的 lessonId");
            }
            if (code == 50019) {
                outcome[0] = "NEED_BIND";
                throw BizException.conflict("50019 未完成学校身份绑定，请先在雨课堂完成绑定");
            }
            if (code != 0) {
                outcome[0] = "BIZ_ERROR_" + code;
                throw BizException.conflict("checkin 失败 code=" + code + " " + msg);
            }
            if (pptJwt.isBlank()) {
                outcome[0] = "NO_SET_AUTH";
                throw BizException.conflict("checkin 成功但未返回 Set-Auth(pptJwt)，无法自渲染课件");
            }
            outcome[0] = "SUCCESS";
            return new LessonCredential(lessonId, pptJwt, socketJwt, identityId,
                    System.currentTimeMillis() + CREDENTIAL_TTL_MS);
        } catch (BizException e) {
            if ("UNKNOWN".equals(outcome[0])) outcome[0] = "BIZ_EXCEPTION";
            throw e;
        } catch (Exception e) {
            outcome[0] = "NETWORK_ERROR";
            bizMsg[0] = e.getMessage() == null ? "" : e.getMessage();
            throw BizException.conflict("checkin 请求异常：" + e.getMessage());
        } finally {
            // 无论成功失败都落一条诊断，供 §6.7 验证闭环
            diagnostic.record(new LessonDiagnostic.Record(
                    LessonDiagnostic.now(), lessonId, source, url,
                    safeHeaders, List.copyOf(missing),
                    httpStatus[0], bizCode[0], bizMsg[0],
                    setAuthPresent[0], setAuthFp[0],
                    socketPresent[0], identityPresent[0],
                    outcome[0], System.currentTimeMillis() - started));
        }
    }

    /** 供随堂题 API 复用：只取 pptJwt（不暴露完整凭证给其他层）。 */
    public String pptJwt(SessionContext ctx, String lessonId) {
        return enterLesson(ctx, lessonId).pptJwt();
    }

    /** 登出或会话失效时清除该会话的全部凭证。 */
    public void evict(String sessionId) {
        cache.keySet().removeIf(k -> k.startsWith(sessionId + "#"));
    }

    private static void putIfPresent(HttpRequest.Builder req, String name, String value) {
        if (value != null && !value.isBlank()) req.header(name, value);
    }

    private static String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return "";
    }
}
