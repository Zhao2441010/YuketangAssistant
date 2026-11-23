package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jiclub.gongxiaohu.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * 账号密码登录（真实接口，协议来自官方前端 login.js 抓包）。
 *
 * <p>POST {base}/pc/login/verify_pwd_login/，body：
 * <pre>
 * {"type":"PP"|"E","name":"手机号/邮箱","pwd":"RSA加密后的密码",
 *  "ticket":"滑块验证码ticket","randstr":"滑块验证码randstr"}
 * </pre>
 * <ul>
 *   <li>type：PP=手机号+密码，E=邮箱+密码，PC=手机号+验证码</li>
 *   <li>pwd：官方用内置 1024 位 RSA 公钥（PKCS1Padding）加密，Base64 输出</li>
 *   <li>ticket/randstr：腾讯防水墙滑块验证码结果（appid 2091064951），需用户手动完成</li>
 * </ul>
 * 成功后响应 Set-Cookie 含 sessionid（与扫码登录同一会话体系）。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktPasswordLoginApi {
    private static final Logger log = LoggerFactory.getLogger(YktPasswordLoginApi.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final AppProperties.PasswordLogin cfg;

    public YktPasswordLoginApi(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.yktGateway().baseUrl();
        this.cfg = properties.passwordLogin();
    }

    /** 返回结果：userId + sessionid。 */
    public record LoginOutcome(String userId, String sessionId, String message) {
        public boolean ok() { return sessionId != null && !sessionId.isBlank(); }
    }

    /**
     * @param account  手机号或邮箱
     * @param password 明文密码（本方法内加密）
     * @param type     PP=手机号，E=邮箱
     * @param ticket   滑块 ticket（用户完成滑块后获得）
     * @param randstr  滑块 randstr
     */
    public LoginOutcome login(String account, String password, String type,
                              String ticket, String randstr) {
        try {
            String encrypted = encryptPassword(password);
            ObjectNode body = mapper.createObjectNode();
            body.put("type", type == null || type.isBlank() ? "PP" : type);
            body.put("name", account);
            body.put("pwd", encrypted);
            body.put("ticket", ticket == null ? "" : ticket);
            body.put("randstr", randstr == null ? "" : randstr);

            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + cfg.endpointOrDefault()))
                    .header("User-Agent", UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/json")
                    .header("Origin", baseUrl)
                    .header("Referer", baseUrl + "/web")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            String sid = extractCookie(resp, "sessionid");
            JsonNode root = mapper.readTree(resp.body());
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                int code = root.path("status_code").asInt(0);
                String msg = root.path("msg").asText("登录失败");
                log.warn("账号密码登录失败 code={} msg={}", code, msg);
                return new LoginOutcome("", "", msg + "（code=" + code + "）");
            }
            if (sid.isBlank()) {
                // 有些版本 sessionid 在 body 里
                sid = text(root.path("data"), "sessionid", "session_id", "sessionKey");
            }
            String userId = text(root.path("data"), "user_id", "userId", "userid");
            if (userId.isBlank()) userId = text(root, "user_id", "userId");
            log.info("账号密码登录成功 userId={} hasSession={}", mask(userId), !sid.isBlank());
            return new LoginOutcome(userId, sid, "登录成功");
        } catch (Exception e) {
            log.warn("账号密码登录异常: {}", e.getMessage());
            return new LoginOutcome("", "", "登录异常：" + e.getMessage());
        }
    }

    /** 用官方 RSA 公钥加密密码（RSA/ECB/PKCS1Padding，Base64）。 */
    public String encryptPassword(String password) throws Exception {
        String key = cfg.rsaPublicKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("未配置 password-login.rsa-public-key");
        }
        byte[] der = Base64.getDecoder().decode(key);
        PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static String extractCookie(HttpResponse<?> resp, String name) {
        for (String raw : resp.headers().allValues("Set-Cookie")) {
            for (String part : raw.split(";")) {
                String kv = part.trim();
                int i = kv.indexOf('=');
                if (i > 0 && kv.substring(0, i).trim().equalsIgnoreCase(name)) {
                    return kv.substring(i + 1).trim();
                }
            }
        }
        return "";
    }

    private static String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return "";
    }

    private static String mask(String s) {
        return s == null || s.length() < 4 ? "****" : s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}
