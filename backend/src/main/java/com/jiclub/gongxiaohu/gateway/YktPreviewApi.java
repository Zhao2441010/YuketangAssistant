package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.Models.QuestionType;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 提前获取题目（博客第 5 节「提前预览题目」）。
 *
 * <p>雨课堂把随堂客观题随课件一起下发：老师发题前，题目已存在于课件幻灯片的
 * {@code problem} 字段中，因此可以提前拉取预览。链路（来源 powercess/yuketang-mcp 实测）：
 * <ol>
 *     <li>正在上课：GET /api/v3/classroom/on-lesson-upcoming-exam → lessonId</li>
 *     <li>课时元信息：GET /api/v3/classroom-report/student/lesson-info?lesson_id=... → presentationIds</li>
 *     <li>课件全页：GET /api/v3/lesson-summary/student/presentation?presentation_id=&lesson_id= → slides[]</li>
 *     <li>每页的 {@code problem} 对象即题目，可能尚未发布（state=UNPUBLISHED）</li>
 * </ol>
 *
 * <p>鉴权只需 {@code Cookie: sessionid}；题目图片无需 Cookie。响应成功字段可能是
 * code/errcode/success 三套之一，这里统一宽松判断。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktPreviewApi {
    private static final Logger log = LoggerFactory.getLogger(YktPreviewApi.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;

    public YktPreviewApi(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.yktGateway().baseUrl();
    }

    /** 按 lessonId 提前拉取该课堂所有课件页上的题目。 */
    public List<Quiz> fetchPreviewQuizzes(String sessionId, String lessonId, String courseId) {
        if (lessonId == null || lessonId.isBlank()) {
            log.warn("lessonId 为空，无法提前获取题目");
            return List.of();
        }
        try {
            JsonNode info = get("/api/v3/classroom-report/student/lesson-info",
                    "lesson_id", lessonId, sessionId);
            List<String> presentationIds = new ArrayList<>();
            JsonNode ids = info.path("presentationIds");
            if (ids.isArray()) {
                ids.forEach(n -> presentationIds.add(n.asText()));
            }
            if (presentationIds.isEmpty()) {
                // 兼容另一套结构：课堂小结里也带 presentations
                JsonNode summary = get("/api/v3/lesson-summary/student", "lesson_id", lessonId, sessionId);
                JsonNode pres = summary.path("presentations");
                if (pres.isArray()) {
                    pres.forEach(n -> {
                        String id = text(n, "id", "presentationId", "");
                        if (!id.isBlank()) presentationIds.add(id);
                    });
                }
            }
            log.info("提前获取题目：lessonId={} 课件数={}", lessonId, presentationIds.size());

            List<Quiz> quizzes = new ArrayList<>();
            for (String pid : presentationIds) {
                JsonNode slidesRoot = get("/api/v3/lesson-summary/student/presentation",
                        new String[][]{{"presentation_id", pid}, {"lesson_id", lessonId}}, sessionId);
                JsonNode slides = slidesRoot.path("slides");
                if (!slides.isArray()) continue;
                int page = 0;
                for (JsonNode slide : slides) {
                    page++;
                    JsonNode problem = slide.path("problem");
                    if (problem.isNull() || !problem.isObject() || problem.isEmpty()) continue;
                    Quiz quiz = parseProblem(problem, courseId, page);
                    if (quiz != null) quizzes.add(quiz);
                }
            }
            return quizzes;
        } catch (Exception e) {
            log.warn("提前获取题目失败 lessonId={}: {}", lessonId, e.getMessage());
            return List.of();
        }
    }

    /** 把 slides[].problem 宽松解析为 Quiz（字段名可能有多个版本）。 */
    public static Quiz parseProblem(JsonNode p, String courseId, int page) {
        String problemId = text(p, "problem_id", "problemId", "id", "question_id", "");
        String typeText = text(p, "type", "question_type", "questionType", "题型", "单选题");
        String body = text(p, "body", "question", "stem", "title", "content", "");
        // 有些版本题干是 HTML，粗去标签
        body = body.replaceAll("<[^>]+>", "").trim();
        if (body.isEmpty() && problemId.isEmpty()) {
            return null;
        }
        String answer = text(p, "answer", "correct_answer", "correctAnswer", "");

        List<String> options = new ArrayList<>();
        JsonNode opts = firstArray(p, "options", "option_list", "choices");
        if (opts != null) {
            for (JsonNode o : opts) {
                if (o.isTextual()) options.add(o.asText());
                else {
                    String key = text(o, "key", "label", "");
                    String val = text(o, "value", "text", "content", "");
                    options.add(key.isEmpty() ? val : key + ". " + val);
                }
            }
        }

        int score = 0;
        JsonNode sc = firstPresent(p, "score", "Score", "point");
        if (sc != null && sc.isNumber()) score = sc.asInt();

        // 判断发布状态：problem 里若明确未发布则 UNPUBLISHED（仍可预览，不可提交）
        String status = text(p, "status", "state", "submission_status", "");
        QuizOpenState state = (status.contains("unpublish") || status.contains("not_start") || "0".equals(status))
                ? QuizOpenState.UNPUBLISHED : QuizOpenState.PUBLISHED;

        return new Quiz(
                problemId.isEmpty() ? "page-" + page : problemId,
                courseId,
                typeText,
                score,
                body,
                state,
                answer,
                inferType(typeText),
                options,
                List.of()
        );
    }

    private JsonNode get(String path, String queryKey, String queryValue, String sessionId) throws Exception {
        return get(path, new String[][]{{queryKey, queryValue}}, sessionId);
    }

    private JsonNode get(String path, String[][] query, String sessionId) throws Exception {
        StringBuilder url = new StringBuilder(baseUrl).append(path).append('?');
        for (int i = 0; i < query.length; i++) {
            if (i > 0) url.append('&');
            url.append(query[i][0]).append('=').append(java.net.URLEncoder.encode(query[i][1], java.nio.charset.StandardCharsets.UTF_8));
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(url.toString()))
                .header("User-Agent", UA)
                .header("Accept", "application/json, text/plain, */*")
                .header("Cookie", "sessionid=" + sessionId)
                .header("xtbz", "ykt")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        JsonNode root = mapper.readTree(resp.body());
        // 三套成功字段：code / errcode / success，宽松判断
        if (root.has("code") && root.path("code").asInt(0) != 0) {
            throw new IllegalStateException("业务错误 code=" + root.path("code").asInt()
                    + " msg=" + root.path("msg").asText(""));
        }
        if (root.has("errcode") && root.path("errcode").asInt(0) != 0) {
            throw new IllegalStateException("业务错误 errcode=" + root.path("errcode").asInt());
        }
        if (root.has("success") && !root.path("success").asBoolean(true)) {
            throw new IllegalStateException("业务错误 msg=" + root.path("msg").asText(""));
        }
        return root.path("data");
    }

    private static QuestionType inferType(String typeText) {
        if (typeText == null) return QuestionType.SINGLE_CHOICE;
        String t = typeText.toLowerCase();
        if (typeText.contains("多选") || t.contains("multiple")) return QuestionType.MULTIPLE_CHOICE;
        if (typeText.contains("填空") || typeText.contains("简答") || typeText.contains("主观")
                || t.contains("blank") || t.contains("shortanswer")) return QuestionType.FILL_BLANK;
        if (typeText.contains("图片") || typeText.contains("上传") || t.contains("image")) return QuestionType.IMAGE_UPLOAD;
        return QuestionType.SINGLE_CHOICE;
    }

    private static String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return "";
    }

    private static JsonNode firstArray(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && v.isArray()) return v;
        }
        return null;
    }

    private static JsonNode firstPresent(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull()) return v;
        }
        return null;
    }
}
