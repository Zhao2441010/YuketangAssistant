package com.jiclub.gongxiaohu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.gateway.YktInClassQuizApi;
import com.jiclub.gongxiaohu.model.Models.QuestionType;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import com.jiclub.gongxiaohu.model.Models.SubmitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 随堂超时补答服务。
 *
 * <p>核心：老师推送的随堂题随课件下发，倒计时结束但老师未收题时仍可提交且幂等覆盖。
 * 本服务缓存每节课的 pptJwt（签到换取），供拉题/提交复用；答题记录按 (problemId, session) 去重覆盖。
 */
@Service
public class InClassQuizService {
    private static final Logger log = LoggerFactory.getLogger(InClassQuizService.class);

    private final ObjectProvider<YktInClassQuizApi> api; // real 模式才有
    private final com.jiclub.gongxiaohu.gateway.YktGateway gateway;
    private final Map<String, String> jwtCache = new ConcurrentHashMap<>();      // key=sessionKey|lessonId
    private final Map<String, Integer> submitCount = new ConcurrentHashMap<>();  // 幂等计数

    public InClassQuizService(ObjectProvider<YktInClassQuizApi> api,
                              com.jiclub.gongxiaohu.gateway.YktGateway gateway) {
        this.api = api;
        this.gateway = gateway;
    }

    private String cacheKey(String sessionKey, String lessonId) {
        return sessionKey + "|" + lessonId;
    }

    /** 签到换取 pptJwt 并缓存。 */
    public String ensureJwt(String sessionKey, String lessonId) {
        YktInClassQuizApi real = api.getIfAvailable();
        if (real == null) return "mock-jwt"; // 演示模式
        String key = cacheKey(sessionKey, lessonId);
        String cached = jwtCache.get(key);
        if (cached != null && !cached.isBlank()) return cached;
        String jwt = real.checkinForJwt(sessionKey, lessonId);
        if (jwt.isBlank()) {
            throw BizException.conflict("随堂签到失败（课程可能未开始或已结束），无法获取题目");
        }
        jwtCache.put(key, jwt);
        return jwt;
    }

    /** 按课程 ID 解析出正在上课的 lessonId（前端只需传 courseId）。 */
    public String resolveLessonId(String sessionKey, String courseId) {
        return gateway.findLiveLesson(sessionKey, courseId)
                .map(com.jiclub.gongxiaohu.gateway.YktGateway.CourseInfo::lessonId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> BizException.conflict("该课程当前没有进行中的课堂，无法获取随堂题"));
    }

    /** 拉取随堂题并转为 Quiz。 */
    public List<Quiz> listQuizzes(String sessionKey, String lessonId, String presentationId) {
        YktInClassQuizApi real = api.getIfAvailable();
        if (real == null) return mockQuizzes(lessonId);
        String jwt = ensureJwt(sessionKey, lessonId);
        List<YktInClassQuizApi.RawProblem> raw = real.fetchProblems(sessionKey, jwt, lessonId, presentationId);
        List<Quiz> quizzes = new ArrayList<>();
        for (YktInClassQuizApi.RawProblem p : raw) {
            quizzes.add(new Quiz(
                    p.problemId(),
                    lessonId,
                    typeName(p.problemType()),
                    p.page(),
                    p.body(),
                    p.answered() ? QuizOpenState.SUBMITTED : QuizOpenState.PUBLISHED,
                    "",
                    questionType(p.problemType()),
                    p.options(),
                    List.of()
            ));
        }
        return quizzes;
    }

    /** 提交答案（超时补答）。重复提交幂等覆盖，计数自增。 */
    public SubmitResult submit(String sessionKey, String lessonId, String problemId,
                               int problemType, String answer) {
        YktInClassQuizApi real = api.getIfAvailable();
        if (real == null) {
            return new SubmitResult(problemId, true, "演示提交成功",
                    bump(problemId, sessionKey), Instant.now().toEpochMilli(), false);
        }
        String jwt = ensureJwt(sessionKey, lessonId);
        boolean ok = real.submitAnswer(sessionKey, jwt, problemId, problemType, answer);
        if (!ok) {
            throw BizException.conflict("上游提交失败（老师可能已收题），请确认后重试");
        }
        int count = bump(problemId, sessionKey);
        log.info("随堂补答提交成功 problemId={} 第{}次", problemId, count);
        return new SubmitResult(problemId, true,
                count > 1 ? "补交成功（已覆盖此前答案）" : "提交成功",
                count, Instant.now().toEpochMilli(), false);
    }

    private int bump(String problemId, String sessionKey) {
        String key = problemId + ":" + Integer.toHexString(sessionKey.hashCode());
        int n = submitCount.getOrDefault(key, 0) + 1;
        submitCount.put(key, n);
        return n;
    }

    private static String typeName(int type) {
        return switch (type) {
            case 1 -> "单选题";
            case 2 -> "多选题";
            case 3 -> "投票题";
            case 4 -> "填空题";
            case 5 -> "主观题";
            default -> "单选题";
        };
    }

    private static QuestionType questionType(int type) {
        return switch (type) {
            case 2, 3 -> QuestionType.MULTIPLE_CHOICE;
            case 4, 5 -> QuestionType.FILL_BLANK;
            default -> QuestionType.SINGLE_CHOICE;
        };
    }

    /** 演示模式的随堂题（四题型，含一道"已超时可补交"示例）。 */
    private static List<Quiz> mockQuizzes(String lessonId) {
        return List.of(
                new Quiz("inclass-1", lessonId, "单选题", 5,
                        "以下哪个是动态路由协议？", QuizOpenState.TIMEOUT_MAKEUP, "",
                        QuestionType.SINGLE_CHOICE, List.of("A. RIP", "B. 静态路由", "C. 直连路由"), List.of()),
                new Quiz("inclass-2", lessonId, "多选题", 10,
                        "以下哪些属于 IGP？", QuizOpenState.PUBLISHED, "",
                        QuestionType.MULTIPLE_CHOICE, List.of("A. RIP", "B. OSPF", "C. BGP"), List.of()),
                new Quiz("inclass-3", lessonId, "填空题", 5,
                        "OSPF 的全称是 ____", QuizOpenState.PUBLISHED, "",
                        QuestionType.FILL_BLANK, List.of(), List.of("空 1"))
        );
    }

    /** 清空会话的 jwt 缓存（登出时调用）。 */
    public void evict(String sessionKey) {
        jwtCache.keySet().removeIf(k -> k.startsWith(sessionKey + "|"));
    }
}
