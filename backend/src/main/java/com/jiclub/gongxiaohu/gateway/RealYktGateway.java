package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.AuthModels.AuthCredential;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.Models.Course;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import com.jiclub.gongxiaohu.model.Models.QuestionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 真实模式网关：按配置调用雨课堂官方接口。
 *
 * <p><b>重要说明</b>：博客只公开了少量接口细节（auth-status、join_lesson），
 * 其余路径与响应字段需你自行抓包后在本类或 application.yml 中补齐。
 * 本类使用 JsonNode 做宽松解析，字段名按需调整即可，不影响上层服务。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class RealYktGateway implements YktGateway {
    private static final Logger log = LoggerFactory.getLogger(RealYktGateway.class);

    private final AppProperties.YktGateway props;
    private final RestClient client;
    private final YktWebSocketLogin wsLogin;
    private final LessonCredentialClient credentialClient;

    public RealYktGateway(AppProperties properties, YktWebSocketLogin wsLogin,
                          LessonCredentialClient credentialClient) {
        this.props = properties.yktGateway();
        this.wsLogin = wsLogin;
        this.credentialClient = credentialClient;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.readTimeoutMs()));
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .defaultHeader("User-Agent", "GongXiaoHu-Android/3.0.0")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** 鉴权头：真实网页端登录成功后拿到的是 sessionid（Cookie），保持兼容两种形式。 */
    private void auth(org.springframework.http.HttpHeaders headers, String sessionKey) {
        headers.set("Cookie", sessionKey.startsWith("sessionid=")
                ? sessionKey : "sessionid=" + sessionKey);
        headers.set("X-Session-Key", sessionKey);
    }

    // ==================== 扫码登录（真实接口） ====================

    @Override
    public LoginQr createLoginQr() {
        // 实测可用的 WebSocket 协议（标准区域 www.yuketang.cn）
        return wsLogin.createLoginQr();
    }

    @Override
    public Optional<AuthCredential> pollAuthStatus(String ticket) {
        // 等待扫码确认：WebSocket 收到 loginsuccess 后用 UserID+Auth 换 sessionid
        return wsLogin.pollAuthStatus(ticket);
    }

    /** 短轮询入口：waitSeconds<=0 时立即返回，避免移动端长空闲连接被掐断。 */
    @Override
    public Optional<AuthCredential> pollAuthStatus(String ticket, long waitSeconds) {
        return wsLogin.pollAuthStatus(ticket, waitSeconds);
    }

    /**
     * 正在上课检测：使用 v3 接口（来源 powercess/yuketang-mcp，注释指明旧的
     * v/course_meta/on_lesson_courses 会漏报）。返回 onLessonClassrooms 数组。
     */
    public List<CourseInfo> fetchLiveLessons(String sessionKey) {
        String path = props.endpoint("live-list-v3");
        JsonNode root = client.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .headers(h -> auth(h, sessionKey))
                .retrieve()
                .body(JsonNode.class);
        List<CourseInfo> result = new ArrayList<>();
        if (root == null) return result;
        JsonNode lessons = root.path("data").path("onLessonClassrooms");
        if (!lessons.isArray()) lessons = root.path("onLessonClassrooms");
        for (JsonNode n : lessons) {
            // 记录原始字段名与 role/status，定位「onLessonClassrooms 是否混入非上课条目」
            java.util.Set<String> fieldNames = new java.util.LinkedHashSet<>();
            n.fieldNames().forEachRemaining(fieldNames::add);
            log.info("onLessonClassroom 字段={} role={} status={} lessonId={}",
                    fieldNames, n.path("role").asText("-"), n.path("status").asText("-"),
                    text(n, "lessonId", "lesson_id", ""));
            result.add(new CourseInfo(
                    text(n, "lessonId", "lesson_id", ""),
                    text(n, "courseId", "course_id", ""),
                    text(n, "courseName", "course_name", ""),
                    1,
                    0,
                    text(n, "classroomId", "classroom_id", "")
            ));
        }
        return result;
    }

    @Override
    public CourseListResult fetchCourseList(String sessionKey) {
        // ✅ 2026-11-23 实测（长江雨课堂）：GET /v2/api/web/courses/list?identity=2
        // 返回 {"errcode":0,"data":{"list":[{name, teacher:{name}, classroom_id, course:{name}, time(ms)...}]}}
        String path = props.endpoint("course-list");
        JsonNode root = client.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("identity", 2)
                        .queryParam("format", "json").build())
                .headers(h -> auth(h, sessionKey))
                .retrieve()
                .body(JsonNode.class);

        JsonNode list = root == null ? null : root.path("data").path("list");
        if (list == null || !list.isArray()) {
            list = root != null && root.path("data").path("course_list").isArray()
                    ? root.path("data").path("course_list") : null;
        }
        // 正在上课的 courseId 集合（v3 live 接口）；失败则为空，不影响课表展示
        java.util.Set<String> liveIds = new java.util.HashSet<>();
        try {
            for (CourseInfo live : fetchLiveLessons(sessionKey)) {
                if (!live.courseId().isBlank()) liveIds.add(live.courseId());
                if (!live.classroomId().isBlank()) liveIds.add(live.classroomId());
                if (!live.lessonId().isBlank()) liveIds.add(live.lessonId());
                log.info("正在上课: courseId={} classroomId={} lessonId={} name={}",
                        live.courseId(), live.classroomId(), live.lessonId(), live.courseName());
            }
        } catch (Exception e) {
            log.warn("获取正在上课列表失败，status 全部按 0 处理：{}", e.getMessage());
        }

        List<Course> courses = new ArrayList<>();
        if (list != null) {
            for (JsonNode n : list) {
                String teacher = text(n.path("teacher"), "name", "teacher_name", "");
                String classroomId = text(n, "classroom_id", "id", "");
                String courseId = text(n.path("course"), "id", "");
                String id = classroomId.isEmpty()
                        ? (courseId.isEmpty() ? text(n, "course_id", "") : courseId) : classroomId;
                // 课表无 status 字段：用 live 接口的 courseId/classroomId 判定是否正在上课
                int status = liveIds.contains(courseId) || liveIds.contains(classroomId) ? 1 : 0;
                // §14.4：university_id / classroom_id 是 lesson_info_v2 等课堂接口的必需身份头
                String universityId = text(n.path("course"), "university_id", "universityId", "");
                courses.add(new Course(
                        id,
                        text(n.path("course"), "name", "").isEmpty()
                                ? text(n, "name", "未命名课程") : text(n.path("course"), "name"),
                        teacher,
                        text(n, "room", "classroom", text(n.path("course"), "university_name", "")),
                        n.path("time").asText(""),
                        n.path("weekday").asInt(0),
                        status,
                        n.path("total_slides").asInt(0),
                        n.path("current_slide").asInt(0),
                        universityId,
                        classroomId
                ));
            }
        }
        return new CourseListResult(courses,
                courses.stream().filter(c -> c.status() == 1).toList(), "ykt");
    }

    @Override
    public Optional<CourseInfo> findLiveLesson(String sessionKey, String courseId) {
        // 优先用 v3 live 接口返回的真实 lessonId（课件题预览需要它）
        try {
            // 课表里带 university_id / classroom_id，用于给 live 结果补齐身份头
            String uniId = "", clsId = "";
            try {
                for (Course c : fetchCourseList(sessionKey).allCourses()) {
                    if (c.courseId().equals(courseId) || c.classroomId().equals(courseId)) {
                        uniId = c.universityId(); clsId = c.classroomId(); break;
                    }
                }
            } catch (Exception ignored) { /* 课表失败不影响 live 匹配 */ }

            for (CourseInfo live : fetchLiveLessons(sessionKey)) {
                boolean match = live.courseId().equals(courseId)
                        || live.classroomId().equals(courseId)
                        || (!live.lessonId().isBlank() && live.lessonId().equals(courseId));
                if (match) {
                    String u = live.universityId().isBlank() ? uniId : live.universityId();
                    String c = live.classroomId().isBlank() ? clsId : live.classroomId();
                    return Optional.of(new CourseInfo(live.lessonId(), live.courseId(),
                            live.courseName(), live.status(), live.currentSlide(), c, u));
                }
            }
        } catch (Exception e) {
            log.debug("live 接口查询 lessonId 失败: {}", e.getMessage());
        }
        return fetchCourseList(sessionKey).liveCourses().stream()
                .filter(c -> c.courseId().equals(courseId))
                .findFirst()
                .map(c -> new CourseInfo("lesson-" + c.courseId(), c.courseId(),
                        c.name(), c.status(), c.currentSlide(), c.classroomId(), c.universityId()));
    }

    @Override
    public String exchangeLessonCredential(String sessionKey, String lessonId) {
        // §6.5 第 1 项修正：旧实现把 sessionid 原样当「课堂凭据」，等于没有换。
        // 真正的课堂态凭证是 checkin 换出的三元组，pptJwt 才是课件/题目的 Bearer。
        String sid = sessionKey.startsWith("sessionid=")
                ? sessionKey.substring("sessionid=".length()) : sessionKey;
        try {
            return credentialClient.pptJwt(
                    com.jiclub.gongxiaohu.model.AuthModels.SessionContext.minimal(sid), lessonId);
        } catch (Exception e) {
            log.info("课堂凭证换取不可用 lessonId={}: {}", lessonId, e.getMessage());
            return "";
        }
    }

    @Override
    public Optional<Quiz> fetchQuiz(String sessionKey, String quizId) {
        String path;
        try {
            path = props.endpoint("quiz-detail");
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
        try {
            JsonNode root = client.get()
                    .uri(uriBuilder -> uriBuilder.path(path)
                            .queryParam("quiz_id", quizId)
                            .queryParam("format", "json").build())
                    .headers(h -> auth(h, sessionKey))
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || root.isMissingNode()) {
                return Optional.empty();
            }
            return Optional.of(parseQuiz(quizId, root.path("data")));
        } catch (Exception e) {
            log.warn("拉取题目详情失败（占位接口），返回 empty: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Quiz> fetchQuizzes(String sessionKey, String courseId) {
        // 随堂题接口尚未抓包；quiz-list 为占位路径，失败时优雅返回空列表，避免整页 500。
        String path;
        try {
            path = props.endpoint("quiz-list");
        } catch (IllegalStateException e) {
            log.debug("未配置 quiz-list 端点，返回空题目列表");
            return List.of();
        }
        try {
            JsonNode root = client.get()
                    .uri(uriBuilder -> uriBuilder.path(path)
                            .queryParam("course_id", courseId)
                            .queryParam("format", "json").build())
                    .headers(h -> auth(h, sessionKey))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode list = root == null ? null : root.path("data").path("quiz_list");
            if (list == null || !list.isArray()) {
                list = root != null && root.path("data").isArray() ? root.path("data") : null;
            }
            List<Quiz> quizzes = new ArrayList<>();
            if (list != null) {
                for (JsonNode n : list) {
                    quizzes.add(parseQuiz(text(n, "quiz_id", "id", ""), n));
                }
            }
            return quizzes;
        } catch (Exception e) {
            log.warn("拉取题目失败（占位接口或随堂题未抓包），返回空列表: {}", e.getMessage());
            return List.of();
        }
    }

    private static Quiz parseQuiz(String quizId, JsonNode n) {
        String typeText = text(n, "type", "question_type", "单选题");
        List<String> options = new ArrayList<>();
        JsonNode opts = n.path("options");
        if (opts.isArray()) {
            opts.forEach(o -> options.add(o.isTextual() ? o.asText() : o.path("text").asText()));
        }
        List<String> blanks = new ArrayList<>();
        JsonNode blankNode = n.path("blanks");
        if (blankNode.isArray()) {
            blankNode.forEach(b -> blanks.add(b.asText()));
        }
        QuestionType questionType = switch (typeText) {
            case "多选题" -> QuestionType.MULTIPLE_CHOICE;
            case "填空题", "主观题", "简答题" -> QuestionType.FILL_BLANK;
            case "图片上传题" -> QuestionType.IMAGE_UPLOAD;
            default -> QuestionType.SINGLE_CHOICE;
        };
        return new Quiz(quizId,
                text(n, "course_id", "", ""),
                typeText,
                n.path("score").asInt(0),
                text(n, "question", "title", ""),
                mapState(text(n, "state", "status", "")),
                text(n, "correct_answer", "answer", ""),
                questionType,
                options,
                blanks
        );
    }

    @Override
    public boolean pushAnswer(String sessionKey, String quizId, String answer, List<String> imageUrls) {
        String path = props.endpoint("submit-answer");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("quiz_id", quizId);
        form.add("answer", answer);
        form.add("images", String.join(",", imageUrls));
        JsonNode root = client.post()
                .uri(UriComponentsBuilder.fromPath(path).build().toUri())
                .headers(h -> auth(h, sessionKey))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        return root != null && (root.path("code").asInt(0) == 0 || root.path("success").asBoolean(false));
    }

    private static QuizOpenState mapState(String raw) {
        return switch (raw) {
            case "unpublished" -> QuizOpenState.UNPUBLISHED;
            case "closed", "ended", "finish" -> QuizOpenState.CLOSED;
            case "timeout_makeup", "makeup" -> QuizOpenState.TIMEOUT_MAKEUP;
            case "submitted" -> QuizOpenState.SUBMITTED;
            default -> QuizOpenState.PUBLISHED;
        };
    }

    /** 宽容取字符串：按多个候选字段名依次尝试。 */
    private static String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return keys.length > 0 ? node.path(keys[keys.length - 1]).asText() : "";
    }
}
