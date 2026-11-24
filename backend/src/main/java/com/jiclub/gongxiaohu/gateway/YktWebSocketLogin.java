package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.AuthModels.AuthCredential;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 真实扫码登录（实测可用协议，2026-11-23 验证）。
 *
 * <p>与博客中的 HTTP 长轮询不同：实测 changjiang.ykt.io/custom/auth/v1/auth/status 返回 404，
 * 而标准区域的网页端登录走 WebSocket：
 * <ol>
 *     <li>连接 wss://www.yuketang.cn/wsapp/</li>
 *     <li>发送 {"op":"requestlogin","role":"web","version":1.4,"type":"qrcode","from":"web"}</li>
 *     <li>收到 {"op":"requestlogin","loginid":...,"ticket":"https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=...","expire_seconds":60}</li>
 *     <li>下载 ticket 图片展示二维码；用户微信扫码并确认</li>
 *     <li>同一 WebSocket 收到 {"op":"loginsuccess","UserID":...,"Auth":...,"subscribe_status":true}</li>
 *     <li>GET /v/course_meta/user_info 取 csrftoken，POST /pc/web_login 用 UserID+Auth 换 sessionid</li>
 * </ol>
 *
 * <p>说明：sessionid 即后续业务接口的凭据；四个区域（standard/hetang/changjiang/huanghe）
 * 会话互不通用，当前实现 standard，其余区域可通过配置 ws-url / base-url 切换。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktWebSocketLogin {
    private static final Logger log = LoggerFactory.getLogger(YktWebSocketLogin.class);
    /** 单次长轮询校验时长：45 秒（< 官方二维码有效期 60 秒，留 15 秒缓冲）。 */
    static final int POLL_TIMEOUT_SECONDS = 45;

    private static final String REQUEST_LOGIN_JSON =
            "{\"op\":\"requestlogin\",\"role\":\"web\",\"version\":1.4,\"type\":\"qrcode\",\"from\":\"web\"}";
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String wsUrl;
    private final String baseUrl;
    private final Map<String, LoginFlow> flows = new ConcurrentHashMap<>();

    public YktWebSocketLogin(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        AppProperties.YktGateway cfg = properties.yktGateway();
        this.wsUrl = cfg.wsUrl() != null && !cfg.wsUrl().isBlank()
                ? cfg.wsUrl() : "wss://www.yuketang.cn/wsapp/";
        this.baseUrl = cfg.baseUrl() != null && !cfg.baseUrl().isBlank()
                ? cfg.baseUrl() : "https://www.yuketang.cn";
        // 不设 connectTimeout：WebSocket 握在部分网络下较慢，握手超时单独在 buildAsync 处控制
        this.http = HttpClient.newHttpClient();
    }

    /** 建立 WebSocket、请求二维码、下载票图并转为 data URL。 */
    public LoginQr createLoginQr() {
        LoginFlow flow = new LoginFlow();
        try {
            WebSocket ws = http.newWebSocketBuilder()
                    .header("Origin", baseUrl)
                    .header("User-Agent", UA)
                    .buildAsync(URI.create(wsUrl), listener(flow))
                    .get(20, TimeUnit.SECONDS);
            flow.ws = ws;
            // 官方服务偶发"需客户端重发 requestlogin 才响应"（博客亦有提及），最多重试 3 次
            QrInfo info = null;
            for (int attempt = 1; attempt <= 3 && info == null; attempt++) {
                ws.sendText(REQUEST_LOGIN_JSON, true).get(5, TimeUnit.SECONDS);
                try {
                    info = flow.qrFuture.get(5, TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.debug("第 {} 次 requestlogin 未响应，重试", attempt);
                }
            }
            if (info == null) {
                throw new IllegalStateException("官方 WebSocket 未返回二维码 ticket（已重试 3 次）");
            }
            byte[] image = http.send(
                            HttpRequest.newBuilder(URI.create(info.ticketUrl()))
                                    .header("User-Agent", UA).GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray())
                    .body();
            if (image.length == 0) {
                throw new IllegalStateException("二维码图片下载为空");
            }
            String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image);
            flow.expiresAt = Instant.now().plusSeconds(info.expireSeconds()).toEpochMilli();
            flow.loginId = info.loginId();
            flows.put(info.loginId(), flow);
            log.info("真实二维码已生成 loginid={} expire={}s imgBytes={}",
                    info.loginId(), info.expireSeconds(), image.length);
            return new LoginQr(
                    info.loginId(),
                    dataUrl,
                    info.ticketUrl(),
                    Instant.now().plusSeconds(info.expireSeconds()).toEpochMilli(),
                    POLL_TIMEOUT_SECONDS,
                    false
            );
        } catch (Exception e) {
            closeQuietly(flow.ws);
            throw new IllegalStateException("真实二维码获取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 等待扫码确认（最多 45 秒），拿到 UserID/Auth 后换取 sessionid。
     *
     * @param loginId 创建二维码时返回的 loginid（不是图片 URL）
     */
    public Optional<AuthCredential> pollAuthStatus(String loginId) {
        LoginFlow flow = flows.get(loginId);
        if (flow == null) {
            return Optional.empty();
        }
        if (flow.expiresAt > 0 && System.currentTimeMillis() > flow.expiresAt) {
            log.debug("二维码已过期 loginid={}", loginId);
            cleanup(loginId, flow);
            return Optional.empty();
        }
        try {
            // 二维码有效期 60 秒，单次长轮询 45 秒；超时不销毁连接，供前端续轮询
            JsonNode info = flow.loginFuture.get(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String userId = text(info, "UserID", "user_id");
            String auth = text(info, "Auth", "auth");
            if (auth.isBlank()) {
                return Optional.empty();
            }
            com.jiclub.gongxiaohu.model.AuthModels.SessionContext ctx =
                    exchangeSessionId(userId, auth, info.get("UserID"));
            log.info("真实扫码登录成功 userId={} 有csrftoken={}",
                    mask(userId), !ctx.csrftoken().isBlank());
            cleanup(loginId, flow); // 成功后释放连接
            return Optional.of(new AuthCredential(userId, ctx.sessionId(), ctx));
        } catch (java.util.concurrent.TimeoutException e) {
            // 关键：超时只返回空，保留 flow 与 WebSocket，前端可在二维码有效期内续轮询
            log.debug("真实登录等待扫码超时（可续轮询） loginid={}", loginId);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanup(loginId, flow);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("真实登录失败 loginid={}: {}", loginId, e.getMessage());
            cleanup(loginId, flow);
            return Optional.empty();
        }
    }

    private void cleanup(String loginId, LoginFlow flow) {
        flows.remove(loginId);
        closeQuietly(flow.ws);
    }

    /**
     * 用 UserID + Auth 调 /pc/web_login 换 sessionid，并带出完整 L1 登录态。
     *
     * <p>§6.2 ①：checkin 需要 csrftoken / X-UID / university-id / uv-id，
     * 过去只返回 sessionid 导致课堂凭证换取必 401。这里从 user_info 的 Cookie
     * 与响应体宽松解析这些字段，存进 SessionContext 一路带到 AuthService。
     */
    private com.jiclub.gongxiaohu.model.AuthModels.SessionContext exchangeSessionId(
            String userId, String auth, JsonNode rawUserId) throws Exception {
        HttpResponse<String> pre = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/v/course_meta/user_info"))
                        .header("User-Agent", UA).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String csrf = cookie(pre, "csrftoken");
        String uni = "", uv = "";
        try {
            JsonNode info = mapper.readTree(pre.body());
            JsonNode d = info.path("data");
            if (d.isMissingNode()) d = info;
            uni = firstText(d, "university_id", "universityId", "school_id");
            uv = firstText(d, "uv_id", "uvId");
        } catch (Exception ignored) {
            // user_info 结构因区域而异，解析不到就留空，可由 /auth/import-cookie 补齐
        }

        ObjectNode body = mapper.createObjectNode();
        if (rawUserId != null && rawUserId.isNumber()) {
            body.put("UserID", rawUserId.asLong());
        } else {
            body.put("UserID", userId);
        }
        body.put("Auth", auth);

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(baseUrl + "/pc/web_login"))
                .header("User-Agent", UA)
                .header("Content-Type", "application/json")
                .header("Referer", baseUrl + "/web")
                .header("Accept", "application/json, text/plain, */*")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (!csrf.isBlank()) {
            req.header("Cookie", "csrftoken=" + csrf).header("X-CSRFToken", csrf);
        }
        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        String sessionId = cookie(resp, "sessionid");
        if (sessionId.isBlank()) {
            log.warn("web_login 未返回 sessionid，响应片段={}",
                    resp.body() == null ? "" : resp.body().substring(0, Math.min(200, resp.body().length())));
            sessionId = "sk-" + userId;
        }
        // 保留同一会话的 Cookie 串供 checkin 使用（csrftoken 必须与 sessionid 同源）
        String rawCookie = "csrftoken=" + csrf + "; sessionid=" + sessionId;
        if (!uni.isBlank()) rawCookie += "; university_id=" + uni;
        if (!uv.isBlank()) rawCookie += "; uv_id=" + uv;
        return new com.jiclub.gongxiaohu.model.AuthModels.SessionContext(
                sessionId, csrf, userId, uni, uv, "", rawCookie);
    }

    private static String firstText(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return "";
    }

    private WebSocket.Listener listener(LoginFlow flow) {
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
                    handleMessage(message, flow);
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                log.debug("WebSocket 关闭 code={} reason={}", statusCode, reason);
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                flow.qrFuture.completeExceptionally(error);
                flow.loginFuture.completeExceptionally(error);
            }
        };
    }

    private void handleMessage(String message, LoginFlow flow) {
        try {
            JsonNode node = mapper.readTree(message);
            String op = node.path("op").asText("");
            if ("requestlogin".equals(op) && node.has("ticket")) {
                flow.qrFuture.complete(new QrInfo(
                        node.path("ticket").asText(),
                        node.path("loginid").asText(),
                        node.path("expire_seconds").asInt(60),
                        node.path("qrcode").asText("")
                ));
            } else if ("loginsuccess".equals(op) || node.has("subscribe_status")) {
                // 关键修复：只有真正带 Auth 的消息才算登录成功。
                // subscribe_status 是订阅状态通知，可能先于 loginsuccess 到达且不含 Auth；
                // 若它也 complete loginFuture，poll 会拿到空 Auth 并返回空，
                // 而 future 已无法再次 complete，真正的 loginsuccess 被丢弃 → 永远登不上。
                String auth = text(node, "Auth", "auth");
                if (!auth.isBlank()) {
                    log.info("收到登录成功凭据 loginid={} op={}", flow.loginId, op);
                    flow.loginFuture.complete(node);
                } else {
                    log.debug("忽略不含 Auth 的状态通知 op={} keys={}", op, node.fieldNames());
                }
            }
        } catch (Exception e) {
            log.debug("忽略无法解析的 WebSocket 消息: {}", message.substring(0, Math.min(80, message.length())));
        }
    }

    private static String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return "";
    }

    private static String cookie(HttpResponse<?> response, String name) {
        for (String raw : response.headers().allValues("Set-Cookie")) {
            for (String part : raw.split(";")) {
                String kv = part.trim();
                int idx = kv.indexOf('=');
                if (idx > 0 && kv.substring(0, idx).trim().equalsIgnoreCase(name)) {
                    return kv.substring(idx + 1).trim();
                }
            }
        }
        return "";
    }

    private static void closeQuietly(WebSocket ws) {
        if (ws != null) {
            try { ws.abort(); } catch (Exception ignored) {}
        }
    }

    private static String mask(String value) {
        return value == null || value.length() < 4 ? "****"
                : value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private static final class LoginFlow {
        final CompletableFuture<QrInfo> qrFuture = new CompletableFuture<>();
        final CompletableFuture<JsonNode> loginFuture = new CompletableFuture<>();
        final StringBuilder buffer = new StringBuilder();
        volatile WebSocket ws;
        volatile long expiresAt;
        volatile String loginId;
    }

    private record QrInfo(String ticketUrl, String loginId, int expireSeconds, String qrcode) {}
}
