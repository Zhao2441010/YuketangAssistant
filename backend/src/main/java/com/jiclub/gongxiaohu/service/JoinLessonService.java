package com.jiclub.gongxiaohu.service;

import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.LessonCredentialClient;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.model.AuthModels.SessionContext;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.JoinLessonRequest;
import com.jiclub.gongxiaohu.model.Models.JoinLessonResult;
import com.jiclub.gongxiaohu.util.SignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 免扫码进课堂会话服务（技术说明 §7，2026-11-24 认知纠正后重写）。
 *
 * <p><b>核心纠正</b>：官方「动态二维码」是给<b>旁听生</b>的临时进班链接
 * ({@code /v2/web/join_lesson?lesson_id=&sign=&ts=})，
 * 而正规在读学生学号本就在选课名单库中。官方帮助文档列出 5 种进课堂方式，
 * 其中「正在上课」提示是已加入班级学生的独立通道；官方 {@code pc.js} 的
 * {@code getOnLesson()} 拿 onLessonClassrooms 后，{@code openFullscreen()} 直接
 * {@code window.open("/lesson/fullscreen/v3/" + lessonId + "?source=5")}
 * ——<b>无 checkin、无 sign、无 ts、无动态码</b>，仅凭官方域的 L1 Cookie（sessionid）。
 *
 * <p>因此 {@code POST /api/v3/lesson/checkin} 是<b>签到动作</b>（check-in），
 * 不是建立课堂会话的前置条件。旧实现为了拿 pptJwt 主动调用它，
 * 等于自己走进旁听生签到通道，才被 50070 拦住。
 *
 * <p>现在分两条互不依赖的链路：
 * <ol>
 *   <li><b>进课堂会话（主）</b>：开课发现 → 官方固定入口 + 官方域 Cookie 注入。
 *       不调用 checkin，不受动态码约束。Cookie 注入由各端原生能力完成
 *       （Android WebView CookieManager / CDP / 同源反向代理）。</li>
 *   <li><b>自渲染增强（可选）</b>：仅当需要在 App 内自绘课件/题目时，
 *       才按需尝试 checkin 换 pptJwt；失败完全不阻断 ①。</li>
 * </ol>
 *
 * <p>边界：动态码签到（考勤）仍需本人当场扫码，本服务不做、也不尝试绕过。
 */
@Service
public class JoinLessonService {
    private static final Logger log = LoggerFactory.getLogger(JoinLessonService.class);

    private final YktGateway gateway;
    private final AppProperties properties;
    private final ObjectProvider<LessonCredentialClient> credentialClient;

    /**
     * 自渲染会话表：entryId → 课堂上下文。仅内存，进程重启即失效。
     * 前端凭 entryId 调用自渲染接口，后端据此取凭证，凭证本身不出后端。
     */
    private final Map<String, ClassSession> entries = new ConcurrentHashMap<>();

    public JoinLessonService(YktGateway gateway, AppProperties properties,
                             ObjectProvider<LessonCredentialClient> credentialClient) {
        this.gateway = gateway;
        this.properties = properties;
        this.credentialClient = credentialClient;
    }

    /** 课堂自渲染会话上下文（后端内存，绝不下发前端）。 */
    public record ClassSession(
            String lessonId,
            String courseId,
            String courseName,
            SessionContext sessionContext,
            LessonCredentialClient.LessonCredential credential, // 可能为 null（动态码降级）
            long expiresAt
    ) {}

    public CourseListResult getCourseList(String sessionKey) {
        requireSessionKey(sessionKey);
        return gateway.fetchCourseList(sessionKey);
    }

    /**
     * 主动进课堂（用户点击触发）：默认<b>不调用 checkin</b>，只做开课发现 + 返回固定入口。
     * 这是博客「免扫码秒进班」的正确链路，不受动态码约束。
     */
    public JoinLessonResult join(JoinLessonRequest request) {
        return join(request, SessionContext.minimal(request.sessionKey()), false);
    }

    /** 兼容旧调用：带完整 L1 上下文，仍默认不签到。 */
    public JoinLessonResult join(JoinLessonRequest request, SessionContext ctx) {
        return join(request, ctx, false);
    }

    /**
     * @param needCredential 仅当需要在 App 内自绘课件/题目时才传 true，按需尝试 checkin；
     *                       失败完全不阻断进课堂会话
     */
    public JoinLessonResult join(JoinLessonRequest request, SessionContext ctx, boolean needCredential) {
        String sessionKey = request.sessionKey();
        requireSessionKey(sessionKey);
        // ctx 的 sessionId 可能与 request.sessionKey() 不同（导入 Cookie 时），以 ctx 为准
        if (ctx.sessionId().isBlank()) ctx = SessionContext.minimal(sessionKey);

        YktGateway.CourseInfo lesson = gateway.findLiveLesson(sessionKey, request.courseId())
                .orElseThrow(() -> BizException.conflict(
                        "课程 " + request.courseId() + " 当前未开课，无法进班"));

        long now = Instant.now().getEpochSecond();
        long ttl = properties.joinSignature() == null ? 300
                : properties.joinSignature().ttlSeconds();
        String baseUrl = properties.yktGateway() == null || properties.yktGateway().baseUrl().isBlank()
                ? "https://changjiang.yuketang.cn" : properties.yktGateway().baseUrl();
        // §6.2 ④：官方在读学生固定入口，无 sign / 无 ts
        String officialEntry = baseUrl + "/lesson/fullscreen/v3/"
                + lesson.lessonId() + "?source=5";

        // 班级/classroom-id 用于 checkin 头部（若网关解析到）
        if (!ctx.classroomId().isBlank() || !lesson.classroomId().isBlank()) {
            ctx = new SessionContext(ctx.sessionId(), ctx.csrftoken(), ctx.userId(),
                    ctx.universityId(), ctx.uvId(),
                    ctx.classroomId().isBlank() ? lesson.classroomId() : ctx.classroomId(),
                    ctx.rawCookie());
        }

        String cacheKey = ctx.sessionId() + "#" + lesson.lessonId();
        ClassSession existing = entries.get(cacheKey);
        boolean fromCache = existing != null && existing.expiresAt() > System.currentTimeMillis();

        boolean credentialReady = fromCache && existing.credential() != null;
        String hint = needCredential
                ? "已进入课堂会话；正在尝试自渲染增强"
                : "在读学生固定入口已就绪：凭官方域 Cookie 直接进入，无需扫码/动态码";
        LessonCredentialClient.LessonCredential credential = fromCache ? existing.credential() : null;

        // 关键：checkin（签到）只在「自渲染增强」时按需调用，且失败不阻断进课堂。
        // 官方在读学生进课堂不依赖 pptJwt —— 固定入口 + L1 Cookie 即可。
        if (needCredential && !credentialReady) {
            LessonCredentialClient client = credentialClient.getIfAvailable();
            if (client == null) {
                hint = "演示模式：使用内置课件数据，无需真实课堂凭证";
            } else {
                try {
                    credential = client.enterLesson(ctx, lesson.lessonId());
                    credentialReady = true;
                    hint = "已进入课堂会话；自渲染凭证就绪";
                } catch (BizException e) {
                    // checkin 是签到动作，被拒不影响进课堂；仅自渲染降级
                    hint = "已进入课堂会话；自渲染增强不可用（" + e.getMessage() + "）";
                    log.info("自渲染凭证换取失败（不影响进课堂） lessonId={}: {}",
                            lesson.lessonId(), e.getMessage());
                }
            }
        }

        long expiresAtMs = System.currentTimeMillis() + ttl * 1000L;
        String entryId = fromCache ? findEntryId(cacheKey) : UUID.randomUUID().toString().replace("-", "");
        entries.put(entryId, new ClassSession(lesson.lessonId(), request.courseId(),
                lesson.courseName(), ctx, credential, expiresAtMs));

        log.info("建立课堂自渲染会话 course={} lesson={} entryId={} 凭证={}",
                lesson.courseId(), lesson.lessonId(), mask(entryId), credentialReady ? "就绪" : "降级");
        return new JoinLessonResult(
                lesson.lessonId(), lesson.courseName(), entryId, officialEntry,
                credentialReady, hint, now, now + ttl, fromCache);
    }

    /** 按 entryId 取后端自渲染会话（供 /lessons/{entryId}/slides 等接口）。 */
    public ClassSession resolveEntry(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            throw BizException.badRequest("缺少 entryId");
        }
        ClassSession session = entries.get(entryId);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) {
            entries.remove(entryId);
            throw new BizException(401, "课堂会话已过期，请重新点击进入课堂");
        }
        return session;
    }

    /**
     * 本地防误触校验（§6.3 ③ 降级）：sign 由本地 secret 生成，官方<b>不认</b>，
     * 仅用于本地幂等/防误触，不能称为「服务端校验」。
     */
    public boolean verifyJoinUrl(String lessonId, long ts, String sign) {
        if (properties.joinSignature() == null) return false;
        return SignUtil.verify(properties.joinSignature().secret(), lessonId, ts, sign,
                properties.joinSignature().ttlSeconds());
    }

    /** 登出时清除该会话的所有 entry。 */
    public void evict(String sessionId) {
        entries.entrySet().removeIf(e ->
                e.getValue().sessionContext().sessionId().equals(sessionId));
    }

    private String findEntryId(String cacheKey) {
        // 按 sessionId#lessonId 反查已有 entryId，避免重复建会话
        String[] parts = cacheKey.split("#", 2);
        for (Map.Entry<String, ClassSession> e : entries.entrySet()) {
            ClassSession s = e.getValue();
            if (s.sessionContext().sessionId().equals(parts[0])
                    && s.lessonId().equals(parts[1])
                    && s.expiresAt() > System.currentTimeMillis()) return e.getKey();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static void requireSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw BizException.badRequest("缺少 session_key");
        }
    }

    private static String mask(String id) {
        return id == null || id.length() < 8 ? "****" : id.substring(0, 8) + "****";
    }
}
