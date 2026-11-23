package com.jiclub.gongxiaohu.service;

import org.springframework.stereotype.Component;

/**
 * 从请求头解析雨课堂 session_key：
 * 优先用登录后签发的 X-Session-Token（推荐，明文不落客户端），
 * 兼容直接传 X-Session-Key（旧演示方式 / 抓包调试）。
 */
@Component
public class SessionResolver {
    private final AuthService authService;

    public SessionResolver(AuthService authService) {
        this.authService = authService;
    }

    public String resolve(String token, String legacySessionKey) {
        if (token != null && !token.isBlank()) {
            return authService.resolveSessionKey(token);
        }
        if (legacySessionKey != null && !legacySessionKey.isBlank()) {
            return legacySessionKey;
        }
        throw new com.jiclub.gongxiaohu.common.BizException(401,
                "缺少登录凭据，请扫码登录后在请求头携带 X-Session-Token");
    }
}
