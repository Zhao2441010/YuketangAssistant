package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.gateway.YktContentApi;
import com.jiclub.gongxiaohu.model.ContentModels.Activity;
import com.jiclub.gongxiaohu.model.ContentModels.Lesson;
import com.jiclub.gongxiaohu.model.ContentModels.Presentation;
import com.jiclub.gongxiaohu.model.ContentModels.Slide;
import com.jiclub.gongxiaohu.service.SessionResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 课程中心：课程活动/课时列表、课件 PPT 页与教师题目。 */
@RestController
@RequestMapping("/api/v1/content")
public class ContentController {
    private final ObjectProvider<YktContentApi> contentApi; // real 模式才有
    private final SessionResolver sessionResolver;

    public ContentController(ObjectProvider<YktContentApi> contentApi, SessionResolver sessionResolver) {
        this.contentApi = contentApi;
        this.sessionResolver = sessionResolver;
    }

    @GetMapping("/activities")
    public ApiResponse<List<Activity>> activities(
            @RequestParam String courseId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        String sessionKey = sessionResolver.resolve(token, legacyKey);
        YktContentApi api = contentApi.getIfAvailable();
        if (api == null) return ApiResponse.ok(mockActivities(courseId));
        return ApiResponse.ok(api.fetchActivities(sessionKey, courseId));
    }

    @GetMapping("/lesson")
    public ApiResponse<Lesson> lesson(
            @RequestParam String lessonId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        String sessionKey = sessionResolver.resolve(token, legacyKey);
        YktContentApi api = contentApi.getIfAvailable();
        if (api == null) return ApiResponse.ok(mockLesson(lessonId));
        return ApiResponse.ok(api.fetchLesson(sessionKey, lessonId));
    }

    @GetMapping("/slides")
    public ApiResponse<List<Presentation>> slides(
            @RequestParam String lessonId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        String sessionKey = sessionResolver.resolve(token, legacyKey);
        YktContentApi api = contentApi.getIfAvailable();
        if (api == null) return ApiResponse.ok(mockPresentations(lessonId));
        return ApiResponse.ok(api.fetchPresentations(sessionKey, lessonId));
    }

    // ============ mock 演示数据 ============
    private static List<Activity> mockActivities(String courseId) {
        long now = System.currentTimeMillis();
        return List.of(
                new Activity("lesson-1", "cw-1", "第 1 讲 · 计算机网络体系结构", 14, "课堂", now - 86400000, true, true),
                new Activity("lesson-2", "cw-2", "第 2 讲 · 路由与交换", 14, "课堂", now - 3600000, false, false),
                new Activity("lesson-3", "cw-3", "随堂测验（第三章）", 16, "考试", now, false, false)
        );
    }

    private static Lesson mockLesson(String lessonId) {
        return new Lesson(lessonId, "第 2 讲 · 路由与交换", "计算机网络A",
                "计算机网络A", "王建勇", List.of("pres-1"));
    }

    private static List<Presentation> mockPresentations(String lessonId) {
        String prob = "{\"problemId\":39145902,\"problemType\":1,\"body\":\"以下哪个是动态路由协议？\","
                + "\"options\":[{\"key\":\"A\",\"value\":\"RIP\"},{\"key\":\"B\",\"value\":\"静态路由\"}]}";
        List<Slide> slides = List.of(
                new Slide("s1", 1, "", null),
                new Slide("s2", 2, "", null),
                new Slide("s3", 3, "", prob)
        );
        return List.of(new Presentation("pres-1", "课件 1", "", slides.size(), slides));
    }
}
