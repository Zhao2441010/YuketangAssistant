package com.jiclub.gongxiaohu.service;

import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.JoinLessonRequest;
import com.jiclub.gongxiaohu.model.Models.JoinLessonResult;
import com.jiclub.gongxiaohu.util.SignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 免扫码秒进班（博客第 1 节）。
 *
 * <p>流程：
 * <ol>
 *     <li>请求课表 get_user_course_list</li>
 *     <li>过滤 status == 1 的进行中课堂</li>
 *     <li>用本地 session_key 换取课堂凭据</li>
 *     <li>签发带短期时间戳签名的 join_lesson 链接</li>
 * </ol>
 *  note: 签名链接由服务端校验，避免客户端伪造 lessonId。
 */
@Service
public class JoinLessonService {
    private static final Logger log = LoggerFactory.getLogger(JoinLessonService.class);

    private final YktGateway gateway;
    private final AppProperties properties;
    // 内存缓存：凭据短时复用，进程重启即失效（符合"不落地隐私"原则）
    private final ConcurrentHashMap<String, CachedCredential> credentialCache = new ConcurrentHashMap<>();

    public JoinLessonService(YktGateway gateway, AppProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    public CourseListResult getCourseList(String sessionKey) {
        requireSessionKey(sessionKey);
        return gateway.fetchCourseList(sessionKey);
    }

    public JoinLessonResult join(JoinLessonRequest request) {
        String sessionKey = request.sessionKey();
        requireSessionKey(sessionKey);

        YktGateway.CourseInfo lesson = gateway.findLiveLesson(sessionKey, request.courseId())
                .orElseThrow(() -> BizException.conflict(
                        "课程 " + request.courseId() + " 当前未开课（status != 1），无法免扫码进班"));

        String cacheKey = sessionKey.hashCode() + ":" + lesson.lessonId();
        CachedCredential cached = credentialCache.get(cacheKey);
        boolean fromCache = cached != null && cached.expiresAt() > System.currentTimeMillis();
        String credential = fromCache ? cached.credential()
                : gateway.exchangeLessonCredential(sessionKey, lesson.lessonId());

        if (!fromCache) {
            long ttlMs = properties.joinSignature().ttl().toMillis();
            credentialCache.put(cacheKey,
                    new CachedCredential(credential, System.currentTimeMillis() + ttlMs));
        }

        long ts = Instant.now().getEpochSecond();
        String secret = properties.joinSignature().secret();
        String sign = SignUtil.sign(secret, lesson.lessonId(), ts);
        String joinUrl = UriComponentsBuilder
                .fromHttpUrl("https://changjiang.ykt.io")
                .path("/v2/web/join_lesson")
                .queryParam("lesson_id", lesson.lessonId())
                .queryParam("sign", sign)
                .queryParam("ts", ts)
                .build()
                .toUriString();

        log.info("签发进班链接 course={} lesson={} ts={}", lesson.courseId(), lesson.lessonId(), ts);
        return new JoinLessonResult(
                lesson.lessonId(),
                lesson.courseName(),
                joinUrl,
                ts,
                ts + properties.joinSignature().ttlSeconds(),
                credential,
                fromCache
        );
    }

    /** 校验签名链接是否有效（供前端或课堂页在未跳转前预检）。 */
    public boolean verifyJoinUrl(String lessonId, long ts, String sign) {
        return SignUtil.verify(properties.joinSignature().secret(), lessonId, ts, sign,
                properties.joinSignature().ttlSeconds());
    }

    private static void requireSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw BizException.badRequest("缺少 session_key");
        }
    }

    private record CachedCredential(String credential, long expiresAt) {}
}
