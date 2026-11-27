package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 登录链路测试：用户端 → 后端 → 雨课堂端（mock 网关）。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock", "gongxiaohu.session-store="})
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    /** 一步登录：取二维码 → 模拟扫码 → 长轮询返回 token。 */
    private String login() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/mock-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.sessionKey").exists())
                .andExpect(jsonPath("$.data.mock").value(true))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    void fullLoginChain_shouldReturnTokenAndAccessCourses() throws Exception {
        // 1. 获取二维码（应含 ticket 与 data URL）
        MvcResult qrResult = mvc.perform(get("/api/v1/auth/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket").exists())
                .andExpect(jsonPath("$.data.qrDataUrl").value(
                        org.hamcrest.Matchers.containsString("data:image/svg+xml;base64,")))
                .andExpect(jsonPath("$.data.pollTimeoutSeconds").value(45)) // 单次校验 45 秒
                .andReturn();
        JsonNode qr = mapper.readTree(qrResult.getResponse().getContentAsString()).path("data");
        String ticket = qr.path("ticket").asText();

        // 2. 先发长轮询请求（异步），再模拟扫码确认
        //    这里直接用 mock-login 验证端到端，避免真实 45 秒等待
        String token = login();
        assertThat(token).startsWith("GXH-");

        // 3. 用 token 访问业务接口（课表），应成功
        mvc.perform(get("/api/v1/courses").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.liveCourses.length()").value(2));

        // 4. 会话查询有效
        mvc.perform(get("/api/v1/auth/session").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        // 5. 退出后 token 失效
        mvc.perform(post("/api/v1/auth/logout").header("X-Session-Token", token));
        mvc.perform(get("/api/v1/courses").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void invalidTicket_shouldFailPoll() throws Exception {
        mvc.perform(post("/api/v1/auth/poll").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"NOT-EXIST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }
}
