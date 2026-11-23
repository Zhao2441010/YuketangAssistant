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

    /** 雨课堂返回的授权凭据。 */
    public record AuthCredential(String userId, String sessionKey) {}

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
