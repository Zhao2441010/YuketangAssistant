package com.jiclub.gongxiaohu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.gateway.LessonCredentialClient;
import com.jiclub.gongxiaohu.gateway.YktContentApi;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.gateway.YktInClassQuizApi;
import com.jiclub.gongxiaohu.gateway.YktLiveLessonApi;
import com.jiclub.gongxiaohu.model.AuthModels.SessionContext;
import com.jiclub.gongxiaohu.model.ContentModels.Activity;
import com.jiclub.gongxiaohu.model.ContentModels.Presentation;
import com.jiclub.gongxiaohu.model.ContentModels.Slide;
import com.jiclub.gongxiaohu.model.CoursewareModels;
import com.jiclub.gongxiaohu.model.CoursewareModels.CoursewareBundle;
import com.jiclub.gongxiaohu.model.CoursewareModels.CoursewareLesson;
import com.jiclub.gongxiaohu.model.CoursewareModels.CoursewareManifestEntry;
import com.jiclub.gongxiaohu.model.CoursewareModels.CoursewarePresentation;
import com.jiclub.gongxiaohu.model.CoursewareModels.CoursewareSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 抓取一门课的**全部课件**，包括尚未播放 / 尚未上课的课时。
 *
 * <p>现有 {@code /content/slides} 只处理用户点开的那个课时，而且雨课堂的 lesson-summary
 * 只同步老师已经翻到过的页。这里改为：遍历课程的所有活动（activity），逐个用
 * {@code courseware_id} 拉课件清单与每一页，再显式报告「官方声明页数 vs 实际拿到页数」，
 * 不静默丢页。对确实缺页的课件，可选择用 pptJwt 从课堂接口补全（需 checkin，可能被动态码拦截）。
 *
 * <p>ZIP 里只放封面图片直链下载得到的图片 + 一份 manifest.csv，不落任何 Cookie / token。
 */
@Service
public class FullCoursewareService {
    private static final Logger log = LoggerFactory.getLogger(FullCoursewareService.class);

    private final ObjectProvider<YktContentApi> contentApi;
    private final ObjectProvider<YktInClassQuizApi> quizApi;
    private final ObjectProvider<LessonCredentialClient> credentialClient;
    private final ObjectProvider<YktLiveLessonApi> liveLessonApi;
    private final JoinLessonService joinLessonService;
    private final AuthService authService;
    private final HttpClient http = HttpClient.newHttpClient();

    public FullCoursewareService(ObjectProvider<YktContentApi> contentApi,
                                 ObjectProvider<YktInClassQuizApi> quizApi,
                                 ObjectProvider<LessonCredentialClient> credentialClient,
                                 ObjectProvider<YktLiveLessonApi> liveLessonApi,
                                 JoinLessonService joinLessonService,
                                 AuthService authService) {
        this.contentApi = contentApi;
        this.quizApi = quizApi;
        this.credentialClient = credentialClient;
        this.liveLessonApi = liveLessonApi;
        this.joinLessonService = joinLessonService;
        this.authService = authService;
    }

    /**
     * @param token      本地会话 token
     * @param courseId   课程 id（课表里的 classroomId）
     * @param useJwt     缺页时是否尝试用 pptJwt 补全（会触发 checkin，默认关闭）
     */
    public CoursewareBundle fetchAll(String token, String courseId, boolean useJwt) {
        YktContentApi api = contentApi.getIfAvailable();
        if (api == null) return mockBundle(courseId);

        SessionContext ctx = authService.resolveContext(token);
        String sid = ctx.sessionId();
        List<Activity> activities = api.fetchActivities(sid, courseId);
        log.info("全量课件抓取 courseId={} 活动数={}", courseId, activities.size());

        List<String> warnings = new ArrayList<>();
        List<CoursewareLesson> lessons = new ArrayList<>();
        int presentationCount = 0, slideCount = 0, missing = 0;

        // ① 进行中课堂：走官方课堂页自己的 lesson_info_v2，才能拿到老师还没翻到的页
        YktLiveLessonApi liveApi = liveLessonApi.getIfAvailable();
        if (liveApi != null) {
            try {
                YktGateway.CourseInfo liveLesson = joinLessonService.findLiveLesson(sid, courseId);
                if (liveLesson != null && !liveLesson.lessonId().isBlank()) {
                    YktLiveLessonApi.LivePresentations live = liveApi.fetch(ctx, liveLesson);
                    warnings.addAll(live.warnings());
                    List<CoursewarePresentation> cps = new ArrayList<>();
                    int declared = 0, fetched = 0;
                    for (Presentation p : live.presentations()) {
                        CoursewarePresentation cp = convert(p, sid, ctx,
                                new Activity("LIVE-" + liveLesson.lessonId(), liveLesson.lessonId(),
                                        p.title(), 14, "进行中", 0, false, false),
                                useJwt, warnings);
                        cps.add(cp);
                        declared += cp.declaredCount();
                        fetched += cp.fetchedCount();
                        missing += Math.max(0, cp.declaredCount() - cp.fetchedCount());
                    }
                    presentationCount += cps.size();
                    slideCount += fetched;
                    String warning = cps.stream().anyMatch(CoursewarePresentation::incomplete)
                            ? "官方声明 " + declared + " 页，实际拿到 " + fetched + " 页；未播放页可能尚未同步" : "";
                    lessons.add(new CoursewareLesson("LIVE-" + liveLesson.lessonId(), liveLesson.lessonId(),
                            "【进行中】" + (liveLesson.courseName().isBlank() ? liveLesson.lessonId() : liveLesson.courseName()),
                            false, false, declared, fetched, cps, warning));
                    log.info("全量课件抓取：进行中课堂 lessonId={} 课件数={} 页数={}",
                            liveLesson.lessonId(), cps.size(), fetched);
                }
            } catch (Exception e) {
                warnings.add("进行中课堂课件抓取失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }

        // ② 历史课时：遍历全部活动，包括尚未播放 / 尚未上课的
        for (Activity activity : activities) {
            // 没有 courseware_id 的活动（考试/作业）拿不到课件，跳过但记一笔
            if (activity.coursewareId() == null || activity.coursewareId().isBlank()) {
                warnings.add("课时《" + activity.title() + "》没有课件 id，已跳过");
                continue;
            }
            List<Presentation> presentations = api.fetchPresentations(sid, activity.coursewareId());
            List<CoursewarePresentation> converted = new ArrayList<>();
            int declaredTotal = 0, fetchedTotal = 0;

            for (Presentation p : presentations) {
                CoursewarePresentation cp = convert(p, sid, ctx, activity, useJwt, warnings);
                converted.add(cp);
                declaredTotal += cp.declaredCount();
                fetchedTotal += cp.fetchedCount();
                missing += Math.max(0, cp.declaredCount() - cp.fetchedCount());
            }
            presentationCount += converted.size();
            slideCount += fetchedTotal;

            String warning = converted.stream().anyMatch(CoursewarePresentation::incomplete)
                    ? "官方声明 " + declaredTotal + " 页，实际拿到 " + fetchedTotal
                            + " 页；未播放页可能尚未同步，可尝试「用课堂凭证补全」"
                    : "";
            lessons.add(new CoursewareLesson(
                    activity.activityId(), activity.coursewareId(), activity.title(),
                    activity.finished(), activity.attended(),
                    declaredTotal, fetchedTotal, converted, warning));
        }

        return new CoursewareBundle(courseId, lessons.size(), presentationCount, slideCount,
                missing, missing == 0, lessons, warnings);
    }

    /** 把单份 Presentation 转成完整课件页，必要时用 pptJwt 补全缺页。 */
    private CoursewarePresentation convert(Presentation p, String sid, SessionContext ctx,
                                           Activity activity, boolean useJwt, List<String> warnings) {
        Map<Integer, CoursewareSlide> pages = new LinkedHashMap<>();
        for (Slide s : p.slides()) {
            pages.put(s.page(), new CoursewareSlide(s.page(), s.cover(), s.problem(), "summary"));
        }
        int declared = Math.max(p.slidesCount(), pages.size());

        if (useJwt && declared > pages.size()) {
            YktInClassQuizApi quiz = quizApi.getIfAvailable();
            LessonCredentialClient client = credentialClient.getIfAvailable();
            if (quiz == null || client == null) {
                warnings.add("当前为演示模式，无法用课堂凭证补全课件");
            } else {
                try {
                    String jwt = client.pptJwt(ctx, activity.coursewareId());
                    List<Slide> full = quiz.fetchFullSlides(sid, jwt, p.id());
                    for (Slide s : full) {
                        CoursewareSlide existing = pages.get(s.page());
                        // 已同步页优先；只在缺页或旧页无图片时用课堂接口结果补齐
                        if (existing == null || (existing.imageUrl() == null || existing.imageUrl().isBlank())) {
                            pages.put(s.page(), new CoursewareSlide(
                                    s.page(), s.cover(),
                                    s.problem() != null ? s.problem() : (existing == null ? null : existing.problem()),
                                    existing == null ? "jwt" : existing.source() + "+jwt"));
                        }
                    }
                } catch (Exception e) {
                    warnings.add("课件《" + p.title() + "》用课堂凭证补全失败："
                            + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }
            }
        }

        List<CoursewareSlide> slides = new ArrayList<>(pages.values());
        slides.sort(java.util.Comparator.comparingInt(CoursewareSlide::page));
        String source = slides.stream().anyMatch(s -> s.source().contains("jwt")) ? "summary+jwt" : "summary";
        return new CoursewarePresentation(p.id(), p.title(), declared, slides.size(), slides, source);
    }

    /** 把所有课件页打包成 ZIP（逐页图片 + manifest.csv），直接写响应流，不占大内存。 */
    public void writeZip(String token, String courseId, boolean useJwt, OutputStream out) throws IOException {
        CoursewareBundle bundle = fetchAll(token, courseId, useJwt);
        ZipOutputStream zip = new ZipOutputStream(out);
        List<CoursewareManifestEntry> manifest = new ArrayList<>();
        try {
            for (int li = 0; li < bundle.lessons().size(); li++) {
                CoursewareLesson lesson = bundle.lessons().get(li);
                String lessonDir = String.format("%02d_%s/", li + 1, safe(lesson.title()));
                for (int pi = 0; pi < lesson.presentations().size(); pi++) {
                    CoursewarePresentation pres = lesson.presentations().get(pi);
                    String presDir = lessonDir + String.format("%02d_%s/", pi + 1, safe(pres.title()));
                    for (CoursewareSlide slide : pres.slides()) {
                        String name = presDir + String.format("page_%03d.jpg", slide.page());
                        boolean written = slide.imageUrl() != null && !slide.imageUrl().isBlank()
                                && writeImage(slide.imageUrl(), name, zip);
                        if (!written) {
                            String noteName = presDir + String.format("page_%03d.txt", slide.page());
                            putText(zip, noteName, "该页暂无图片直链（可能尚未同步）。source=" + slide.source());
                            name = noteName;
                        }
                        manifest.add(new CoursewareManifestEntry(name, courseId, lesson.title(),
                                pres.title(), slide.page(), slide.source()));
                    }
                }
            }
            StringBuilder csv = new StringBuilder("\uFEFF课时,课件,页码,来源,文件\n");
            for (CoursewareManifestEntry e : manifest) {
                csv.append(csvField(e.lessonTitle())).append(',')
                        .append(csvField(e.presentationTitle())).append(',')
                        .append(e.page()).append(',')
                        .append(e.source()).append(',')
                        .append(csvField(e.fileName())).append('\n');
            }
            putText(zip, "manifest.csv", csv.toString());
            putText(zip, "README.txt", "工小湖全量课件抓取\n课程 id: " + courseId
                    + "\n课时数: " + bundle.lessonCount() + "，课件数: " + bundle.presentationCount()
                    + "，页数: " + bundle.slideCount() + "，缺页: " + bundle.missingPageCount()
                    + "\n仅包含你自己账号可见的课件，请勿传播。\n");
        } finally {
            zip.finish();
            zip.flush();
        }
    }

    private boolean writeImage(String url, String entryName, ZipOutputStream zip) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET().build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200 || resp.body().length == 0) return false;
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(resp.body());
            zip.closeEntry();
            return true;
        } catch (Exception e) {
            log.debug("课件图片下载失败 {}：{}", entryName, e.getMessage());
            return false;
        }
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String safe(String s) {
        String v = (s == null || s.isBlank()) ? "untitled" : s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return v.length() > 60 ? v.substring(0, 60) : v;
    }

    private static String csvField(String s) {
        String v = s == null ? "" : s;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** 演示模式：两个课时，其中一节课尚未播放（finished=false），照样返回全部页。 */
    private CoursewareBundle mockBundle(String courseId) {
        List<CoursewareLesson> lessons = new ArrayList<>();
        CoursewarePresentation pres = new CoursewarePresentation("pres-1", "课件 1", 3, 3,
                List.of(
                        new CoursewareSlide(1, "", null, "summary"),
                        new CoursewareSlide(2, "", null, "summary"),
                        new CoursewareSlide(3, "", "{\"body\":\"演示题\"}", "summary")),
                "summary");
        lessons.add(new CoursewareLesson("act-1", "cw-1", "第 1 讲 · 已播放", true, true, 3, 3, List.of(pres), ""));
        lessons.add(new CoursewareLesson("act-2", "cw-2", "第 2 讲 · 尚未播放", false, false, 3, 3, List.of(pres), ""));
        return new CoursewareBundle(courseId, lessons.size(), 2, 6, 0, true, lessons, List.of());
    }
}
