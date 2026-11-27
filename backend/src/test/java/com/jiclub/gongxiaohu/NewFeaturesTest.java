package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 新功能接口层测试：提前获取题目 + 会话自动续期（mock 模式）。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock", "gongxiaohu.session-store="})
class NewFeaturesTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String login() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/mock-login"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    @Test
    void renew_shouldExtendSessionAndReturnRemaining() throws Exception {
        String token = login();
        // 会话信息含 remainingSeconds / renewable
        MvcResult info = mvc.perform(get("/api/v1/auth/session").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.remainingSeconds").exists())
                .andReturn();
        long remaining = mapper.readTree(info.getResponse().getContentAsString())
                .path("data").path("remainingSeconds").asLong();
        assertThat(remaining).isGreaterThan(0);

        // 主动续期成功
        mvc.perform(post("/api/v1/auth/renew").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.renewable").value(true));
    }

    @Test
    void previewQuizzes_shouldReturnQuizzesInMockMode() throws Exception {
        String token = login();
        // mock 模式：preview 回退到网关 fetchQuizzes，应返回题目列表
        mvc.perform(get("/api/v1/preview/quizzes")
                        .param("courseId", "net")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void inClassQuizzes_shouldReturnMockQuizzesAndSubmit() throws Exception {
        String token = login();
        // 拉随堂题（mock 模式返回 3 道题，含一道 TIMEOUT_MAKEUP）
        MvcResult r = mvc.perform(get("/api/v1/inclass/quizzes")
                        .param("lessonId", "lesson-1")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        int count = mapper.readTree(r.getResponse().getContentAsString()).path("data").size();
        assertThat(count).isEqualTo(3);

        // 提交随堂答案，重复提交幂等覆盖（submitCount 递增）
        String body = "{\"lessonId\":\"lesson-1\",\"problemId\":\"inclass-1\","
                + "\"problemType\":1,\"answer\":\"B\"}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/inclass/answers").content(body)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/inclass/answers").content(body)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitCount").value(2));
    }

    @Test
    void passwordLogin_shouldEstablishSessionAndAccessCourses() throws Exception {
        // 账号密码登录（mock 模式）→ 拿 token
        String body = "{\"account\":\"20260001\",\"password\":\"any\",\"type\":\"PP\"}";
        MvcResult r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/password-login").content(body)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.mock").value(true))
                .andReturn();
        String token = mapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("token").asText();
        assertThat(token).startsWith("GXH-");

        // 用该 token 访问业务接口，验证会话贯通
        mvc.perform(get("/api/v1/courses").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 空账号应 400
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/password-login")
                        .content("{\"account\":\"\",\"password\":\"x\"}")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void renew_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(post("/api/v1/auth/renew"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
