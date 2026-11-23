package com.jiclub.gongxiaohu.model;

import java.util.List;

/** 课程中心（课程 → 课时 → 课件/题目）三级下钻的数据模型。 */
public final class ContentModels {
    private ContentModels() {}

    /** 课程活动/课时条目。 */
    public record Activity(
            String activityId,     // 活动 id
            String coursewareId,   // 课件 id——历史课件必须用这个作 lesson_id 才能取到（关键！）
            String title,
            int type,              // 活动类型码（实测上课 type=14）
            String typeName,       // 类型中文名：上课/作业/考试等
            long createTime,       // 毫秒时间戳
            boolean finished,      // 是否已完成
            boolean attended       // 是否出勤
    ) {}

    /** 课时元信息 + 课件清单。 */
    public record Lesson(
            String lessonId,
            String lessonName,
            String courseName,
            String classroomName,
            String teacherName,
            List<String> presentationIds
    ) {}

    /** 课件页（PPT 页 + 可能携带的教师题目）。 */
    public record Slide(
            String id,
            int page,              // 1-based 页码
            String cover,          // 可直接下载的图片地址（无需 Cookie）
            String problem         // 教师题目原始 JSON（null 表示无题）
    ) {}

    /** 课件（一个或多个 PPT 文件）。 */
    public record Presentation(
            String id,
            String title,
            String cover,
            int slidesCount,
            List<Slide> slides
    ) {}
}
