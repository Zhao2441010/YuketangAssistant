package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jiclub.gongxiaohu.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 随堂题接口（区别于课后测试题 YktExerciseApi）。
 *
 * <p>协议来源：长江雨课堂开源项目 Chiu-xaH/Fuck-Yangtze-RainClassroom（2026-11-23 抓包验证接口存在）。
 * 老师上课推送的随堂题随课件下发，即使倒计时结束，只要老师未点"结束作答"，提交接口仍可接收且幂等覆盖
 * —— 这正是"超时补答"的技术基础。
 *
 * <ol>
 *   <li>签到换 JWT：POST /api/v3/lesson/checkin
 *       body {"source":21,"lessonId":"...","joinIfNotIn":true}
 *       → 响应头 Set-Auth = pptJwt，body.data.lessonToken = socketJwt，data.identityId</li>
 *   <li>拉课件题：GET /api/v3/lesson/presentation/fetch?presentation_id={pid}
 *       Header Authorization: Bearer {pptJwt} → data.slides[].problem
 *       problem 字段：problemId / problemType(1单选2多选3投票4填空5主观) / body / options / answers</li>
 *   <li>提交：POST /api/v3/lesson/problem/answer
 *       Header Authorization: Bearer {pptJwt}
 *       body {"problemId":..,"problemType":..,"dt":毫秒时间戳,"result":答案}</li>
 * </ol>
 *
 * <p>result 格式：选择/投票 = ["A"]；填空 = {"1":"答案"}；主观 = {"content":"...","attachments":{"filelist":[]}}。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktInClassQuizApi {
    private static final Logger log = LoggerFactory.getLogger(YktInClassQuizApi.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;

    public YktInClassQuizApi(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.yktGateway().baseUrl();
    }

    /** 签到并换取 pptJwt（课件题与提交都需要它）。 */
    public String checkinForJwt(String sessionId, String lessonId) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("source", 21); // 二维码来源
            body.put("lessonId", String.valueOf(lessonId));
            body.put("joinIfNotIn", true);

            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/api/v3/lesson/checkin"))
                            .header("User-Agent", UA)
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Content-Type", "application/json")
                            .header("Cookie", "sessionid=" + sessionId)
                            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            String jwt = resp.headers().firstValue("Set-Auth").orElse("");
            JsonNode root = mapper.readTree(resp.body());
            int code = root.path("code").asInt(0);
            if (code != 0) {
                log.warn("随堂签到失败 lessonId={} code={} msg={}", lessonId, code, root.path("msg").asText());
                return "";
            }
            if (jwt.isBlank()) {
                log.warn("随堂签到未返回 Set-Auth JWT lessonId={}", lessonId);
                return "";
            }
            return jwt;
        } catch (Exception e) {
            log.warn("随堂签到异常 lessonId={}: {}", lessonId, e.getMessage());
            return "";
        }
    }

    /**
     * 拉取某节课的所有随堂题（课件里已下发的题，含未发布的）。
     * 返回 List<RawProblem>，调用方可转成 Quiz 或用于超时补答。
     */
    public List<RawProblem> fetchProblems(String sessionId, String pptJwt, String lessonId,
                                          String presentationId) {
        List<RawProblem> result = new ArrayList<>();
        if (pptJwt.isBlank() || presentationId.isBlank()) return result;
        try {
            String url = baseUrl + "/api/v3/lesson/presentation/fetch?presentation_id="
                    + URLEncoder.encode(presentationId, StandardCharsets.UTF_8);
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("User-Agent", UA)
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Cookie", "sessionid=" + sessionId)
                            .header("Authorization", "Bearer " + pptJwt)
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            // 历史/已归档课堂用 jwt 的 fetch 会 401，fallback 到 lesson-summary（coursewareId 作 lesson_id）
            if (resp.statusCode() != 200 || root.path("code").asInt(0) != 0) {
                log.info("presentation/fetch 不可用，fallback 到 lesson-summary（lessonId={}）", lessonId);
                return fetchProblemsViaSummary(sessionId, lessonId, presentationId);
            }
            JsonNode slides = root.path("data").path("slides");
            if (!slides.isArray()) return result;
            result.addAll(parseSlides(slides));
            log.info("随堂题拉取：presentation={} 题数={}", presentationId, result.size());
        } catch (Exception e) {
            log.warn("拉随堂题异常 presentation={}: {}", presentationId, e.getMessage());
        }
        return result;
    }

    /** fallback：用 lesson-summary/student/presentation 拉题（无需 jwt，coursewareId 作 lesson_id）。 */
    private List<RawProblem> fetchProblemsViaSummary(String sessionId, String lessonId, String presentationId) {
        List<RawProblem> result = new ArrayList<>();
        try {
            String url = baseUrl + "/api/v3/lesson-summary/student/presentation?presentation_id="
                    + URLEncoder.encode(presentationId, StandardCharsets.UTF_8)
                    + "&lesson_id=" + URLEncoder.encode(lessonId, StandardCharsets.UTF_8);
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("User-Agent", UA)
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Cookie", "sessionid=" + sessionId)
                            .header("xtbz", "ykt")
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            if (root.path("code").asInt(0) != 0) {
                log.warn("lesson-summary 拉题失败 code={} msg={}", root.path("code").asInt(), root.path("msg").asText());
                return result;
            }
            result.addAll(parseSlides(root.path("data").path("slides")));
            log.info("lesson-summary 随堂题拉取：题数={}", result.size());
        } catch (Exception e) {
            log.warn("lesson-summary 拉题异常: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 解析 slides 中的 problem，兼容两种真实结构：
     * <ul>
     *   <li>老结构：problem 直接含 body/options/problemType</li>
     *   <li>新结构（实测历史课件）：problem.content 才是题目正文，problemId 在 content 里，
     *       option 为 {key,value}，answer 为数组</li>
     * </ul>
     */
    private List<RawProblem> parseSlides(JsonNode slides) {
        List<RawProblem> result = new ArrayList<>();
        if (!slides.isArray()) return result;
        int page = 0;
        for (JsonNode slide : slides) {
            page++;
            JsonNode outer = slide.path("problem");
            if (!outer.isObject() || outer.isEmpty()) continue;
            // 展开 content 嵌套层
            JsonNode p = outer.has("content") && outer.path("content").isObject()
                    ? outer.path("content") : outer;
            String pid = text(outer, "problemId", "problem_id", "id");
            if (pid.isBlank()) pid = text(p, "problemId", "problem_id", "id");
            int type = p.path("problemType").asInt(outer.path("problemType").asInt(0));
            String body = text(p, "body", "title", "stem");
            body = body.replaceAll("<[^>]+>", "").trim();
            List<String> options = new ArrayList<>();
            JsonNode opts = p.path("options");
            if (opts.isArray()) {
                for (JsonNode o : opts) {
                    if (o.isTextual()) options.add(o.asText());
                    else options.add(text(o, "key", "label") + ". " + text(o, "value", "text", "content"));
                }
            }
            // answers 非空表示已作答（历史课件里是 answer 数组）
            boolean answered = (p.path("answers").isArray() && p.path("answers").size() > 0)
                    || (p.path("answer").isArray() && p.path("answer").size() > 0);
            result.add(new RawProblem(pid, type, body, options, page,
                    text(slide, "cover", "coverAlt"), answered));
        }
        return result;
    }

    /**
     * 提交随堂答案（超时补答核心：只要老师未结束作答即可提交，重复提交幂等覆盖）。
     *
     * @param problemType 题型码：1单选 2多选 3投票 4填空 5主观
     * @param answer      选择传 "A" 或 "A,B"；填空传 "答案1,答案2"；主观传正文
     */
    public boolean submitAnswer(String sessionId, String pptJwt, String problemId,
                                int problemType, String answer) {
        if (pptJwt.isBlank()) {
            log.warn("pptJwt 为空，无法提交随堂答案");
            return false;
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("problemId", problemId);
            body.put("problemType", problemType);
            body.put("dt", System.currentTimeMillis());
            body.set("result", buildResult(problemType, answer));

            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/api/v3/lesson/problem/answer"))
                            .header("User-Agent", UA)
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Content-Type", "application/json")
                            .header("Cookie", "sessionid=" + sessionId)
                            .header("Authorization", "Bearer " + pptJwt)
                            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            int code = root.path("code").asInt(0);
            boolean ok = resp.statusCode() == 200 && code == 0;
            log.info("随堂答案提交 problemId={} type={} success={} msg={}",
                    problemId, problemType, ok, root.path("msg").asText(""));
            return ok;
        } catch (Exception e) {
            log.warn("随堂答案提交异常 problemId={}: {}", problemId, e.getMessage());
            return false;
        }
    }

    /** 按题型构造 result 字段（静态，便于单测）。 */
    public static JsonNode buildResult(int problemType, String answer) {
        ObjectMapper m = new ObjectMapper();
        // 投票/单选/多选：字符串数组
        if (problemType == 1 || problemType == 2 || problemType == 3) {
            ArrayNode arr = m.createArrayNode();
            for (String part : answer.split(",")) {
                if (!part.isBlank()) arr.add(part.trim());
            }
            return arr;
        }
        // 填空：{"1":"答案1","2":"答案2"}
        if (problemType == 4) {
            ObjectNode obj = m.createObjectNode();
            String[] parts = answer.split(",");
            for (int i = 0; i < parts.length; i++) {
                obj.put(String.valueOf(i + 1), parts[i].trim());
            }
            return obj;
        }
        // 主观题：{"content":"...","attachments":{"filelist":[]}}
        ObjectNode obj = m.createObjectNode();
        obj.put("content", answer);
        ObjectNode attachments = m.createObjectNode();
        attachments.set("filelist", m.createArrayNode());
        obj.set("attachments", attachments);
        return obj;
    }

    private static String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return "";
    }

    /** 随堂题原始数据（未转 Quiz，供补答页使用）。 */
    public record RawProblem(String problemId, int problemType, String body,
                             List<String> options, int page, String imageUrl, boolean answered) {}
}
