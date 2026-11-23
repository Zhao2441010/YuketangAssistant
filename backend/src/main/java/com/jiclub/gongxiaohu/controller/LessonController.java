package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.JoinLessonRequest;
import com.jiclub.gongxiaohu.model.Models.JoinLessonResult;
import com.jiclub.gongxiaohu.service.JoinLessonService;
import com.jiclub.gongxiaohu.service.SessionResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 免扫码秒进班接口。 */
@RestController
@RequestMapping("/api/v1")
public class LessonController {
    private final JoinLessonService service;
    private final SessionResolver sessionResolver;

    public LessonController(JoinLessonService service, SessionResolver sessionResolver) {
        this.service = service;
        this.sessionResolver = sessionResolver;
    }

    /** 获取当前课表，liveCourses 即 status==1 的可秒进课堂。 */
    @GetMapping("/courses")
    public ApiResponse<CourseListResult> courses(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        return ApiResponse.ok(service.getCourseList(sessionResolver.resolve(token, legacyKey)));
    }

    /** 免扫码进班：签名链接由服务端签发。 */
    @PostMapping("/lessons/join")
    public ApiResponse<JoinLessonResult> join(
            @Valid @RequestBody JoinLessonRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        String sessionKey = token != null && !token.isBlank()
                ? sessionResolver.resolve(token, null) : request.sessionKey();
        return ApiResponse.ok(service.join(new JoinLessonRequest(request.courseId(), sessionKey)));
    }

    /** 校验临时进班签名链接。 */
    @GetMapping("/lessons/verify-sign")
    public ApiResponse<Boolean> verifySign(
            @RequestParam String lessonId,
            @RequestParam long ts,
            @RequestParam String sign) {
        return ApiResponse.ok(service.verifyJoinUrl(lessonId, ts, sign));
    }
}
