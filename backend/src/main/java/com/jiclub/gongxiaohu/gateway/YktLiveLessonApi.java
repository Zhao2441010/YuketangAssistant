package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.AuthModels.SessionContext;
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
 * 进行中课堂的课件：复现官方全屏课堂页自己发出的 {@code GET /v/lesson/lesson_info_v2}。
 *
 * <p>为什么要单独走这条：历史课件用 {@code /api/v3/lesson-summary/student} 能拿到整份 PPT，
 * 但正在进行中的课堂，老师还没翻到的页不会同步到 lesson-summary。官方课堂页是从
 * {@code data.presentationList[]} 里读课件与页的，其中 {@code presentation[]} 含有整份课件的所有页。
 * 这里只做只读拉取，不调用 checkin、不进课堂会话、不绕过动态码。
 *
 * <p>响应结构按版本有差异，解析时对常见字段名做兼容；无法识别的字段通过 warnings 返回，
 * 便于真实开课时补齐，而不是静默丢页。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "real")
public class YktLiveLessonApi {
    private static final Logger log = LoggerFactory.getLogger(YktLiveLessonApi.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String baseUrl;

    public YktLiveLessonApi(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.yktGateway().baseUrl();
    }

    public record LivePresentations(
            String lessonId,
            int code,
            String msg,
            boolean hasUserAuth,
            List<Presentation> presentations,
            List<String> warnings
    ) {}

    /** 拉进行中课堂的全部课件页；接口不可用时返回空列表 + warning，绝不抛异常给前端。 */
    public LivePresentations fetch(SessionContext ctx, String lessonId) {
        List<String> warnings = new ArrayList<>();
        if (ctx == null || ctx.sessionId().isBlank() || lessonId == null || lessonId.isBlank()) {
            return new LivePresentations(lessonId, -1, "缺少登录态或 lessonId", false, List.of(),
                    List.of("缺少登录态或 lessonId，无法拉进行中课堂课件"));
        }
        String url = baseUrl + "/v/lesson/lesson_info_v2?lesson_id="
                + URLEncoder.encode(lessonId, StandardCharsets.UTF_8) + "&source=5";
        try {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("User-Agent", UA)
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Cookie", ctx.cookieHeader())
                            .header("xtbz", "ykt")
                            .header("X-Client", "h5")
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            int code = root.path("code").asInt(-1);
            String msg = root.path("msg").asText("");
            JsonNode data = root.path("data");
            boolean hasAuth = !data.path("userAuth").asText("").isBlank();

            JsonNode list = data.path("presentationList");
            if (!list.isArray() || list.isEmpty()) {
                warnings.add("lesson_info_v2 未返回 presentationList（code=" + code + " msg=" + msg + "）");
                return new LivePresentations(lessonId, code, msg, hasAuth, List.of(), warnings);
            }
            List<Presentation> out = new ArrayList<>();
            int idx = 0;
            for (JsonNode p : list) {
                idx++;
                String pid = text(p, "presentationID", "presentationId", "id");
                if (pid.isBlank()) {
                    warnings.add("第 " + idx + " 份课件缺少 presentationID，已跳过");
                    continue;
                }
                String title = text(p, "title", "name", "presentationName");
                if (title.isBlank()) title = "课件 " + idx;

                // 官方结构：presentationList[].presentation[] 是整份课件的所有页
                JsonNode slidesNode = p.path("presentation");
                if (!slidesNode.isArray() || slidesNode.isEmpty()) slidesNode = p.path("slides");
                List<Slide> slides = new ArrayList<>();
                int i = 0;
                if (slidesNode.isArray()) {
                    for (JsonNode s : slidesNode) {
                        i++;
                        JsonNode problem = s.get("problem");
                        String problemJson = (problem != null && problem.isObject() && !problem.isEmpty())
                                ? mapper.writeValueAsString(problem) : null;
                        int page = s.has("index") || s.has("page") || s.has("pageNum")
                                ? firstInt(s, "index", "page", "pageNum") : i;
                        slides.add(new Slide(
                                text(s, "id", "slideId"),
                                page,
                                text(s, "cover", "coverAlt", "image", "image_url", "url", "ppt"),
                                problemJson));
                    }
                } else {
                    warnings.add("课件《" + title + "》未内嵌页列表，需按 presentationID 单独拉取");
                }
                slides.sort(java.util.Comparator.comparingInt(Slide::page));
                int declared = firstInt(p, "pageCount", "slidesCount", "page_num", "pageNum");
                if (declared <= 0) declared = slides.size();
                out.add(new Presentation(pid, title,
                        text(p, "cover", "coverAlt", "image"), declared, slides));
            }
            log.info("进行中课堂课件拉取 lessonId={} code={} 课件数={} 总页数={}",
                    lessonId, code, out.size(), out.stream().mapToInt(x -> x.slides().size()).sum());
            return new LivePresentations(lessonId, code, msg, hasAuth, out, warnings);
        } catch (Exception e) {
            log.warn("lesson_info_v2 拉课件异常 lessonId={}: {}", lessonId, e.getMessage());
            warnings.add("官方课堂接口请求失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            return new LivePresentations(lessonId, -1, "", false, List.of(), warnings);
        }
    }

    private static String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && !v.asText("").isBlank()) return v.asText();
        }
        return "";
    }

    private static int firstInt(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && v.isNumber()) return v.asInt();
        }
        return 0;
    }
}
