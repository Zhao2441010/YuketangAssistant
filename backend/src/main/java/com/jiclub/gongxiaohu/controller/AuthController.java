package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.gateway.LessonCredentialClient;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.AuthModels.LoginResult;
import com.jiclub.gongxiaohu.model.AuthModels.SessionInfo;
import com.jiclub.gongxiaohu.service.AuthService;
import com.jiclub.gongxiaohu.service.InClassQuizService;
import com.jiclub.gongxiaohu.service.JoinLessonService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** 扫码登录接口：用户端 → 后端 → 雨课堂端的入口。 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JoinLessonService joinLessonService;
    private final InClassQuizService inClassQuizService;
    private final ObjectProvider<LessonCredentialClient> credentialClient;

    public AuthController(AuthService authService, JoinLessonService joinLessonService,
                          InClassQuizService inClassQuizService,
                          ObjectProvider<LessonCredentialClient> credentialClient) {
        this.authService = authService;
        this.joinLessonService = joinLessonService;
        this.inClassQuizService = inClassQuizService;
        this.credentialClient = credentialClient;
    }

    /** 1. 获取登录二维码（mock 模式返回本地生成的二维码 data URL）。 */
    @GetMapping("/qr")
    public ApiResponse<LoginQr> qr() {
        return ApiResponse.ok(authService.createLoginQr());
    }

    /**
     * 2. 轮询扫码结果。
     *
     * <p>body {ticket, wait}：wait 为单次最长等待秒数，缺省 45（兼容旧客户端长轮询）；
     * 前端传 0 走短轮询，1 秒内必定返回，避免移动端长空闲连接被掐断导致成功响应丢失。
     * 同一 ticket 重复轮询是幂等的——会拿回同一个 token。
     */
    @PostMapping("/poll")
    public ApiResponse<LoginResult> poll(@RequestBody Map<String, String> body) {
        String ticket = body.get("ticket");
        if (ticket == null || ticket.isBlank()) {
            return ApiResponse.fail(400, "缺少 ticket");
        }
        long wait = parseWaitSeconds(body.get("wait"));
        long started = System.currentTimeMillis();
        try {
            var result = authService.pollAndLogin(ticket, wait);
            log.info("扫码轮询成功 ticket={} userId={} mock={} wait={}s 耗时={}ms",
                    fingerprint(ticket), result.userId(), result.mock(), wait,
                    System.currentTimeMillis() - started);
            return ApiResponse.ok(result);
        } catch (com.jiclub.gongxiaohu.common.BizException e) {
            // 409 = 尚未确认/已超时，前端会续轮询；debug 级留痕，排查"扫码后停住"用
            log.debug("扫码轮询暂未拿到凭据 ticket={} code={} wait={}s 耗时={}ms msg={}",
                    fingerprint(ticket), e.getCode(), wait,
                    System.currentTimeMillis() - started, e.getMessage());
            throw e;
        }
    }

    /** wait 缺省 45 秒（旧客户端长轮询）；负数或非法值回落到默认，上限 45。 */
    private static long parseWaitSeconds(String raw) {
        if (raw == null || raw.isBlank()) return 45;
        try {
            return Math.max(0, Math.min(Long.parseLong(raw.trim()), 45));
        } catch (NumberFormatException e) {
            return 45;
        }
    }

    private static String fingerprint(String ticket) {
        if (ticket == null || ticket.isBlank()) return "-";
        return ticket.length() <= 8 ? ticket : ticket.substring(0, 8);
    }

    /** 3. 演示模式：模拟手机端扫码确认（真实模式不需要此接口）。 */
    @PostMapping("/mock-confirm")
    public ApiResponse<Boolean> mockConfirm(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.mockConfirm(body.get("ticket")));
    }

    /** 4. 查询当前会话是否有效（前端刷新页面时校验本地 token）。 */
    @GetMapping("/session")
    public ApiResponse<SessionInfo> session(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ApiResponse.ok(authService.sessionInfo(token));
    }

    /** 主动续期：延长本地会话有效期，返回新的过期时间。 */
    @PostMapping("/renew")
    public ApiResponse<SessionInfo> renew(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ApiResponse.ok(authService.renew(token));
    }

    /** 5. 退出登录：同时清除后端内存中的课堂凭证与自渲染会话（方案④安全边界）。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        // 先取学号用于审计，再清会话（清完就查不到了）
        String userId = authService.peekUserId(token);
        try {
            String sid = authService.resolveSessionKey(token);
            LessonCredentialClient client = credentialClient.getIfAvailable();
            if (client != null) client.evict(sid);
            joinLessonService.evict(sid);
            inClassQuizService.evict(sid);
        } catch (Exception ignored) {
            // token 已失效时仍需完成本地登出
        }
        authService.logout(token);
        com.jiclub.gongxiaohu.common.AuditLog.logout(userId);
        return ApiResponse.ok();
    }

    /** 账号密码登录：body {account,password,type,ticket,randstr}。 */
    @PostMapping("/password-login")
    public ApiResponse<LoginResult> passwordLogin(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.loginByPassword(
                body.get("account"), body.get("password"),
                body.get("type"), body.get("ticket"), body.get("randstr")));
    }

    /** 用已有真实 sessionid 导入会话，跳过扫码（真实模式专用）。 */
    @PostMapping("/import-session")
    public ApiResponse<LoginResult> importSession(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.importSession(body.get("sessionId"), body.get("userId")));
    }

    /**
     * §6.2 ①：导入整串 Cookie，补齐 L1 登录态（csrftoken / X-UID / university-id / uv-id）。
     * 课堂凭证换取需要这些字段，仅 sessionid 会 401。Cookie 只存后端内存，不回传前端。
     */
    @PostMapping("/import-cookie")
    public ApiResponse<LoginResult> importCookie(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.importCookie(body.get("cookie"), body.get("userId")));
    }

    /** 便捷接口：仅演示模式用，一步完成「建票据→模拟确认→登录」，方便自动化联调。 */
    @PostMapping("/mock-login")
    public ApiResponse<LoginResult> mockLogin() {
        LoginQr qr = authService.createLoginQr();
        authService.mockConfirm(qr.ticket());
        return ApiResponse.ok(authService.pollAndLogin(qr.ticket()));
    }
}
