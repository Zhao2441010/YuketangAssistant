package com.jiclub.gongxiaohu.model;

/**
 * 扫码登录相关模型（对应博客第 2 节：阻塞长轮询，当前单次校验 45 秒）。
 */
public final class AuthModels {
    private AuthModels() {}

    /** 登录二维码：ticket 用于长轮询查询授权状态。 */
    public record LoginQr(
            String ticket,
            String qrDataUrl,          // data:image/svg+xml;base64,... 或官方二维码地址
            String qrContent,          // 扫码内容（真实模式可能为官方链接）
            long expiresAt,
            int pollTimeoutSeconds,    // 长轮询最长挂起秒数（30）
            boolean mockMode           // 是否为演示模式（前端据此显示「模拟确认」按钮）
    ) {}

    /**
     * 雨课堂返回的授权凭据。context 为完整 L1 登录态（csrftoken 等），
     * 扫码登录时由 web_login 交换过程自然带出；Mock 模式退化为最小上下文。
     */
    public record AuthCredential(String userId, String sessionKey, SessionContext context) {
        public AuthCredential(String userId, String sessionKey) {
            this(userId, sessionKey, SessionContext.minimal(sessionKey));
        }
    }

    /**
     * L1 登录态上下文（§6.2 ①）：checkin 需要的不只是 sessionid。
     *
     * <p>官方 POST 请求要求 csrftoken / X-CSRFToken / X-UID / classroom-id /
     * university-id / uv-id 以及明文 sessionid 同名头。缺任一项即 401 / 50000。
     * 这些值来自扫码 web_login 或账号密码登录的 Set-Cookie 与 user_info 响应。
     * 允许为空时凭证客户端会自动从 cookie 串推导或跳过该头，便于逐步补全抓包。
     */
    public record SessionContext(
            String sessionId,       // 雨课堂 sessionid（核心）
            String csrftoken,       // CSRF token，与 Cookie 同名
            String userId,          // X-UID（user_id）
            String universityId,    // university-id
            String uvId,            // uv-id
            String classroomId,     // classroom-id（按课堂变化）
            String rawCookie        // 用户导入的整串 Cookie（可选，作为兜底）
    ) {
        /** 从 sessionid 构造最小上下文（兼容旧链路 / 抓包未补全时使用）。 */
        public static SessionContext minimal(String sessionId) {
            return new SessionContext(sessionId == null ? "" : sessionId,
                    "", "", "", "", "", "");
        }

        /** 解析整串 Cookie：csrftoken=..; sessionid=..; ... */
        public static SessionContext fromCookie(String cookie, String userId) {
            String sid = "", csrf = "", uni = "", uv = "";
            if (cookie != null && !cookie.isBlank()) {
                for (String part : cookie.split(";")) {
                    String[] kv = part.trim().split("=", 2);
                    if (kv.length != 2) continue;
                    switch (kv[0].trim().toLowerCase()) {
                        case "sessionid" -> sid = kv[1].trim();
                        case "csrftoken" -> csrf = kv[1].trim();
                        case "university_id", "university-id" -> uni = kv[1].trim();
                        case "uv_id", "uv-id" -> uv = kv[1].trim();
                        default -> { /* 忽略其余 cookie */ }
                    }
                }
            }
            return new SessionContext(sid, csrf, userId == null ? "" : userId,
                    uni, uv, "", cookie == null ? "" : cookie);
        }

        /** 组装官方请求 Cookie 头（优先用原始串，否则用解析出的字段）。 */
        public String cookieHeader() {
            if (rawCookie != null && !rawCookie.isBlank()) return rawCookie;
            StringBuilder sb = new StringBuilder();
            if (!csrftoken.isBlank()) sb.append("csrftoken=").append(csrftoken).append("; ");
            sb.append("sessionid=").append(sessionId);
            if (!universityId.isBlank()) sb.append("; university_id=").append(universityId);
            if (!uvId.isBlank()) sb.append("; uv_id=").append(uvId);
            return sb.toString();
        }
    }

    /** 登录结果：token 供业务接口使用，sessionKey 交客户端本地加密保存。 */
    public record LoginResult(
            String token,
            String userId,
            String sessionKey,
            long expiresAt,
            boolean mock
    ) {}

    /** 会话信息查询。remainingSeconds 供前端判断是否需要主动续期。 */
    public record SessionInfo(
            String token, String userId, long expiresAt, boolean valid,
            long remainingSeconds, boolean renewable
    ) {
        public SessionInfo(String token, String userId, long expiresAt, boolean valid) {
            this(token, userId, expiresAt, valid,
                    Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000), false);
        }
    }
}
