package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.AuthModels.LoginResult;
import com.jiclub.gongxiaohu.model.AuthModels.SessionInfo;
import com.jiclub.gongxiaohu.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 扫码登录接口：用户端 → 后端 → 雨课堂端的入口。 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 1. 获取登录二维码（mock 模式返回本地生成的二维码 data URL）。 */
    @GetMapping("/qr")
    public ApiResponse<LoginQr> qr() {
        return ApiResponse.ok(authService.createLoginQr());
    }

    /** 2. 45 秒阻塞长轮询：手机确认登录的瞬间立即返回 token。 */
    @PostMapping("/poll")
    public ApiResponse<LoginResult> poll(@RequestBody Map<String, String> body) {
        String ticket = body.get("ticket");
        if (ticket == null || ticket.isBlank()) {
            return ApiResponse.fail(400, "缺少 ticket");
        }
        return ApiResponse.ok(authService.pollAndLogin(ticket));
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

    /** 5. 退出登录。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        authService.logout(token);
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

    /** 便捷接口：仅演示模式用，一步完成「建票据→模拟确认→登录」，方便自动化联调。 */
    @PostMapping("/mock-login")
    public ApiResponse<LoginResult> mockLogin() {
        LoginQr qr = authService.createLoginQr();
        authService.mockConfirm(qr.ticket());
        return ApiResponse.ok(authService.pollAndLogin(qr.ticket()));
    }
}
