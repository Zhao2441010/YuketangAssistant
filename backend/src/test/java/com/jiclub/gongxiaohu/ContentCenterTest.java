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

/** 课程中心接口测试（mock 模式）：课程活动 → 课件页 + 教师题目。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock", "gongxiaohu.session-store="})
class ContentCenterTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String login() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/mock-login"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    @Test
    void activities_shouldReturnMockLessons() throws Exception {
        String token = login();
        MvcResult r = mvc.perform(get("/api/v1/content/activities")
                        .param("courseId", "net").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        int count = mapper.readTree(r.getResponse().getContentAsString()).path("data").size();
        assertThat(count).isEqualTo(3);
    }

    @Test
    void slides_shouldReturnPresentationsWithProblems() throws Exception {
        String token = login();
        MvcResult r = mvc.perform(get("/api/v1/content/slides")
                        .param("lessonId", "lesson-1").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        var data = mapper.readTree(r.getResponse().getContentAsString()).path("data");
        assertThat(data.size()).isEqualTo(1);
        // 第 3 页含教师题目
        String problem = data.get(0).path("slides").get(2).path("problem").asText();
        assertThat(problem).contains("动态路由协议");
    }

    @Test
    void lesson_shouldReturnPresentationIds() throws Exception {
        String token = login();
        mvc.perform(get("/api/v1/content/lesson")
                        .param("lessonId", "lesson-1").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.presentationIds[0]").value("pres-1"));
    }
}
