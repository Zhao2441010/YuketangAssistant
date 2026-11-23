package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.gateway.YktPreviewApi;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.service.SessionResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 提前获取题目（博客第 5 节）：在老师发题前预览课件里已下发的题目。 */
@RestController
@RequestMapping("/api/v1/preview")
public class PreviewController {
    private final YktGateway gateway;
    private final SessionResolver sessionResolver;
    private final ObjectProvider<YktPreviewApi> previewApi; // real 模式才有此 bean

    public PreviewController(YktGateway gateway, SessionResolver sessionResolver,
                             ObjectProvider<YktPreviewApi> previewApi) {
        this.gateway = gateway;
        this.sessionResolver = sessionResolver;
        this.previewApi = previewApi;
    }

    /**
     * 提前拉取某门正在上课课程的题目。
     *
     * @param courseId 课程 ID（课表返回的 courseId / classroomId 均可）
     */
    @GetMapping("/quizzes")
    public ApiResponse<List<Quiz>> quizzes(
            @RequestParam String courseId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        String sessionKey = sessionResolver.resolve(token, legacyKey);
        YktPreviewApi api = previewApi.getIfAvailable();
        if (api == null) {
            // 演示模式：走网关已有的题目列表
            return ApiResponse.ok(gateway.fetchQuizzes(sessionKey, courseId));
        }
        String lessonId = gateway.findLiveLesson(sessionKey, courseId)
                .map(YktGateway.CourseInfo::lessonId)
                .orElse("");
        if (lessonId.isBlank()) {
            return ApiResponse.fail(409, "该课程当前没有进行中的课堂，无法提前获取题目");
        }
        return ApiResponse.ok(api.fetchPreviewQuizzes(sessionKey, lessonId, courseId));
    }
}
