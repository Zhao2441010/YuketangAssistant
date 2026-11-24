package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 日志能力验收：确认
 * <ol>
 *   <li>audit.log 记录<b>明文学号</b>（回答"哪个学号登录了"）；</li>
 *   <li>app.log 有访问行（IP / 方法 / 路径 / 状态 / 耗时 / 脱敏学号）；</li>
 *   <li>两份日志都<b>不含</b> token 原文。</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock"})
class LoggingAuditTest {

    private static final Path AUDIT = Path.of("logs", "audit.log");
    private static final Path APP = Path.of("logs", "app.log");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void auditLog_shouldRecordPlainStudentId_loginAndAccess() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/mock-login"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = mapper.readTree(login).path("data").path("token").asText();
        String userId = mapper.readTree(login).path("data").path("userId").asText();
        assertThat(token).startsWith("GXH-");
        assertThat(userId).isNotBlank();

        // 带 token 调一次业务接口，触发访问日志
        mvc.perform(get("/api/v1/courses").header("X-Session-Token", token));

        List<String> audit = Files.readAllLines(AUDIT, StandardCharsets.UTF_8);
        List<String> app = Files.readAllLines(APP, StandardCharsets.UTF_8);

        assertThat(audit).anyMatch(line ->
                line.contains("LOGIN_OK") && line.contains("userId=" + userId));
        assertThat(app).anyMatch(line ->
                line.contains("ACCESS") && line.contains("GET /api/v1/courses")
                        && line.contains("status=200"));

        // 凭证绝不落盘
        assertThat(String.join("\n", audit)).doesNotContain(token);
        assertThat(String.join("\n", app)).doesNotContain(token);
    }

    @Test
    void auditLog_shouldRecordImportCookieWithPlainId() throws Exception {
        String cookie = "sessionid=test-session-abc; csrftoken=csrf-xyz; "
                + "university_id=1234; uv_id=uv-5678";
        mvc.perform(post("/api/v1/auth/import-cookie")
                        .contentType("application/json")
                        .content("{\"cookie\":\"" + cookie + "\",\"userId\":\"2023123456\"}"))
                .andReturn();

        List<String> audit = Files.readAllLines(AUDIT, StandardCharsets.UTF_8);
        assertThat(audit).anyMatch(line ->
                line.contains("LOGIN_OK") && line.contains("method=import-cookie")
                        && line.contains("userId=2023123456"));
        // Cookie 内容不得进日志
        assertThat(String.join("\n", audit)).doesNotContain("test-session-abc");
    }
}
