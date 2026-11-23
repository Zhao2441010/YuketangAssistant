package com.jiclub.gongxiaohu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 应用配置，绑定 application.yml 中 gongxiaohu.* 前缀。
 */
@ConfigurationProperties(prefix = "gongxiaohu")
public record AppProperties(
        String mode,
        JoinSignature joinSignature,
        YktGateway yktGateway,
        PasswordLogin passwordLogin,
        String uploadDir
) {

    /** 账号密码登录配置：RSA 公钥与滑块验证码 appid 均来自官方前端 login.js。 */
    public record PasswordLogin(
            String rsaPublicKey,
            String captchaAppId,
            String endpoint
    ) {
        public String endpointOrDefault() {
            return endpoint == null || endpoint.isBlank()
                    ? "/pc/login/verify_pwd_login/" : endpoint;
        }
        public String captchaAppIdOrDefault() {
            return captchaAppId == null || captchaAppId.isBlank() ? "2091064951" : captchaAppId;
        }
    }

    public record JoinSignature(String secret, long ttlSeconds) {
        public Duration ttl() {
            return Duration.ofSeconds(ttlSeconds);
        }
    }

    /** 真实网关配置；仅 mode=real 时生效。 */
    public record YktGateway(
            String region,
            String baseUrl,
            String wsUrl,
            int connectTimeoutMs,
            int readTimeoutMs,
            Map<String, String> endpoints
    ) {
        public String endpoint(String key) {
            String value = endpoints == null ? null : endpoints.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "缺少 ykt-gateway.endpoints." + key
                                + " 配置，请根据实际抓包结果补充真实接口路径");
            }
            return value;
        }
    }

    public boolean isMockMode() {
        return !"real".equalsIgnoreCase(mode);
    }
}
