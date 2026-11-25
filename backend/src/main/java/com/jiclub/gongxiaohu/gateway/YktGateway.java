package com.jiclub.gongxiaohu.gateway;

import com.jiclub.gongxiaohu.model.AuthModels.AuthCredential;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.Quiz;

import java.util.Optional;

/**
 * 雨课堂上游网关抽象。
 *
 * <p>博客技术路线的核心：客户端不直连官方接口，而是经过「无状态流式中继网关」。
 * 本接口即该网关的抽象层；Mock 实现开箱即用，Real 实现按抓包结果调用官方接口。
 */
public interface YktGateway {

    /** 创建扫码登录票据与二维码。 */
    LoginQr createLoginQr();

    /**
     * 45 秒阻塞长轮询授权状态（小于二维码 60 秒有效期）。
     *
     * @return 扫码确认成功返回凭据；超时或未确认返回 empty
     */
    Optional<AuthCredential> pollAuthStatus(String ticket);

    /**
     * 同上，可指定本次最长等待秒数。
     *
     * <p>2026-11-25 新增：移动端（Android WebView / 运营商 NAT）会把 40 秒以上的空闲连接
     * 静默掐断，服务端把「扫码成功」写回这条已死连接时，前端只会收到网络错误，
     * 表现就是"后端 LOGIN_OK、手机停在二维码页"。前端改用 1 秒内的短轮询后不再有长空闲连接。
     *
     * @param waitSeconds 本次最长等待秒数；<= 0 表示立即返回（短轮询）
     * @return 已确认则返回凭据，否则 empty
     */
    default Optional<AuthCredential> pollAuthStatus(String ticket, long waitSeconds) {
        return pollAuthStatus(ticket);
    }

    /** 获取课表：对应 get_user_course_list。 */
    CourseListResult fetchCourseList(String sessionKey);

    /** 按课程 ID 查进行中课堂；无进行中课堂返回 empty。 */
    Optional<CourseInfo> findLiveLesson(String sessionKey, String courseId);

    /** 用 session_key 换取课堂凭据。 */
    String exchangeLessonCredential(String sessionKey, String lessonId);

    /** 获取题目详情与作答状态。 */
    Optional<Quiz> fetchQuiz(String sessionKey, String quizId);

    /** 按课程拉取题目列表（供超时补答页展示）。 */
    java.util.List<Quiz> fetchQuizzes(String sessionKey, String courseId);

    /** 真正向上游提交答案（幂等由服务层保证）。 */
    boolean pushAnswer(String sessionKey, String quizId, String answer, java.util.List<String> imageUrls);

    /** 课堂信息（网关内部表示）。classroomId 可选，用于和课表匹配。 */
    record CourseInfo(String lessonId, String courseId, String courseName, int status, int currentSlide,
                      String classroomId, String universityId) {
        public CourseInfo(String lessonId, String courseId, String courseName, int status, int currentSlide,
                          String classroomId) {
            this(lessonId, courseId, courseName, status, currentSlide, classroomId, "");
        }
        public CourseInfo(String lessonId, String courseId, String courseName, int status, int currentSlide) {
            this(lessonId, courseId, courseName, status, currentSlide, "", "");
        }
    }
}
