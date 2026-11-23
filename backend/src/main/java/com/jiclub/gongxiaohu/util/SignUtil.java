package com.jiclub.gongxiaohu.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 临时进班链接签名：对应博客第 1 节的 sign + ts 机制。
 *
 * <p>sign = HMAC-SHA256(secret, lessonId + "|" + ts)，
 * 服务端签发、服务端校验，链接仅在 ttl 秒内有效。
 */
public final class SignUtil {
    private SignUtil() {}

    public static String sign(String secret, String lessonId, long ts) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal((lessonId + "|" + ts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 不可用", e);
        }
    }

    /** 校验签名与时效；常量时间比较避免时序侧信道。 */
    public static boolean verify(String secret, String lessonId, long ts, String sign, long ttlSeconds) {
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > ttlSeconds) {
            return false;
        }
        String expected = sign(secret, lessonId, ts);
        return expected.length() == (sign == null ? -1 : sign.length())
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                        sign.getBytes(StandardCharsets.UTF_8));
    }
}
