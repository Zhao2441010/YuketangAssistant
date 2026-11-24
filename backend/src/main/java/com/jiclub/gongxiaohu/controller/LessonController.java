package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.YktContentApi;
import com.jiclub.gongxiaohu.model.AuthModels.SessionContext;
import com.jiclub.gongxiaohu.model.ContentModels.Presentation;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.JoinLessonRequest;
import com.jiclub.gongxiaohu.model.Models.JoinLessonResult;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.service.AuthService;
import com.jiclub.gongxiaohu.service.InClassQuizService;
import com.jiclub.gongxiaohu.service.JoinLessonService;
import com.jiclub.gongxiaohu.service.SessionResolver;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 课堂进班与方案④自渲染接口。 */
@RestController
@RequestMapping("/api/v1")
public class LessonController {
    private static final Logger log = LoggerFactory.getLogger(LessonController.class);

    private final JoinLessonService service;
    private final SessionResolver sessionResolver;
    private final AuthService authService;
    private final InClassQuizService inClassQuizService;
    private final ObjectProvider<YktContentApi> contentApi;
    private final AppProperties properties;

    public LessonController(JoinLessonService service, SessionResolver sessionResolver,
                            AuthService authService, InClassQuizService inClassQuizService,
                            ObjectProvider<YktContentApi> contentApi, AppProperties properties) {
        this.service = service;
        this.sessionResolver = sessionResolver;
        this.authService = authService;
        this.inClassQuizService = inClassQuizService;
        this.contentApi = contentApi;
        this.properties = properties;
    }

    /** 获取当前课表。 */
    @GetMapping("/courses")
    public ApiResponse<CourseListResult> courses(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        return ApiResponse.ok(service.getCourseList(sessionResolver.resolve(token, legacyKey)));
    }

    /**
     * 免扫码进课堂会话（§7 新方案）：只做开课发现 + 返回官方在读学生固定入口，
     * <b>默认不调用 checkin</b>，因此不经过动态码校验。
     *
     * @param enhance 仅当需要在 App 内自绘课件/题目时传 true，才会按需尝试 checkin 换 pptJwt；
     *                失败不影响进课堂
     */
    @PostMapping("/lessons/join")
    public ApiResponse<JoinLessonResult> join(
            @Valid @RequestBody JoinLessonRequest request,
            @RequestParam(value = "enhance", defaultValue = "false") boolean enhance,
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        String sessionKey = token != null && !token.isBlank()
                ? sessionResolver.resolve(token, null) : request.sessionKey();
        SessionContext ctx = token != null && !token.isBlank()
                ? authService.resolveContext(token) : SessionContext.minimal(sessionKey);
        return ApiResponse.ok(service.join(
                new JoinLessonRequest(request.courseId(), sessionKey), ctx, enhance));
    }

    /**
     * 为原生端（App 内 WebView）提供官方域 Cookie 注入所需的 L1 登录态。
     *
     * <p>博客「免扫码秒进班」链路：在读学生凭官方域 Cookie 打开固定入口即可进入课堂会话，
     * 不需要 checkin、不需要动态码。需登录鉴权；Cookie 仅返回给已登录客户端，
     * 不下发 pptJwt 等课堂凭证，也不写日志。
     */
    @GetMapping("/lessons/entry-cookie")
    public ApiResponse<EntryCookie> entryCookie(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        if (token == null || token.isBlank()) {
            throw new com.jiclub.gongxiaohu.common.BizException(401, "请先登录");
        }
        SessionContext ctx = authService.resolveContext(token);
        String baseUrl = propertiesBaseUrl();
        String domain = baseUrl.replaceAll("/+$", "");
        return ApiResponse.ok(new EntryCookie(domain, ctx.cookieHeader(),
                "请在 App 内 WebView 打开固定入口，注入的 Cookie 才会生效（系统浏览器不共享）"));
    }

    /** 官方域 Cookie 注入信息。 */
    public record EntryCookie(String domain, String cookie, String note) {}

    private String propertiesBaseUrl() {
        return properties.yktGateway() == null || properties.yktGateway().baseUrl().isBlank()
                ? "https://changjiang.yuketang.cn" : properties.yktGateway().baseUrl();
    }

    /**
     * 后端自渲染主方案（用户选定）：仅凭 L1 sessionid 走 lesson-summary 系列，
     * <b>全程不调用 checkin</b>，因此不经过动态码签到、不依赖 pptJwt、不进官方课堂页。
     *
     * <p>课件与题目均由后端拉取后返回给前端自绘。
     * 仅当 {@code enhance=true} 且会话中已有凭证时，才额外用 pptJwt 拉实时随堂题；
     * 这是可选增强，失败自动回落到纯自渲染，绝不阻断主路径。
     */
    @GetMapping("/lessons/{entryId}/render")
    public ApiResponse<ClassroomView> render(
            @PathVariable String entryId,
            @RequestParam(value = "enhance", defaultValue = "false") boolean enhance) {
        JoinLessonService.ClassSession session = service.resolveEntry(entryId);
        String sessionKey = session.sessionContext().sessionId();
        String lessonId = session.lessonId();

        List<Presentation> presentations = List.of();
        YktContentApi api = contentApi.getIfAvailable();
        if (api != null) {
            try {
                presentations = api.fetchPresentations(sessionKey, lessonId);
            } catch (Exception e) {
                log.info("课件拉取失败，返回空课件列表 lessonId={}: {}", lessonId, e.getMessage());
            }
        }

        // 主路径：零签到，只用 L1 sessionid 从 lesson-summary 取题
        List<Quiz> quizzes = inClassQuizService.listQuizzesForSelfRender(sessionKey, lessonId);
        boolean enhanced = false;
        if (enhance && session.credential() != null) {
            try {
                List<Quiz> live = inClassQuizService.listQuizzes(
                        sessionKey, session.credential().pptJwt(), lessonId, "");
                if (!live.isEmpty()) { quizzes = live; enhanced = true; }
            } catch (Exception e) {
                log.info("实时题增强不可用，沿用课件题 lessonId={}: {}", lessonId, e.getMessage());
            }
        }

        String hint = enhanced
                ? "自渲染已启用实时随堂题增强（pptJwt）"
                : "后端自渲染：仅凭登录态读取课件与题目，未调用签到接口、不经过动态码；"
                  + "提交答案仍需在雨课堂本人扫码确认";
        return ApiResponse.ok(new ClassroomView(
                lessonId, session.courseName(), presentations, quizzes,
                session.credential() != null, hint));
    }

    /** 自渲染视图：课件与题目一起返回，前端直接绘制，无需官方页面。 */
    public record ClassroomView(
            String lessonId,
            String courseName,
            List<Presentation> presentations,
            List<Quiz> quizzes,
            boolean credentialReady,
            String hint
    ) {}

    /**
     * 本地防误触校验（§6.3 ③ 降级）：sign 由本地 secret 生成，官方<b>不认</b>，
     * 仅用于本地幂等/防误触，不能称为「服务端校验」。
     */
    @GetMapping("/lessons/verify-sign")
    public ApiResponse<Boolean> verifySign(
            @RequestParam String lessonId,
            @RequestParam long ts,
            @RequestParam String sign) {
        return ApiResponse.ok(service.verifyJoinUrl(lessonId, ts, sign));
    }
}
