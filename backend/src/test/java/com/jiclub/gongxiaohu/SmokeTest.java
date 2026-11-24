package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import com.jiclub.gongxiaohu.model.Models.SubmitAnswerRequest;
import com.jiclub.gongxiaohu.service.JoinLessonService;
import com.jiclub.gongxiaohu.service.MakeupAnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 冒烟测试：覆盖两大专属功能的完整链路。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"gongxiaohu.mode=mock"})
class SmokeTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JoinLessonService joinLessonService;
    @Autowired MakeupAnswerService makeupAnswerService;

    @Test
    void courseList_shouldContainLiveCourses() throws Exception {
        mvc.perform(get("/api/v1/courses").header("X-Session-Key", "test-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveCourses.length()").value(2))
                .andExpect(jsonPath("$.data.source").value("mock"));
    }

    @Test
    void joinLesson_shouldBuildOfficialUrlAndRejectNotLiveCourse() throws Exception {
        mvc.perform(post("/api/v1/lessons/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"net","sessionKey":"test-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joinUrl").value(org.hamcrest.Matchers.containsString("/v2/web/join_lesson?lesson_id=")))
                .andExpect(jsonPath("$.data.lessonId").value("lesson-net"));

        mvc.perform(post("/api/v1/lessons/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"os","sessionKey":"test-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409)); // 未开课拒进
    }

    @Test
    void makeupAnswer_shouldAcceptAfterTimeoutAndBeIdempotent() throws Exception {
        QuizOpenState state = makeupAnswerService
                .getQuiz("test-session", "q-net-132").state();
        assertThat(state).isEqualTo(QuizOpenState.TIMEOUT_MAKEUP);

        String body = mapper.writeValueAsString(new SubmitAnswerRequest(
                "test-session", "聚合路由为 194.24.0.0/19", List.of(), false));

        for (int i = 1; i <= 2; i++) {
            final int expected = i;
            mvc.perform(post("/api/v1/quizzes/q-net-132/answers")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accepted").value(true))
                    .andExpect(jsonPath("$.data.makeup").value(true))
                    .andExpect(jsonPath("$.data.submitCount").value(expected));
        }
    }

    @Test
    void closedQuiz_shouldRejectSubmit() throws Exception {
        // q-net-147 在 mock 中为 PUBLISHED，通过直接调用服务验证 CLOSED 分支需抓包后调整；
        // 此处验证未发布题目不可提交。
        String body = mapper.writeValueAsString(new SubmitAnswerRequest(
                "test-session", "答案", List.of(), false));
        mvc.perform(post("/api/v1/quizzes/q-py-99/answers")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void signVerification_shouldRejectTamperedTs() {
        assertThat(joinLessonService.verifyJoinUrl("lesson-net", 123456789L, "bad")).isFalse();
    }
}
