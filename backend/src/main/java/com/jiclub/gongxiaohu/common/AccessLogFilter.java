package com.jiclub.gongxiaohu.common;

import com.jiclub.gongxiaohu.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问日志：每个 {@code /api/**} 请求一行，形如
 * <pre>
 * 2026-11-24 18:01:02.345 INFO  [tomcat-handler-3] ACCESS - ip=1.2.3.4 GET /api/v1/courses status=200 耗时=83ms user=2023****45
 * </pre>
 *
 * <p>用途：判断"服务到底有没有被访问 / 谁在用 / 哪个接口慢或报错"。
 * 学号在这里<b>脱敏</b>（要明文查审计日志 logs/audit.log）。
 * token 只用于反查学号，绝不写入日志。
 *
 * <p>用 {@link ObjectProvider} 延迟取 AuthService：filter 由容器极早创建，
 * 直接注入 Service 会触发过早初始化告警。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("ACCESS");

    /** 前端定时轮询会话的接口，成功时降为 debug，避免刷屏。 */
    private static final String QUIET_PATH = "/auth/session";

    private final ObjectProvider<AuthService> authService;

    public AccessLogFilter(ObjectProvider<AuthService> authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = clientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String token = request.getHeader("X-Session-Token");

        RequestContext.set(ip, method, uri, request.getHeader("User-Agent"));
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String user = mask(resolveUserId(token));
            boolean quiet = "GET".equalsIgnoreCase(method)
                    && uri.endsWith(QUIET_PATH) && status == 200;
            if (quiet) {
                log.debug("ip={} {} {} status={} 耗时={}ms user={}",
                        ip, method, uri, status, cost, user);
            } else {
                log.info("ip={} {} {} status={} 耗时={}ms user={}",
                        ip, method, uri, status, cost, user);
            }
            RequestContext.clear();
        }
    }

    private String resolveUserId(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return authService.getObject().peekUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    /** 取真实客户端 IP：优先反向代理头，其次 socket。 */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        String remote = request.getRemoteAddr();
        return remote == null ? "-" : remote;
    }

    private static String mask(String userId) {
        if (userId == null || userId.isBlank()) return "anonymous";
        if (userId.length() < 4) return "****";
        return userId.substring(0, 2) + "****" + userId.substring(userId.length() - 2);
    }
}
