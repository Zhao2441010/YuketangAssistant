package com.jiclub.gongxiaohu.model;

import java.util.List;

/**
 * 「一门课的全部课件」抓取结果（含尚未播放/尚未上课的课时）。
 *
 * <p>与 {@link ContentModels} 的区别：那一套是单课时、只返回雨课堂已经同步下来的页；
 * 本模型会遍历课程的全部活动，并逐项标记哪些课时的页数不完整（未播放导致 slides 缺失），
 * 供前端明确提示，而不是静默少页。
 */
public final class CoursewareModels {
    private CoursewareModels() {}

    /** 单个课件页。source = summary（已同步）/ jwt（课堂接口补全）。 */
    public record CoursewareSlide(
            int page,
            String imageUrl,
            String problem,
            String source
    ) {}

    /** 一个 PPT 文件。declaredCount 是官方声明的总页数，fetchedCount 是实际拿到的页数。 */
    public record CoursewarePresentation(
            String id,
            String title,
            int declaredCount,
            int fetchedCount,
            List<CoursewareSlide> slides,
            String source
    ) {
        public boolean incomplete() {
            return declaredCount > fetchedCount;
        }
    }

    /** 一个课时（活动）。finished=false 即尚未播放/尚未上课，仍然会抓取。 */
    public record CoursewareLesson(
            String activityId,
            String coursewareId,
            String title,
            boolean finished,
            boolean attended,
            int declaredSlides,
            int fetchedSlides,
            List<CoursewarePresentation> presentations,
            String warning
    ) {}

    /** 整门课的抓取结果。 */
    public record CoursewareBundle(
            String courseId,
            int lessonCount,
            int presentationCount,
            int slideCount,
            int missingPageCount,
            boolean complete,
            List<CoursewareLesson> lessons,
            List<String> warnings
    ) {}

    /** ZIP 下载清单中的一行。 */
    public record CoursewareManifestEntry(
            String fileName, String courseId, String lessonTitle,
            String presentationTitle, int page, String source
    ) {}
}
