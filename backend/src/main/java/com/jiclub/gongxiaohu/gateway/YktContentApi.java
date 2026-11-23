package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.ContentModels.Activity;
import com.jiclub.gongxiaohu.model.ContentModels.Lesson;
import com.jiclub.gongxiaohu.model.ContentModels.Presentation;
import com.jiclub.gongxiaohu.model.ContentModels.Slide;
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
 * 课程中心内容接口（课程活动 → 课时 → 课件页 + 教师题目）。
 *
 * <p>路径来自 powercess/yuketang-mcp 实测（2026-11-23 确认）：
 * <ul>
 *   <li>活动列表：GET /v2/api/web/logs/learn/{classroomId}?actype=-1&page=0&offset=50&sort=-1</li>
 *   <li>课时元信息：GET /api/v3/classroom-report/student/lesson-info?lesson_id=...</li>
 *   <li>课件页：GET /api/v3/lesson-summary/student/presentation?presentation_id=&lesson_id=
 *       → slides[].cover（图片直链，无需 Cookie）、slides[].problem（教师题目）</li>
 * </ul>
 * 只需 Cookie: sessionid。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktContentApi {
    private static final Logger log = LoggerFactory.getLogger(YktContentApi.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;

    public YktContentApi(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.yktGateway().baseUrl();
    }

    /** 拉课程的全部活动/课时（分页直到取完）。 */
    public List<Activity> fetchActivities(String sessionId, String classroomId) {
        List<Activity> all = new ArrayList<>();
        final int size = 50;
        for (int page = 0; page < 20; page++) { // 最多 1000 条，防止无限循环
            try {
                // 关键：actype=14 才能拿到课件类活动（下载器实测）。actype=-1 会漏。
                String url = baseUrl + "/v2/api/web/logs/learn/" + encode(classroomId)
                        + "?actype=14&page=" + page + "&offset=" + size + "&sort=-1";
                JsonNode data = get(url, sessionId);
                JsonNode arr = data.path("activities");
                if (!arr.isArray() || arr.isEmpty()) break;
                for (JsonNode a : arr) {
                    int type = a.path("type").asInt(0);
                    all.add(new Activity(
                            text(a, "id"),
                            // 历史课件必须用 courseware_id 作 lesson_id，用 activity.id 会返回 LESSON_DELETED
                            text(a, "courseware_id"),
                            text(a, "title", "name"),
                            type,
                            typeName(type),
                            a.path("create_time").asLong(0),
                            a.path("is_finished").asBoolean(false),
                            a.path("attend_status").asBoolean(false)
                    ));
                }
                if (arr.size() < size) break; // offset 是每页条数，返回不足即取完
            } catch (Exception e) {
                log.warn("拉课程活动失败 classroom={} page={}: {}", classroomId, page, e.getMessage());
                break;
            }
        }
        return all;
    }

    /** 拉课时元信息（含 presentationIds）。 */
    public Lesson fetchLesson(String sessionId, String lessonId) {
        try {
            String url = baseUrl + "/api/v3/classroom-report/student/lesson-info?lesson_id=" + encode(lessonId);
            JsonNode data = get(url, sessionId);
            List<String> pids = new ArrayList<>();
            JsonNode ids = data.path("presentationIds");
            if (ids.isArray()) ids.forEach(n -> {
                if (!n.asText().isBlank()) pids.add(n.asText());
            });
            return new Lesson(
                    text(data, "lessonId", "lesson_id", lessonId),
                    text(data, "lessonName", "lesson_name"),
                    text(data, "courseName", "course_name"),
                    text(data, "classroomName", "classroom_name"),
                    text(data, "teacherName", "teacher_name"),
                    pids
            );
        } catch (Exception e) {
            log.warn("拉课时信息失败 lessonId={}: {}", lessonId, e.getMessage());
            return new Lesson(lessonId, "", "", "", "", List.of());
        }
    }

    /**
     * 拉某个课时的所有课件页（PPT + 教师题目）。
     *
     * <p>关键（来自 ChangJiangPPT-Achiever 下载器实测）：lessonId 参数必须传
     * activity.courseware_id 而非 activity.id，否则历史课时返回 LESSON_DELETED。
     * 流程：lesson-summary/student?lesson_id={coursewareId} 拿 presentations，
     * 再逐个 presentation?presentation_id=&lesson_id= 拿 slides（cover 图片 + problem 题目）。
     */
    public List<Presentation> fetchPresentations(String sessionId, String coursewareId) {
        List<Presentation> out = new ArrayList<>();
        if (coursewareId == null || coursewareId.isBlank()) return out;
        try {
            // 下载器的 pres()：用 courseware_id 作 lesson_id
            String summaryUrl = baseUrl + "/api/v3/lesson-summary/student?lesson_id=" + encode(coursewareId);
            JsonNode summary = get(summaryUrl, sessionId);
            JsonNode presArr = summary.path("presentations");
            if (!presArr.isArray() || presArr.isEmpty()) {
                log.info("课时无课件 coursewareId={} msg={}", coursewareId, summary.path("msg").asText(""));
                return out;
            }
            for (JsonNode p : presArr) {
                String pid = text(p, "id");
                if (pid.isBlank()) continue;
                try {
                    String url = baseUrl + "/api/v3/lesson-summary/student/presentation?presentation_id="
                            + encode(pid) + "&lesson_id=" + encode(coursewareId);
                    JsonNode data = get(url, sessionId);
                    List<Slide> slides = new ArrayList<>();
                    JsonNode arr = data.path("slides");
                    if (arr.isArray()) {
                        int i = 0;
                        for (JsonNode s : arr) {
                            i++;
                            JsonNode problem = s.get("problem");
                            String problemJson = (problem != null && problem.isObject() && !problem.isEmpty())
                                    ? mapper.writeValueAsString(problem) : null;
                            slides.add(new Slide(
                                    text(s, "id"),
                                    s.path("index").asInt(i),
                                    text(s, "cover", "coverAlt", "image_url"),
                                    problemJson
                            ));
                        }
                    }
                    slides.sort((a, b) -> Integer.compare(a.page(), b.page()));
                    out.add(new Presentation(pid,
                            text(p, "title").isBlank() ? "课件 " + (out.size() + 1) : text(p, "title"),
                            text(p, "cover", "coverAlt").isBlank() && !slides.isEmpty() ? slides.get(0).cover() : text(p, "cover", "coverAlt"),
                            p.path("slidesCount").asInt(slides.size()), slides));
                } catch (Exception e) {
                    log.warn("拉课件失败 presentation={}: {}", pid, e.getMessage());
                }
            }
            int problems = out.stream().mapToInt(p -> (int) p.slides().stream().filter(s -> s.problem() != null).count()).sum();
            log.info("历史课件拉取成功 coursewareId={} 课件数={} 含题目页={}", coursewareId, out.size(), problems);
        } catch (Exception e) {
            log.warn("拉课时课件失败 coursewareId={}: {}", coursewareId, e.getMessage());
        }
        return out;
    }

    private JsonNode get(String url, String sessionId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json, text/plain, */*")
                .header("Cookie", "sessionid=" + sessionId)
                .header("xtbz", "ykt")
                .header("x-client", "web")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IllegalStateException("HTTP " + resp.statusCode());
        JsonNode root = mapper.readTree(resp.body());
        if (root.has("code") && root.path("code").asInt(0) != 0) {
            throw new IllegalStateException("code=" + root.path("code").asInt() + " msg=" + root.path("msg").asText());
        }
        if (root.has("success") && !root.path("success").asBoolean(true)) {
            throw new IllegalStateException("msg=" + root.path("msg").asText());
        }
        return root.path("data");
    }

    private static String typeName(int type) {
        return switch (type) {
            case 1 -> "上课";
            case 2 -> "课件";
            case 3 -> "作业";
            case 4 -> "考试";
            case 5 -> "讨论";
            case 14 -> "课堂";     // 实测：雨课堂「上课/课件」活动 type=14
            case 15 -> "作业";
            case 16 -> "考试";
            default -> "活动";
        };
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return "";
    }
}
