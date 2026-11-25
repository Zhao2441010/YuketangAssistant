package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.AuthModels.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 免扫码进班客户端（真实模式）：<b>在读学生凭登录态建立官方课堂会话</b>。
 *
 * <p>对应「正确实现链路.md」的官方学生端真实进班路径（以该文档为准，推翻早期靠 Cookie
 * 直接进班的推导）：
 * <ol>
 *     <li>GET {@code /v/lesson/lesson_info_v2?lesson_id={lessonId}&source=5}
 *         —— 带全身份头（X-UID / X-CSRFToken / university-id / uv-id / classroom-id），
 *         返回 {@code data.userAuth}（课堂凭据，缺身份头即 500）；</li>
 *     <li>连 {@code wss://{region}.yuketang.cn/wsapp/}，发
 *         {@code {op:"hello", userid, avatar, role:"student", auth: userAuth, lessonid, presentation, ver:1}}
 *         建立实时会话。</li>
 * </ol>
 *
 * <p>与 {@code /api/v3/lesson/checkin} 的差异：checkin 是<b>签到动作</b>，开动态二维码的课堂会
 * 被 50070 拒绝；而 lesson_info_v2 是课堂页自己拉的<b>进班接口</b>，在读学生无需动态码。
 * 若 lesson_info_v2 仍返回 50070，说明该课堂只能本人扫码，本客户端如实回报、不做绕过。
 *
 * <p>凭据（userAuth）只留在后端内存，绝不回传前端、不写日志。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class LessonSessionClient {
    private static final Logger log = LoggerFactory.getLogger(LessonSessionClient.class);

    /** hello 后等待服务端首个响应的秒数；超时但连接未断仍视为会话已建立。 */
    static final int HELLO_ACK_TIMEOUT_SECONDS = 6;

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36";

    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String wsUrl;

    public LessonSessionClient(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        AppProperties.YktGateway cfg = properties.yktGateway();
        this.baseUrl = cfg.baseUrl() != null && !cfg.baseUrl().isBlank()
                ? cfg.baseUrl() : "https://changjiang.yuketang.cn";
        this.wsUrl = cfg.wsUrl() != null && !cfg.wsUrl().isBlank()
                ? cfg.wsUrl() : "wss://changjiang.yuketang.cn/wsapp/";
    }

    /** 免扫码进班结果（不含任何凭据）。 */
    public record EnterResult(
            String lessonId,
            String courseName,
            boolean entered,             // userAuthReady && socketEstablished
            boolean userAuthReady,        // lesson_info_v2 是否换出课堂凭据 userAuth
            boolean socketEstablished,    // WSS hello 会话是否建立
            String serverOp,              // 服务端首个响应的 op（无即时响应则为空）
            int code,                     // lesson_info_v2 业务码
            String msg,
            String note
    ) {}

    /**
     * 免扫码进班主流程。
     *
     * @param ctx    登录态（含 X-UID / csrftoken / university-id / uv-id / classroom-id）
     * @param lesson 进行中课堂（含 classroomId / universityId 兜底）
     */
    public EnterResult enter(SessionContext ctx, YktGateway.CourseInfo lesson) {
        String lessonId = lesson.lessonId();

        // ① 拉 lesson_info_v2 换课堂凭据 userAuth（身份头缺任一字段即 500）
        JsonNode data;
        int code;
        String msg;
        try {
            String uni = !ctx.universityId().isBlank() ? ctx.universityId() : lesson.universityId();
            String uv = !ctx.uvId().isBlank() ? ctx.uvId() : lesson.universityId();
            String cls = !ctx.classroomId().isBlank() ? ctx.classroomId() : lesson.classroomId();
            String cookieHeader = ctx.cookieHeader();
            if (!cookieHeader.toLowerCase().contains("university_id") && !uni.isBlank()) {
                cookieHeader += "; university_id=" + uni;
            }
            if (!cookieHeader.toLowerCase().contains("uv_id") && !uv.isBlank()) {
                cookieHeader += "; uv_id=" + uv;
            }
            String url = baseUrl + "/v/lesson/lesson_info_v2?lesson_id="
                    + URLEncoder.encode(lessonId, StandardCharsets.UTF_8) + "&source=5";
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Cookie", cookieHeader)
                    .header("xtbz", "ykt")
                    .header("X-Client", "h5");
            if (!ctx.userId().isBlank()) rb.header("X-UID", ctx.userId());
            if (!ctx.csrftoken().isBlank()) rb.header("X-CSRFToken", ctx.csrftoken());
            if (!uni.isBlank()) rb.header("university-id", uni);
            if (!uv.isBlank()) rb.header("uv-id", uv);
            if (!cls.isBlank()) rb.header("classroom-id", cls);

            HttpResponse<String> resp = http.send(rb.GET().build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            data = root.path("data");
            code = root.path("code").isNumber() ? root.path("code").asInt() : -1;
            msg = root.path("msg").asText("");
            log.info("免扫码进班 lesson_info_v2 lessonId={} http={} code={} 有userAuth={}",
                    lessonId, resp.statusCode(), code, !data.path("userAuth").asText("").isBlank());
        } catch (Exception e) {
            log.warn("免扫码进班 lesson_info_v2 异常 lessonId={}: {}", lessonId, e.getMessage());
            return new EnterResult(lessonId, lesson.courseName(), false, false, false, "",
                    -1, "", "进班接口请求失败：" + e.getMessage());
        }

        String userAuth = data.path("userAuth").asText("");
        if (userAuth.isBlank()) {
            String note = code == 50070
                    ? "该课堂已开启动态二维码签到，只能本人扫码进班"
                    : code == 50000
                        ? "登录态已失效，请重新登录"
                        : "未换出课堂凭据：" + msg;
            return new EnterResult(lessonId, lesson.courseName(), false, false, false,
                    "", code, msg, note);
        }

        // ② WSS hello 建立实时会话
        String userid = !ctx.userId().isBlank() ? ctx.userId() : data.path("userID").asText("");
        String avatar = data.path("avatar").asText("");
        String presentation = data.path("activePresentationID").asText("");
        SocketOutcome socket = establishSession(userid, avatar, userAuth, lessonId, presentation);

        boolean entered = socket.established();
        String note = socket.established()
                ? (socket.serverOp().isBlank()
                    ? "已凭登录态换取课堂凭据并建立 WebSocket 会话（服务端暂无即时推送）"
                    : "已凭登录态换取课堂凭据并建立 WebSocket 会话（首帧 op=" + socket.serverOp() + "）")
                : "已换出课堂凭据，但 WebSocket 会话建立失败：" + socket.note();
        return new EnterResult(lessonId, lesson.courseName(), entered, true,
                socket.established(), socket.serverOp(), code, msg, note);
    }

    private record SocketOutcome(boolean established, String serverOp, String note) {}

    /** 连 wsapp、发 hello、等待服务端首个响应。 */
    private SocketOutcome establishSession(String userid, String avatar, String userAuth,
                                           String lessonId, String presentation) {
        HelloFlow flow = new HelloFlow();
        try {
            WebSocket ws = http.newWebSocketBuilder()
                    .header("Origin", baseUrl)
                    .header("User-Agent", UA)
                    .buildAsync(URI.create(wsUrl), listener(flow))
                    .get(20, TimeUnit.SECONDS);
            flow.ws = ws;
            String hello = helloPayload(mapper, userid, avatar, userAuth, lessonId, presentation, 1);
            ws.sendText(hello, true).get(5, TimeUnit.SECONDS);

            String serverOp = "";
            try {
                JsonNode first = flow.firstMessage.get(HELLO_ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                serverOp = first.path("op").asText("");
            } catch (java.util.concurrent.TimeoutException e) {
                // 未收到即时响应，但连接保持 —— 会话已建立（服务端可能只在有事件时才推送）
            }
            // 会话已建立：断开探测连接（真实长连接由各端按需自持）
            closeQuietly(ws);
            return new SocketOutcome(true, serverOp, "");
        } catch (Exception e) {
            closeQuietly(flow.ws);
            return new SocketOutcome(false, "", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /** 包可见：hello 负载，供单测验证字段。 */
    static String helloPayload(ObjectMapper mapper, String userid, String avatar, String auth,
                               String lessonid, String presentation, int ver) {
        ObjectNode node = mapper.createObjectNode();
        node.put("op", "hello");
        node.put("userid", userid == null ? "" : userid);
        node.put("avatar", avatar == null ? "" : avatar);
        node.put("role", "student");
        node.put("auth", auth == null ? "" : auth);
        node.put("lessonid", lessonid == null ? "" : lessonid);
        node.put("presentation", presentation == null ? "" : presentation);
        node.put("ver", ver);
        return node.toString();
    }

    private WebSocket.Listener listener(HelloFlow flow) {
        return new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket ws) {
                flow.ws = ws;
                ws.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                flow.buffer.append(data);
                if (last) {
                    String message = flow.buffer.toString();
                    flow.buffer.setLength(0);
                    if (!flow.firstMessage.isDone()) {
                        try {
                            flow.firstMessage.complete(mapper.readTree(message));
                        } catch (Exception e) {
                            flow.firstMessage.completeExceptionally(e);
                        }
                    }
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                if (!flow.firstMessage.isDone()) {
                    flow.firstMessage.completeExceptionally(
                            new IllegalStateException("WebSocket 关闭 code=" + statusCode + " reason=" + reason));
                }
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                if (!flow.firstMessage.isDone()) {
                    flow.firstMessage.completeExceptionally(error);
                }
            }
        };
    }

    private static void closeQuietly(WebSocket ws) {
        if (ws != null) {
            try { ws.abort(); } catch (Exception ignored) {}
        }
    }

    /** 包可见，供单测。 */
    static final class HelloFlow {
        final CompletableFuture<JsonNode> firstMessage = new CompletableFuture<>();
        final StringBuilder buffer = new StringBuilder();
        volatile WebSocket ws;
    }
}
