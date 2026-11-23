package com.jiclub.gongxiaohu.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 领域模型集中定义（record，Java 21）。
 * 放在同一文件便于阅读；需要拆分时可按类单独拆文件，包名不变。
 */
public final class Models {
    private Models() {}

    /** 课表条目：status=1 表示老师已点「开始上课」，即进行中课堂。 */
    public record Course(
            String courseId,
            String name,
            String teacher,
            String room,
            String timeRange,
            int weekday,
            int status,          // 1 = 上课中, 0 = 未开课
            int totalSlides,
            int currentSlide
    ) {}

    /** 简化课表查询响应。 */
    public record CourseListResult(
            List<Course> allCourses,
            List<Course> liveCourses,   // status == 1，免扫码秒进班的候选
            String source               // mock | ykt
    ) {}

    /** 进班请求。 */
    public record JoinLessonRequest(
            @NotBlank(message = "courseId 不能为空") String courseId,
            @NotBlank(message = "sessionKey 不能为空") String sessionKey
    ) {}

    /** 进班结果：joinUrl 即博客第 1 节的临时签名链接。 */
    public record JoinLessonResult(
            String lessonId,
            String courseName,
            String joinUrl,
            long issuedAt,
            long expiresAt,
            String credential,     // 模拟用 session_key 换取的课堂凭据
            boolean fromCache
    ) {}

    /** 题目作答形式：前端据此渲染不同输入控件。 */
    public enum QuestionType {
        SINGLE_CHOICE,   // 单选题
        MULTIPLE_CHOICE, // 多选题
        FILL_BLANK,      // 填空题（单空或多空）
        IMAGE_UPLOAD     // 图片上传题
    }

    /** 题目作答开放状态。 */
    public enum QuizOpenState {
        UNPUBLISHED,       // 未发布
        PUBLISHED,         // 已发布，作答中
        TIMEOUT_MAKEUP,    // 倒计时已结束但老师未收题 —— 仍可补交
        CLOSED,            // 老师已结束作答 —— 拒绝提交
        SUBMITTED          // 已提交（仍可覆盖）
    }

    /** 题目详情。type 为中文展示名；questionType 供前端决定控件。 */
    public record Quiz(
            String quizId,
            String courseId,
            String type,
            int score,
            String question,
            QuizOpenState state,
            String correctAnswer,
            QuestionType questionType,
            List<String> options,    // 选择题选项文本，如 ["A. RIP","B. OSPF"]
            List<String> blanks      // 填空空位提示；单空 1 项、多空 N 项
    ) {
        /** 兼容原 7 参构造，按中文题型推断 questionType。 */
        public Quiz(String quizId, String courseId, String type, int score, String question,
                    QuizOpenState state, String correctAnswer) {
            this(quizId, courseId, type, score, question, state, correctAnswer,
                    inferType(type), List.of(), List.of());
        }

        private static QuestionType inferType(String type) {
            if (type == null) return QuestionType.SINGLE_CHOICE;
            return switch (type) {
                case "多选题" -> QuestionType.MULTIPLE_CHOICE;
                case "填空题", "主观题", "简答题" -> QuestionType.FILL_BLANK;
                case "图片上传题" -> QuestionType.IMAGE_UPLOAD;
                default -> QuestionType.SINGLE_CHOICE;
            };
        }

        /** 关键：超时不代表关闭，只有 CLOSED 才拒收。 */
        public boolean acceptsAnswer() {
            return state != QuizOpenState.CLOSED && state != QuizOpenState.UNPUBLISHED;
        }
    }

    /**
     * 提交答案请求。
     * sessionKey 可为空：使用 X-Session-Token 登录时由后端注入；
     * answer 可为空：图片上传题仅提交 imageUrls。
     */
    public record SubmitAnswerRequest(
            String sessionKey,
            @Size(max = 2000, message = "答案最多 2000 字") String answer,
            List<String> imageUrls,
            boolean draftOnly       // true = 仅暂存草稿，不正式提交
    ) {
        public List<String> safeImages() {
            return imageUrls == null ? List.of() : imageUrls;
        }
        public String safeAnswer() {
            return answer == null ? "" : answer;
        }
    }

    /** 答案记录：同一 (quizId, sessionKey) 多次提交覆盖，体现幂等。 */
    public record AnswerRecord(
            String quizId,
            String sessionKeyMasked,
            String answer,
            List<String> imageUrls,
            long submittedAt,
            int submitCount
    ) {}

    /** 提交结果。 */
    public record SubmitResult(
            String quizId,
            boolean accepted,
            String message,
            int submitCount,
            long submittedAt,
            boolean makeup
    ) {}

    /** 草稿。 */
    public record Draft(String quizId, String content, long updatedAt) {}

    /** 图片上传结果。 */
    public record UploadResult(String url, String filename, long size) {}
}
