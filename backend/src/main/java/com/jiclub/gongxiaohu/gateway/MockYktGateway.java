package com.jiclub.gongxiaohu.gateway;

import com.jiclub.gongxiaohu.model.AuthModels.AuthCredential;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import com.jiclub.gongxiaohu.model.Models;
import com.jiclub.gongxiaohu.model.Models.Course;
import com.jiclub.gongxiaohu.model.Models.CourseListResult;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import com.jiclub.gongxiaohu.model.Models.QuestionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 演示模式网关：内置与前端一致的课程/题目数据，无需真实凭证即可跑通全流程。
 */
@Component
@ConditionalOnProperty(prefix = "gongxiaohu", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockYktGateway implements YktGateway {
    private static final Logger log = LoggerFactory.getLogger(MockYktGateway.class);
    private static final int POLL_TIMEOUT_SECONDS = 45; // 单次长轮询校验时长（< 二维码有效期 60s）

    /** ticket -> 授权结果占位；前端轮询在此挂起，直到「模拟扫码」触发 complete。 */
    private final Map<String, CompletableFuture<AuthCredential>> pendingTickets = new ConcurrentHashMap<>();

    private static final List<Course> COURSES = List.of(
            new Course("net", "计算机网络", "陈建国 教授", "2609(多媒体)", "10:25-12:00", 1, 1, 230, 147),
            new Course("python", "Python程序设计", "林志远 副教授", "3402(机房)", "14:00-15:35", 1, 1, 68, 42),
            new Course("os", "操作系统原理", "周明华 教授", "2405(多媒体)", "08:30-10:05", 2, 0, 185, 88),
            new Course("ds", "数据结构与算法", "王晓峰 讲师", "2501(多媒体)", "10:25-12:00", 3, 0, 210, 112),
            new Course("prob", "概率论与数理统计", "刘芳 副教授", "1308(阶梯教室)", "16:00-17:35", 4, 0, 156, 64),
            new Course("db", "数据库系统概论", "赵鹏 副教授", "3209(多媒体)", "08:30-10:05", 5, 0, 192, 95)
    );

    private static final List<Quiz> QUIZZES = List.of(
            new Quiz("q-net-132", "net", "主观题", 10,
                    "P178-2：请写出 R1 向 R2 通告聚合路由 194.24.0.0/19 时的 BGP 核心路径属性，并附上子网聚合演算过程。",
                    QuizOpenState.TIMEOUT_MAKEUP, "194.24.0.0/19",
                    QuestionType.FILL_BLANK, List.of(), List.of("简答")),
            new Quiz("q-net-147", "net", "单选题", 5,
                    "自治系统之间的路由选择被称为域间路由选择，当前标准使用的外部网关协议（EGP）是？",
                    QuizOpenState.PUBLISHED, "C"),
            new Quiz("q-net-201", "net", "单选题", 5,
                    "RIP 协议中，跳数最大为 15，跳数为多少时表示网络不可达？",
                    QuizOpenState.TIMEOUT_MAKEUP, "A",
                    QuestionType.SINGLE_CHOICE, List.of("16", "15", "0", "255"), List.of()),
            new Quiz("q-net-202", "net", "多选题", 8,
                    "关于自治系统（AS）与路由协议，下列说法正确的有哪些？",
                    QuizOpenState.TIMEOUT_MAKEUP, "A,B,D",
                    QuestionType.MULTIPLE_CHOICE,
                    List.of("AS 内部使用 IGP", "BGP-4 是外部网关协议",
                            "RIP 基于链路状态算法", "OSPF 基于链路状态算法"), List.of()),
            new Quiz("q-net-203", "net", "填空题", 5,
                    "因特网早期的 RFC 文档中把路由器称为____。",
                    QuizOpenState.TIMEOUT_MAKEUP, "网关",
                    QuestionType.FILL_BLANK, List.of(), List.of("填空 1")),
            new Quiz("q-net-204", "net", "填空题", 6,
                    "域间路由选择使用____协议，域内路由选择可使用____协议（举一例）。",
                    QuizOpenState.TIMEOUT_MAKEUP, "BGP-4,RIP",
                    QuestionType.FILL_BLANK, List.of(), List.of("第 1 空", "第 2 空")),
            new Quiz("q-net-205", "net", "图片上传题", 10,
                    "请拍摄并提交你的子网聚合演算草稿（支持最多 9 张图片）。",
                    QuizOpenState.TIMEOUT_MAKEUP, "",
                    QuestionType.IMAGE_UPLOAD, List.of(), List.of()),
            new Quiz("q-py-42", "python", "单选题", 5,
                    "下面语句中可以帮助用户快速定位到异常的语句是？",
                    QuizOpenState.PUBLISHED, "A")
    );

    // ==================== 扫码登录（模拟） ====================

    @Override
    public LoginQr createLoginQr() {
        String ticket = "TK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        pendingTickets.put(ticket, new CompletableFuture<>());
        String qrDataUrl = buildQrDataUrl(ticket);
        log.info("创建登录二维码 ticket={}", ticket);
        return new LoginQr(
                ticket,
                qrDataUrl,
                "https://changjiang.ykt.io/v2/web/login?ticket=" + ticket,
                Instant.now().plusSeconds(60).toEpochMilli(), // 与真实二维码 60s 有效期一致
                POLL_TIMEOUT_SECONDS,
                true
        );
    }

    @Override
    public Optional<AuthCredential> pollAuthStatus(String ticket) {
        CompletableFuture<AuthCredential> future = pendingTickets.get(ticket);
        if (future == null) {
            return Optional.empty();
        }
        try {
            // 博客核心：单次请求挂起最多 45 秒，扫码确认瞬间立刻返回
            AuthCredential credential = future.get(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingTickets.remove(ticket);
            return Optional.of(credential);
        } catch (java.util.concurrent.TimeoutException e) {
            log.debug("登录轮询超时 ticket={}", ticket);
            pendingTickets.remove(ticket);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (java.util.concurrent.ExecutionException e) {
            return Optional.empty();
        }
    }

    /** 模拟用户在手机上点击「确认登录」：触发长轮询立即返回。 */
    public boolean confirmScan(String ticket) {
        CompletableFuture<AuthCredential> future = pendingTickets.get(ticket);
        if (future == null || future.isDone()) {
            return false;
        }
        String sessionKey = "ykt-sk-" + UUID.randomUUID().toString().replace("-", "");
        String userId = "2026" + String.valueOf(Math.abs(ticket.hashCode())).substring(0, 8);
        future.complete(new AuthCredential(userId, sessionKey));
        log.info("模拟扫码确认成功 ticket={} userId={}", ticket, userId);
        return true;
    }

    /** 生成可在浏览器展示的伪二维码 SVG（data URL），避免演示时依赖外部图床。 */
    private static String buildQrDataUrl(String ticket) {
        int size = 25;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='250' height='250' viewBox='0 0 250 250'>");
        svg.append("<rect width='250' height='250' fill='white'/>");
        int seed = ticket.hashCode();
        int cell = 10;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean inFinder = (x < 7 && y < 7) || (x >= size - 7 && y < 7) || (x < 7 && y >= size - 7);
                boolean dark;
                if (inFinder) {
                    int lx = x < 7 ? x : x - (size - 7);
                    int ly = y < 7 ? y : y - (size - 7);
                    dark = (lx == 0 || lx == 6 || ly == 0 || ly == 6) || (lx >= 2 && lx <= 4 && ly >= 2 && ly <= 4);
                } else {
                    dark = ((seed * (x + 1) * (y + 3)) & 7) < 3;
                }
                if (dark) {
                    svg.append("<rect x='").append(x * cell).append("' y='").append(y * cell)
                            .append("' width='").append(cell).append("' height='").append(cell)
                            .append("' fill='#1C1C1E'/>");
                }
            }
        }
        svg.append("</svg>");
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public CourseListResult fetchCourseList(String sessionKey) {
        log.debug("mock: fetchCourseList");
        return new CourseListResult(
                COURSES,
                COURSES.stream().filter(c -> c.status() == 1).toList(),
                "mock"
        );
    }

    @Override
    public Optional<CourseInfo> findLiveLesson(String sessionKey, String courseId) {
        return COURSES.stream()
                .filter(c -> c.courseId().equals(courseId) && c.status() == 1)
                .findFirst()
                .map(c -> new CourseInfo(
                        "lesson-" + c.courseId(), c.courseId(), c.name(), c.status(), c.currentSlide()));
    }

    @Override
    public String exchangeLessonCredential(String sessionKey, String lessonId) {
        // 模拟「用本地 session_key 换取课堂凭据」，不落库、不记录明文
        return "cred-" + Integer.toHexString((sessionKey + lessonId).hashCode());
    }

    @Override
    public Optional<Quiz> fetchQuiz(String sessionKey, String quizId) {
        return QUIZZES.stream().filter(q -> q.quizId().equals(quizId)).findFirst();
    }

    @Override
    public List<Quiz> fetchQuizzes(String sessionKey, String courseId) {
        return QUIZZES.stream()
                .filter(q -> q.courseId().equals(courseId)
                        && q.state() == QuizOpenState.TIMEOUT_MAKEUP)
                .toList();
    }

    @Override
    public boolean pushAnswer(String sessionKey, String quizId, String answer, List<String> imageUrls) {
        log.debug("mock: pushAnswer quiz={} answerLen={} images={}", quizId, answer.length(), imageUrls.size());
        return true;
    }
}
