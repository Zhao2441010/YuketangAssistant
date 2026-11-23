package com.jiclub.gongxiaohu.controller;

import com.jiclub.gongxiaohu.common.ApiResponse;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.model.Models.Draft;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.SubmitAnswerRequest;
import com.jiclub.gongxiaohu.model.Models.SubmitResult;
import com.jiclub.gongxiaohu.model.Models.UploadResult;
import com.jiclub.gongxiaohu.service.MakeupAnswerService;
import com.jiclub.gongxiaohu.service.SessionResolver;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/** 超时补答接口。 */
@RestController
@RequestMapping("/api/v1")
public class AnswerController {
    private final MakeupAnswerService service;
    private final AppProperties properties;
    private final SessionResolver sessionResolver;

    public AnswerController(MakeupAnswerService service, AppProperties properties, SessionResolver sessionResolver) {
        this.service = service;
        this.properties = properties;
        this.sessionResolver = sessionResolver;
    }

    /** 按课程拉取题目列表（超时补答页使用）。 */
    @GetMapping("/quizzes")
    public ApiResponse<java.util.List<Quiz>> quizzes(
            @RequestParam String courseId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        return ApiResponse.ok(service.listQuizzes(sessionResolver.resolve(token, legacyKey), courseId));
    }

    @GetMapping("/quizzes/{quizId}")
    public ApiResponse<Quiz> quiz(
            @PathVariable String quizId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        return ApiResponse.ok(service.getQuiz(sessionResolver.resolve(token, legacyKey), quizId));
    }

    /** 提交 / 补交答案。 */
    @PostMapping("/quizzes/{quizId}/answers")
    public ApiResponse<SubmitResult> submit(
            @PathVariable String quizId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        SubmitAnswerRequest resolved = withSessionKey(request, token);
        return ApiResponse.ok(service.submit(quizId, resolved));
    }

    /** 暂存草稿。 */
    @PutMapping("/quizzes/{quizId}/draft")
    public ApiResponse<Draft> saveDraft(
            @PathVariable String quizId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ApiResponse.ok(service.saveDraft(quizId, withSessionKey(request, token)));
    }

    @GetMapping("/quizzes/{quizId}/draft")
    public ApiResponse<Draft> getDraft(
            @PathVariable String quizId,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Session-Key", required = false) String legacyKey) {
        return ApiResponse.ok(service.getDraft(quizId, sessionResolver.resolve(token, legacyKey)));
    }

    /** 若有本地 token，则用 token 解析出的 session_key 覆盖 body 里的值。 */
    private SubmitAnswerRequest withSessionKey(SubmitAnswerRequest request, String token) {
        if (token == null || token.isBlank()) {
            return request;
        }
        String key = sessionResolver.resolve(token, null);
        return new SubmitAnswerRequest(key, request.answer(), request.imageUrls(), request.draftOnly());
    }

    /** 上传草稿图（前端限定最多 9 张，服务端再次校验）。 */
    @PostMapping(value = "/uploads/draft-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "existing", defaultValue = "0") int existing) {
        return ApiResponse.ok(service.uploadDraftImage(file, existing));
    }

    /** 读取本地图片。 */
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) throws Exception {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Path.of(properties.uploadDir()).toAbsolutePath().normalize().resolve(filename);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(resource);
    }
}
