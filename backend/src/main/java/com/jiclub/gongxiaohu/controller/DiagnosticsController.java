package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.gateway.LessonDiagnostic;
import com.jiclub.gongxiaohu.service.AuthService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证诊断接口：把 §6.7 待补抓包所需的证据结构化返回，无需用户手工抓包。
 *
 * <p>所有内容均已脱敏（无 Cookie / JWT 明文），且仅存后端内存。
 * 需携带有效 X-Session-Token，避免诊断信息对外公开。
 */
@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticsController {
    private final LessonDiagnostic diagnostic;
    private final AuthService authService;

    public DiagnosticsController(LessonDiagnostic diagnostic, AuthService authService) {
        this.diagnostic = diagnostic;
        this.authService = authService;
    }

    /** 查询最近 50 次 checkin 的诊断记录 + §6.7 验证清单状态。 */
    @GetMapping
    public ApiResponse<LessonDiagnostic.Report> report(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        requireLogin(token);
        return ApiResponse.ok(diagnostic.report());
    }

    /** 清空诊断记录（开始新一轮验证前调用）。 */
    @DeleteMapping
    public ApiResponse<Void> clear(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        requireLogin(token);
        diagnostic.clear();
        return ApiResponse.ok();
    }

    private void requireLogin(String token) {
        // sessionInfo 返回 valid=false 表示未登录；不泄露任何会话细节
        if (token == null || token.isBlank() || !authService.sessionInfo(token).valid()) {
            throw new com.jiclub.gongxiaohu.common.BizException(401, "请先登录后再查看诊断信息");
        }
    }
}
