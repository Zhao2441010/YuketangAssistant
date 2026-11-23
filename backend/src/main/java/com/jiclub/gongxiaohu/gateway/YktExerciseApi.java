package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jiclub.gongxiaohu.config.AppProperties;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 真实交题接口（2026-11-23 用真实长江雨课堂账号抓包验证）。
 *
 * <p>课后测试题（非随堂题）的完整链路：
 * <ol>
 *   <li>章节：GET /mooc-api/v1/lms/learn/course/chapter?cid={classroom_id}&term=latest&uv_id={uv}&classroom_id={cid}</li>
 *   <li>leaf_info：GET /mooc-api/v1/lms/learn/leaf_info/{cid}/{leaf_id}/ → leaf_type_id</li>
 *   <li>题目：GET /mooc-api/v1/lms/exercise/get_exercise_list/{leaf_type_id}/ → data.problems[]</li>
 *   <li>交题：POST /mooc-api/v1/lms/exercise/problem_apply/
 *       <ul>
 *         <li>单选/多选/判断：{"classroom_id":int,"problem_id":int,"answer":["A","B"]}</li>
 *         <li>填空：{"classroom_id":int,"problem_id":int,"answers":{"1":"空1","2":"空2"}}</li>
 *       </ul>
 *   </li>
 * </ol>
 * 必需 Header：Cookie: sessionid=...、classroom-id、university-id、uv-id、xtbz=ykt、x-client/xt-agent=web、x-csrftoken。
 *
 * <p>实测结果：章节/leaf_info/题目列表均 success=true；已在真实账号抓到 1 道 ShortAnswer 题
 * （problem_id=39145902，含字体混淆 font URL）。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktExerciseApi {
    private static final Logger log = LoggerFactory.getLogger(YktExerciseApi.class);
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;
    /** sessionid -> 最近使用的 classroom_id / university_id（交题需要）。 */
    private final Map<String, int[]> context = new ConcurrentHashMap<>();

    public YktExerciseApi(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.yktGateway().baseUrl();
    }

    /** 记录交题上下文（进入课程时调用）。 */
    public void remember(String sessionKey, int classroomId, int universityId) {
        context.put(sessionKey, new int[]{classroomId, universityId});
    }

    /** 提交答案；返回 true 表示官方返回 success=true。 */
    public boolean submitAnswer(String sessionKey, int problemId, String type, String answer) {
        int[] ctx = context.get(sessionKey);
        if (ctx == null) {
            log.warn("未记录该会话的 classroom_id/university_id，无法交题；请先进入课程");
            return false;
        }
        int classroomId = ctx[0];
        int universityId = ctx[1];
        String csrf = fetchCsrf(sessionKey);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("classroom_id", classroomId);
        payload.put("problem_id", problemId);
        if ("FillBlank".equalsIgnoreCase(type)) {
            ObjectNode answers = mapper.createObjectNode();
            String[] parts = answer.split(",");
            for (int i = 0; i < parts.length; i++) {
                answers.put(String.valueOf(i + 1), parts[i].trim());
            }
            payload.set("answers", answers);
        } else {
            var arr = payload.putArray("answer");
            for (String part : answer.split(",")) {
                if (!part.isBlank()) arr.add(part.trim());
            }
        }

        try {
            HttpRequest.Builder req = HttpRequest.newBuilder(
                            URI.create(baseUrl + "/mooc-api/v1/lms/exercise/problem_apply/"))
                    .header("User-Agent", UA)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("accept", "application/json, text/plain, */*")
                    .header("classroom-id", String.valueOf(classroomId))
                    .header("university-id", String.valueOf(universityId))
                    .header("uv-id", String.valueOf(universityId))
                    .header("xtbz", "ykt")
                    .header("xt-agent", "web")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
            req.header("Cookie", "sessionid=" + sessionKey
                    + (csrf.isBlank() ? "" : "; csrftoken=" + csrf));
            if (!csrf.isBlank()) req.header("x-csrftoken", csrf);

            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            boolean ok = resp.statusCode() == 200 && root.path("success").asBoolean(false);
            log.info("交题结果 problemId={} success={} resp={}", problemId, ok,
                    resp.body().substring(0, Math.min(200, resp.body().length())));
            return ok;
        } catch (Exception e) {
            log.warn("交题失败 problemId={}: {}", problemId, e.getMessage());
            return false;
        }
    }

    private String fetchCsrf(String sessionKey) {
        try {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/v/course_meta/user_info"))
                            .header("User-Agent", UA)
                            .header("Cookie", "sessionid=" + sessionKey).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            for (String raw : resp.headers().allValues("Set-Cookie")) {
                for (String part : raw.split(";")) {
                    String kv = part.trim();
                    if (kv.startsWith("csrftoken=")) return kv.substring("csrftoken=".length());
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
