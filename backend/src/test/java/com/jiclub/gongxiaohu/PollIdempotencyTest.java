package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 扫码轮询的两条硬性约束（2026-11-25「后端 LOGIN_OK、前端停在二维码页」修复）：
 *
 * <ol>
 *     <li>短轮询（wait=0）未确认时必须秒回，不能挂 45 秒——移动端长空闲连接会被掐断</li>
 *     <li>同一 ticket 重复轮询必须幂等返回同一 token，让丢失的成功响应能被下次轮询取回</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock"})
class PollIdempotencyTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void shortPoll_shouldReturnImmediately_andSameTicketShouldBeIdempotent() throws Exception {
        String qrBody = mvc.perform(get("/api/v1/auth/qr"))
                .andReturn().getResponse().getContentAsString();
        String ticket = mapper.readTree(qrBody).path("data").path("ticket").asText();
        assertThat(ticket).isNotBlank();

        // 1) 未扫码确认：wait=0 必须立刻返回 409（不能阻塞）
        long started = System.currentTimeMillis();
        mvc.perform(post("/api/v1/auth/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pollBody(ticket, "0")))
                .andExpect(jsonPath("$.code").value(409));
        long cost = System.currentTimeMillis() - started;
        assertThat(cost).as("短轮询应立即返回，不能挂起等待").isLessThan(3000);

        // 2) 手机点确认
        mvc.perform(post("/api/v1/auth/mock-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"" + ticket + "\"}"))
                .andExpect(jsonPath("$.data").value(true));

        // 3) 第一次轮询拿到 token
        String first = mvc.perform(post("/api/v1/auth/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pollBody(ticket, "0")))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String firstToken = mapper.readTree(first).path("data").path("token").asText();
        String userId = mapper.readTree(first).path("data").path("userId").asText();
        assertThat(firstToken).isNotBlank();
        assertThat(userId).isNotBlank();

        // 4) 模拟"第一次响应前端没收到"：同 ticket 再轮询，必须拿回同一个 token
        String second = mvc.perform(post("/api/v1/auth/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pollBody(ticket, "0")))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(second).path("data").path("token").asText())
                .as("同 ticket 重复轮询应返回同一 token，前端才能补偿取回登录态")
                .isEqualTo(firstToken);

        // 5) 该 token 可直接用于业务接口
        mvc.perform(get("/api/v1/courses").header("X-Session-Token", firstToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    private static String pollBody(String ticket, String wait) {
        return "{\"ticket\":\"" + ticket + "\",\"wait\":\"" + wait + "\"}";
    }
}
