package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.SubmitResult;
import com.jiclub.gongxiaohu.service.InClassQuizService;
import com.jiclub.gongxiaohu.service.SessionResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 随堂超时补答：老师推送的随堂题，倒计时结束但老师未收题时仍可提交。 */
@RestController
@RequestMapping("/api/v1/inclass")
public class InClassQuizController {
    private final InClassQuizService service;
    private final SessionResolver sessionResolver;

    public InClassQuizController(InClassQuizService service, SessionResolver sessionResolver) {
        this.service = service;
        this.sessionResolver = sessionResolver;
    }

    /** 拉取某节课的随堂题（含未发布/已超时题）。 */
    @GetMapping("/quizzes")
    public ApiResponse<List<Quiz>> quizzes(
            @RequestParam(required = false) String lessonId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String presentationId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        String sessionKey = sessionResolver.resolve(token, legacyKey);
        String resolvedLesson = lessonId;
        if ((resolvedLesson == null || resolvedLesson.isBlank()) && courseId != null) {
            resolvedLesson = service.resolveLessonId(sessionKey, courseId);
        }
        return ApiResponse.ok(service.listQuizzes(sessionKey,
                resolvedLesson == null ? "" : resolvedLesson,
                presentationId == null ? "" : presentationId));
    }

    /** 提交 / 超时补交随堂答案（重复提交幂等覆盖）。 */
    @PostMapping("/answers")
    public ApiResponse<SubmitResult> submit(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        String sessionKey = sessionResolver.resolve(token, legacyKey);
        String lessonId = String.valueOf(body.getOrDefault("lessonId", ""));
        if (lessonId.isBlank() || "null".equals(lessonId)) {
            String courseId = String.valueOf(body.getOrDefault("courseId", ""));
            lessonId = service.resolveLessonId(sessionKey, courseId);
        }
        String problemId = String.valueOf(body.getOrDefault("problemId", ""));
        int problemType = body.get("problemType") instanceof Number n
                ? n.intValue() : Integer.parseInt(String.valueOf(body.getOrDefault("problemType", "1")));
        String answer = String.valueOf(body.getOrDefault("answer", ""));
        return ApiResponse.ok(service.submit(sessionKey, lessonId, problemId, problemType, answer));
    }
}
