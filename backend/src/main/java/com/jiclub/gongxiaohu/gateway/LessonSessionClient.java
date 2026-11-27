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
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 免扫码进课堂会话（工小湖博客 §1 + example1.py + course_helper 三源交叉验证）。
 *
 * <p>正确链路是「checkin 换课堂凭证 → WSS hello 建会话」，<b>不是</b> lesson_info_v2 的 userAuth：
 * <ol>
 *     <li>POST {@code /api/v3/lesson/checkin} 换 {@code data.lessonToken}(=socketJwt) + {@code data.identityId}
 *         + 响应头 {@code Set-Auth}(=pptJwt) —— 已由 {@link LessonCredentialClient} 实现；</li>
 *     <li>WSS {@code /wsapp/} 发 5 字段 hello：{@code {op:"hello", userid:identityId, role:"student",
 *         auth:lessonToken, lessonid:lessonId}} 建立实时会话。</li>
 * </ol>
 *
 * <p>两个独立开源实现（example1.py、course_helper）完全一致：hello 是 5 字段、auth=lessonToken、
 * userid=identityId，无 ver/avatar/presentation。服务端收到 hello 后回 showpresentation / hello 等帧，
 * 即代表课堂会话已建立。
 *
 * <p><b>以参考项目 course_helper 为准</b>：WSS hello 的 {@code userid} 用登录态全局 user_id
 * （即 {@link SessionContext#userId()}，对应 {@code /v/course_meta/user_info} 的
 * {@code user_profile.user_id}），<b>不是</b> checkin 返回的 identityId（那是课堂内临时身份）。
 * 若 SessionContext.userId 缺失才退回 identityId 兜底。auth 仍用 checkin 的 lessonToken(=socketJwt)。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class LessonSessionClient {
    private static final Logger log = LoggerFactory.getLogger(LessonSessionClient.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";
    /** 发出 hello 后等待服务端首帧的秒数；超时视为「已连上但课堂暂无推送」。 */
    private static final int HELLO_ACK_TIMEOUT_SECONDS = 8;

    private final LessonCredentialClient credentialClient;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String wsUrl;
    private final String baseUrl;

    public LessonSessionClient(LessonCredentialClient credentialClient, ObjectMapper mapper,
                               AppProperties properties) {
        this.credentialClient = credentialClient;
        this.mapper = mapper;
        AppProperties.YktGateway cfg = properties.yktGateway();
        this.wsUrl = cfg != null && cfg.wsUrl() != null && !cfg.wsUrl().isBlank()
                ? cfg.wsUrl() : "wss://changjiang.yuketang.cn/wsapp/";
        this.baseUrl = cfg != null && cfg.baseUrl() != null && !cfg.baseUrl().isBlank()
                ? cfg.baseUrl() : "https://changjiang.yuketang.cn";
        this.http = HttpClient.newHttpClient();
    }

    /** 免扫码进班结果。serverOp 为服务端回的首帧 op（如 showpresentation / hello），空串表示暂未推送。 */
    public record EnterResult(
            String lessonId,
            String courseName,
            boolean entered,
            String serverOp,
            int code,
            String msg,
            String note
    ) {}

    /**
     * 凭登录态免扫码进入课堂会话。
     *
     * <p>成功判定：checkin 换到 lessonToken+identityId 且 WSS hello 发出后服务端回帧（或连接保持）。
     * 动态码课堂若 checkin 返回 50070 且不下发 lessonToken，会如实返回失败，不做绕过。
     */
    public EnterResult enter(SessionContext ctx, YktGateway.CourseInfo lesson) {
        String lessonId = lesson == null ? "" : lesson.lessonId();
        String courseName = lesson == null ? "" : lesson.courseName();
        // 真实 lessonId 是雪花 ID；"lesson-" 前缀说明网关只匹配到课表、课堂未进行中
        if (lessonId == null || lessonId.isBlank() || lessonId.startsWith("lesson-")) {
            return new EnterResult(lessonId, courseName, false, "", 409, "",
                    "该课程当前未开课，无法免扫码进班");
        }

        LessonCredentialClient.LessonCredential credential;
        try {
            credential = credentialClient.enterLesson(ctx, lessonId);
        } catch (BizException e) {
            return new EnterResult(lessonId, courseName, false, "",
                    e.getCode(), e.getMessage(), e.getMessage());
        }

        String lessonToken = credential.socketJwt();
        String identityId = credential.identityId();
        if (lessonToken == null || lessonToken.isBlank()) {
            return new EnterResult(lessonId, courseName, false, "", 500, "",
                    "checkin 成功但未返回 lessonToken，无法建立课堂会话");
        }
        // userid：course_helper 用登录态 user_id(=SessionContext.userId)，example1.py 用 identityId。
        // 以参考项目 course_helper 为准，userId 缺失时退回 identityId 兜底。
        String userid = (ctx.userId() != null && !ctx.userId().isBlank())
                ? ctx.userId() : identityId;
        if (userid == null || userid.isBlank()) {
            return new EnterResult(lessonId, courseName, false, "", 500, "",
                    "checkin 成功但未返回 userid/identityId，无法建立课堂会话");
        }

        try {
            String serverOp = establishSession(userid, lessonToken, lessonId);
            log.info("免扫码进班成功 lessonId={} courseName={} serverOp={}",
                    lessonId, courseName, serverOp.isBlank() ? "(无首帧)" : serverOp);
            return new EnterResult(lessonId, courseName, true, serverOp, 0, "",
                    "已免扫码进入课堂会话（checkin 换 lessonToken → WSS hello）"
                            + (serverOp.isBlank() ? "" : "，服务端首帧 op=" + serverOp));
        } catch (Exception e) {
            log.warn("免扫码进班 WSS 建会话失败 lessonId={}: {}", lessonId, e.getMessage());
            return new EnterResult(lessonId, courseName, false, "", 500, "",
                    "建立课堂会话失败：" + e.getMessage());
        }
    }

    /** 连接 wsapp、发 5 字段 hello，返回服务端首帧 op（超时返回空串）。 */
    private String establishSession(String userid, String lessonToken, String lessonId) throws Exception {
        String helloJson = helloPayload(mapper, userid, lessonToken, lessonId);
        CompletableFuture<String> firstOp = new CompletableFuture<>();
        WebSocket ws = http.newWebSocketBuilder()
                .header("Origin", baseUrl)
                .header("User-Agent", UA)
                .buildAsync(URI.create(wsUrl), listener(firstOp))
                .get(20, TimeUnit.SECONDS);
        try {
            ws.sendText(helloJson, true).get(5, TimeUnit.SECONDS);
            try {
                return firstOp.get(HELLO_ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                // 连接保持、hello 已发出，但课堂暂无推送帧 —— 仍视为会话已建立
                return "";
            }
        } finally {
            closeQuietly(ws);
        }
    }

    /** 构建 5 字段 hello 负载（包可见，便于单测）。auth=lessonToken、userid=登录态 user_id。 */
    static String helloPayload(ObjectMapper mapper, String userid, String auth, String lessonid) {
        ObjectNode n = mapper.createObjectNode();
        n.put("op", "hello");
        n.put("userid", userid == null ? "" : userid);
        n.put("role", "student");
        n.put("auth", auth == null ? "" : auth);
        n.put("lessonid", lessonid == null ? "" : lessonid);
        return n.toString();
    }

    private WebSocket.Listener listener(CompletableFuture<String> firstOp) {
        return new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void onOpen(WebSocket ws) {
                ws.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    String message = buffer.toString();
                    buffer.setLength(0);
                    if (!firstOp.isDone()) {
                        try {
                            JsonNode node = mapper.readTree(message);
                            firstOp.complete(node.path("op").asText(""));
                        } catch (Exception e) {
                            firstOp.complete("");
                        }
                    }
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                // 服务端在 hello 后主动关闭连接 = 认证被拒（lessonToken 无效）
                if (!firstOp.isDone()) {
                    firstOp.completeExceptionally(
                            new IllegalStateException("服务端关闭连接 code=" + statusCode + " reason=" + reason));
                }
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                if (!firstOp.isDone()) {
                    firstOp.completeExceptionally(error);
                }
            }
        };
    }

    private static void closeQuietly(WebSocket ws) {
        if (ws != null) {
            try { ws.abort(); } catch (Exception ignored) {}
        }
    }
}
