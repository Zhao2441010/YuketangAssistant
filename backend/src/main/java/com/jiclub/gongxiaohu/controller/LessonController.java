package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.LessonSessionClient;
import com.jiclub.gongxiaohu.gateway.YktContentApi;
import com.jiclub.gongxiaohu.gateway.YktGateway;
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
    private final ObjectProvider<LessonSessionClient> lessonSessionClient;
    private final AppProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

    public LessonController(JoinLessonService service, SessionResolver sessionResolver,
                            AuthService authService, InClassQuizService inClassQuizService,
                            ObjectProvider<YktContentApi> contentApi,
                            ObjectProvider<LessonSessionClient> lessonSessionClient,
                            AppProperties properties,
                            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.service = service;
        this.sessionResolver = sessionResolver;
        this.authService = authService;
        this.inClassQuizService = inClassQuizService;
        this.contentApi = contentApi;
        this.lessonSessionClient = lessonSessionClient;
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

    /**
     * 只读探测：复现官方课堂页自己发出的 {@code GET /v/lesson/lesson_info_v2}，验证在读学生能否免扫码进班。
     * 只返回业务码与字段有无；userAuth 不出后端、不写日志。服务端若返回 50070 则如实回报，不做绕过。
     */
    @GetMapping("/lessons/lesson-info")
    public ApiResponse<LessonInfoProbe> lessonInfo(
            @RequestParam String courseId,
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        if (token == null || token.isBlank()) {
            throw new com.jiclub.gongxiaohu.common.BizException(401, "请先登录");
        }
        SessionContext ctx = authService.resolveContext(token);
        YktGateway.CourseInfo lesson = service.findLiveLesson(ctx.sessionId(), courseId);
        String url = propertiesBaseUrl() + "/v/lesson/lesson_info_v2?lesson_id="
                + java.net.URLEncoder.encode(lesson.lessonId(), java.nio.charset.StandardCharsets.UTF_8)
                + "&source=5";
        LessonInfoProbe probe;
        try {
            // 身份值优先取登录态；缺省时从课表 CourseInfo 补齐（university_id/uv_id/classroom_id）
            String uni = !ctx.universityId().isBlank() ? ctx.universityId() : lesson.universityId();
            String uv = !ctx.uvId().isBlank() ? ctx.uvId() : lesson.universityId();
            String cls = !ctx.classroomId().isBlank() ? ctx.classroomId() : lesson.classroomId();
            // Cookie 头：sessionid+csrftoken，并补 university_id/uv_id（官方 set_cookie 依赖 Cookie 里的这两个值）
            String cookieHeader = ctx.cookieHeader();
            if (!cookieHeader.toLowerCase().contains("university_id") && !uni.isBlank()) {
                cookieHeader += "; university_id=" + uni;
            }
            if (!cookieHeader.toLowerCase().contains("uv_id") && !uv.isBlank()) {
                cookieHeader += "; uv_id=" + uv;
            }
            java.net.http.HttpRequest.Builder rb = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Cookie", cookieHeader)
                    .header("xtbz", "ykt")
                    .header("X-Client", "h5");
            // 官方课堂页：window.userid 有值就带 X-UID；POST 场景还带 X-CSRFToken。
            // 缺 X-UID / classroom-id / university-id / uv-id 是 lesson_info_v2 返回 500 的直接原因
            // （官方服务端 'NoneType' object has no attribute 'set_cookie'）。
            if (!ctx.userId().isBlank()) rb.header("X-UID", ctx.userId());
            if (!ctx.csrftoken().isBlank()) rb.header("X-CSRFToken", ctx.csrftoken());
            if (!uni.isBlank()) rb.header("university-id", uni);
            if (!uv.isBlank()) rb.header("uv-id", uv);
            if (!cls.isBlank()) rb.header("classroom-id", cls);
            java.net.http.HttpResponse<String> resp = httpClient.send(rb.GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(resp.body());
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            boolean hasAuth = !data.path("userAuth").asText("").isBlank();
            int code = root.path("code").isNumber() ? root.path("code").asInt() : -1;
            String msg = root.path("msg").asText("");
            String note = hasAuth
                    ? "在读学生可凭登录态换取课堂凭据（官方同款请求）"
                    : code == 50070
                        ? "进班被动态二维码拦截，只能本人扫码"
                        : code == 50000
                            ? "登录态已失效，请重新登录"
                            : "未返回课堂凭据：" + msg;
            // 非 200/非 0 时，把官方原始返回（脱敏）记进日志，便于定位 500 根因
            String bodyFingerprint = resp.statusCode() == 200 && code == 0 ? ""
                    : scrub(resp.body());
            log.info("lesson_info_v2 探测 lessonId={} http={} code={} 有userAuth={} 有csrftoken={} 有uvId={} uni={} cls={} 官方返回={}",
                    lesson.lessonId(), resp.statusCode(), code, hasAuth,
                    !ctx.csrftoken().isBlank(), !uv.isBlank(),
                    uni.isBlank() ? "-" : "***", cls.isBlank() ? "-" : "***", bodyFingerprint);
            probe = new LessonInfoProbe(lesson.lessonId(), lesson.courseName(),
                    resp.statusCode(), code, msg, hasAuth,
                    data.path("presentationList").isArray() ? data.path("presentationList").size() : 0,
                    data.path("quizList").isArray() ? data.path("quizList").size() : 0,
                    data.path("classroom").isObject(), note);
        } catch (Exception e) {
            log.warn("lesson_info_v2 探测异常 lessonId={}: {}", lesson.lessonId(), e.getMessage());
            throw com.jiclub.gongxiaohu.common.BizException.conflict("探测请求失败：" + e.getMessage());
        }
        return ApiResponse.ok(probe);
    }

    /** 官方 lesson_info_v2 探测结果（不含任何凭据）。 */
    public record LessonInfoProbe(
            String lessonId, String courseName, int httpStatus, int code, String msg,
            boolean hasUserAuth, int presentationCount, int quizCount, boolean hasClassroom, String note) {}

    /**
     * 免扫码进班：在读学生凭登录态直接建立官方课堂会话（lesson_info_v2 → userAuth → WSS hello），
     * 全程不扫码、不 checkin。开动态二维码的课堂若被 50070 拦截则如实回报、不做绕过。
     * 课堂凭据 userAuth 只留在后端内存，不回传、不写日志。
     */
    @PostMapping("/lessons/enter")
    public ApiResponse<LessonSessionClient.EnterResult> enter(
            @RequestParam String courseId,
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        if (token == null || token.isBlank()) {
            throw new com.jiclub.gongxiaohu.common.BizException(401, "请先登录");
        }
        SessionContext ctx = authService.resolveContext(token);
        YktGateway.CourseInfo lesson = service.findLiveLesson(ctx.sessionId(), courseId);
        LessonSessionClient client = lessonSessionClient.getIfAvailable();
        if (client == null) {
            // mock 模式：无真实网关，返回演示结果
            return ApiResponse.ok(new LessonSessionClient.EnterResult(
                    lesson.lessonId(), lesson.courseName(), true, true, true,
                    "", 0, "mock", "演示模式：已免扫码进班（未连真实课堂）"));
        }
        return ApiResponse.ok(client.enter(ctx, lesson));
    }

    /** 官方域 Cookie 注入信息。 */
    public record EntryCookie(String domain, String cookie, String note) {}

    private String propertiesBaseUrl() {        return properties.yktGateway() == null || properties.yktGateway().baseUrl().isBlank()
                ? "https://changjiang.yuketang.cn" : properties.yktGateway().baseUrl();
    }

    /** 官方返回体脱敏：去掉 userAuth/token/cookie 等明文，只留业务码/msg/结构，便于日志定位 500。 */
    private static String scrub(String body) {
        if (body == null || body.isBlank()) return "(空)";
        String v = body
                .replaceAll("(?i)(userAuth|authorization|token|sessionid|csrftoken|auth)[\"']?\\s*[:=]\\s*[\"'][^\"']*[\"']", "$1=***")
                .replaceAll("(?i)\"?(userAuth|authorization|sessionid|csrftoken)\"?\\s*[:=]\\s*\"[^\"]{0,200}\"", "$1=***");
        if (v.length() > 500) v = v.substring(0, 500) + "…";
        return v;
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
