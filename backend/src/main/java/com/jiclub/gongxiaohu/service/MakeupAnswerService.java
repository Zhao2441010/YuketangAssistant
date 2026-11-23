package com.jiclub.gongxiaohu.service;

import com.jiclub.gongxiaohu.common.BizException;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.YktGateway;
import com.jiclub.gongxiaohu.model.Models.AnswerRecord;
import com.jiclub.gongxiaohu.model.Models.Draft;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import java.util.List;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import com.jiclub.gongxiaohu.model.Models.SubmitAnswerRequest;
import com.jiclub.gongxiaohu.model.Models.SubmitResult;
import com.jiclub.gongxiaohu.model.Models.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超时补答（博客第 3 节）。
 *
 * <p>核心发现：服务端并未在倒计时归零时掐死接口；只要老师未点「结束作答」，
 * 提交 API 始终可接收，且多次提交幂等覆盖。因此：
 * <ul>
 *     <li>只有 QuizOpenState.CLOSED 才拒收；TIMEOUT_MAKEUP 照常接收并标记 makeup=true</li>
 *     <li>同一 (quizId, sessionKeyHash) 重复提交覆盖旧答案，submitCount 自增</li>
 *     <li>草稿独立暂存，与正式提交解耦</li>
 *     <li>草稿图最多 9 张，本地存储，不落隐私数据库</li>
 * </ul>
 */
@Service
public class MakeupAnswerService {
    private static final Logger log = LoggerFactory.getLogger(MakeupAnswerService.class);
    private static final int MAX_IMAGES = 9;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final YktGateway gateway;
    private final AppProperties properties;
    private final Map<String, AnswerRecord> answers = new ConcurrentHashMap<>();
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();

    public MakeupAnswerService(YktGateway gateway, AppProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    /** 查询题目与作答状态。 */
    public Quiz getQuiz(String sessionKey, String quizId) {
        return gateway.fetchQuiz(sessionKey, quizId)
                .orElseThrow(() -> BizException.notFound("题目不存在：" + quizId));
    }

    /** 按课程拉取题目列表。 */
    public List<Quiz> listQuizzes(String sessionKey, String courseId) {
        return gateway.fetchQuizzes(sessionKey, courseId);
    }

    /**
     * 提交答案：超时仍可补交，老师结束作答后拒收；重复提交覆盖。
     */
    public SubmitResult submit(String quizId, SubmitAnswerRequest request) {
        Quiz quiz = getQuiz(request.sessionKey(), quizId);
        if (!quiz.acceptsAnswer()) {
            throw BizException.conflict(
                    quiz.state() == QuizOpenState.UNPUBLISHED
                            ? "题目尚未发布，无法作答"
                            : "老师已结束作答，服务端不再接收答案");
        }

        // 幂等键：只保存 sessionKey 的哈希，绝不落明文
        String key = quizId + ":" + Integer.toHexString(request.sessionKey().hashCode());
        AnswerRecord previous = answers.get(key);
        int nextCount = previous == null ? 1 : previous.submitCount() + 1;
        long now = Instant.now().toEpochMilli();

        boolean pushed = gateway.pushAnswer(request.sessionKey(), quizId,
                request.safeAnswer(), request.safeImages());
        if (!pushed) {
            throw BizException.conflict("上游提交失败，请重试（本地草稿已保留）");
        }

        answers.put(key, new AnswerRecord(
                quizId,
                mask(request.sessionKey()),
                request.safeAnswer(),
                request.safeImages(),
                now,
                nextCount
        ));
        // 正式提交后清掉草稿（客户端本地草稿也应同步清理）
        drafts.remove(key);

        boolean makeup = quiz.state() == QuizOpenState.TIMEOUT_MAKEUP
                || quiz.state() == QuizOpenState.SUBMITTED;
        log.info("答案提交成功 quiz={} times={} makeup={}", quizId, nextCount, makeup);
        return new SubmitResult(quizId, true,
                makeup ? "补交成功（已超时，老师尚未收题）" : "提交成功",
                nextCount, now, makeup);
    }

    /** 仅存草稿，不向上游提交。 */
    public Draft saveDraft(String quizId, SubmitAnswerRequest request) {
        String key = quizId + ":" + Integer.toHexString(request.sessionKey().hashCode());
        Draft draft = new Draft(quizId, request.safeAnswer(), Instant.now().toEpochMilli());
        drafts.put(key, draft);
        return draft;
    }

    public Draft getDraft(String quizId, String sessionKey) {
        String key = quizId + ":" + Integer.toHexString(sessionKey.hashCode());
        Draft draft = drafts.get(key);
        if (draft == null) {
            throw BizException.notFound("暂无本地草稿（服务端不留存，请以客户端本地存储为准）");
        }
        return draft;
    }

    /** 草稿图上传：最多 9 张，本地磁盘存储，文件名随机化。 */
    public UploadResult uploadDraftImage(MultipartFile file, int existingCount) {
        if (existingCount >= MAX_IMAGES) {
            throw BizException.badRequest("最多支持上传 9 张草稿图");
        }
        if (file.isEmpty()) {
            throw BizException.badRequest("上传文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.')).toLowerCase()
                : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw BizException.badRequest("仅支持 jpg/png/webp/gif 图片格式");
        }

        String filename = UUID.randomUUID() + ext;
        try {
            Path dir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new UploadResult("/api/v1/uploads/" + filename, filename, file.getSize());
        } catch (IOException e) {
            throw new BizException(500, "图片保存失败：" + e.getMessage());
        }
    }

    private static String mask(String sessionKey) {
        if (sessionKey.length() <= 6) return "****";
        return sessionKey.substring(0, 3) + "***" + sessionKey.substring(sessionKey.length() - 3);
    }
}
