package com.jiclub.gongxiaohu.common;

/**
 * 单次 HTTP 请求的线程上下文（仅存活于一次请求内）。
 *
 * <p>只放"日志需要但不必层层透传"的非敏感信息：客户端 IP、请求方法/路径、UA。
 * <b>不放 token / Cookie / sessionid</b>，避免日志层误把凭证带出去。
 */
public final class RequestContext {

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    /** 一次请求的上下文快照。 */
    public record Ctx(String ip, String method, String uri, String userAgent) {
    }

    static void set(String ip, String method, String uri, String userAgent) {
        HOLDER.set(new Ctx(ip, method, uri, userAgent));
    }

    static void clear() {
        HOLDER.remove();
    }

    /** 客户端 IP，不在请求线程内调用时返回 {@code "-"}。 */
    public static String ip() {
        Ctx ctx = HOLDER.get();
        return ctx == null || ctx.ip() == null || ctx.ip().isBlank() ? "-" : ctx.ip();
    }

    /** 请求方法与路径，如 {@code POST /api/v1/auth/poll}。 */
    public static String request() {
        Ctx ctx = HOLDER.get();
        return ctx == null ? "-" : ctx.method() + " " + ctx.uri();
    }

    /** 精简 UA（只保留浏览器/客户端标识前 60 字符），便于区分 App / 小程序 / 浏览器。 */
    public static String client() {
        Ctx ctx = HOLDER.get();
        if (ctx == null || ctx.userAgent() == null || ctx.userAgent().isBlank()) return "-";
        String ua = ctx.userAgent().replace('\n', ' ').replace('\r', ' ').trim();
        String lower = ua.toLowerCase();
        if (lower.contains("okhttp")) return "AndroidApp(okhttp)";
        if (lower.contains("yuketangassistant")) return "NativeShell";
        if (lower.contains("micromessenger")) return "WeChat";
        if (lower.contains("miniprogram")) return "MiniProgram";
        return ua.length() > 60 ? ua.substring(0, 60) : ua;
    }
}
