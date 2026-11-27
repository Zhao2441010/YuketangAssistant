package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.gateway.LessonDiagnostic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证诊断（§6.7）：结构化返回验证证据，且严格脱敏、需登录。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock", "gongxiaohu.session-store="})
class DiagnosticsTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired LessonDiagnostic diagnostic;

    @Test
    void report_shouldRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/diagnostics"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void report_shouldReturnChecklistAndNeverLeakSecrets() throws Exception {
        // 先登录拿 token
        String login = mvc.perform(post("/api/v1/auth/mock-login"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // 造一条包含「敏感值」的诊断记录，验证对外返回时不出现明文
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("Cookie", "csrftoken=SECRET-CSRF; sessionid=SUPER-SECRET-SID");
        raw.put("X-CSRFToken", "SECRET-CSRF");
        raw.put("uv-id", "");
        List<String> missing = new ArrayList<>();
        Map<String, String> safe = LessonDiagnostic.headers(raw, missing);
        diagnostic.record(new LessonDiagnostic.Record(
                "12:00:00.000", "lesson-test", 21, "https://example/checkin",
                safe, List.copyOf(missing), 200, 0, "ok",
                true, LessonDiagnostic.fingerprint("eyJhbGciOiJIUzI1NiJ9.SECRET-JWT"),
                true, true, "SUCCESS", 123));

        String body = mvc.perform(get("/api/v1/diagnostics").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAttempts").value(1))
                .andExpect(jsonPath("$.data.checklist.checkinHeadersCaptured").value(true))
                .andExpect(jsonPath("$.data.checklist.setAuthCaptured").value(true))
                .andExpect(jsonPath("$.data.checklist.tripleCaptured").value(true))
                .andExpect(jsonPath("$.data.checklist.getTokenKnown").value(true))
                .andExpect(jsonPath("$.data.records[0].missingHeaders[0]").value("uv-id"))
                .andReturn().getResponse().getContentAsString();

        // 关键：任何敏感明文都不得出现在响应里
        assertThat(body).doesNotContain("SUPER-SECRET-SID", "SECRET-CSRF", "SECRET-JWT");
        assertThat(body).contains("len=");   // 指纹存在，证明「拿到了」但不暴露内容
    }

    @Test
    void clear_shouldEmptyRecords() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/mock-login"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("data").path("token").asText();

        mvc.perform(delete("/api/v1/diagnostics").header("X-Session-Token", token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/diagnostics").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAttempts").value(0));
    }

    @Test
    void fingerprint_shouldBeIrreversible() {
        assertThat(LessonDiagnostic.fingerprint("abcdefghijk"))
                .isEqualTo("len=11,prefix=abcd***");
        assertThat(LessonDiagnostic.fingerprint("")).isEqualTo("无");
        assertThat(LessonDiagnostic.fingerprint(null)).isEqualTo("无");
    }
}
