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

/** 全量课件抓取：必须包含尚未播放的课时，且 ZIP 可直接下载。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock", "gongxiaohu.session-store="})
class FullCoursewareTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void courseAll_shouldIncludeUnplayedLessons() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/mock-login"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("data").path("token").asText();

        String body = mvc.perform(get("/api/v1/content/course-all")
                        .param("courseId", "C-1")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.lessonCount").value(2))
                .andExpect(jsonPath("$.data.slideCount").value(6))
                .andReturn().getResponse().getContentAsString();

        // 尚未播放的课时必须照样出现在结果里
        assertThat(body).contains("尚未播放");
        assertThat(body).contains("finished\":false");
    }

    @Test
    void courseAll_shouldRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/content/course-all").param("courseId", "C-1"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void courseAllZip_shouldReturnZipWithManifest() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/mock-login"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("data").path("token").asText();

        MvcResult result = mvc.perform(get("/api/v1/content/course-all.zip")
                        .param("courseId", "C-1")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        byte[] zip = result.getResponse().getContentAsByteArray();
        assertThat(zip.length).isGreaterThan(100);
        assertThat(zip[0]).isEqualTo((byte) 'P');
        assertThat(zip[1]).isEqualTo((byte) 'K');
        assertThat(new String(zip, java.nio.charset.StandardCharsets.ISO_8859_1))
                .contains("manifest.csv");
    }
}
