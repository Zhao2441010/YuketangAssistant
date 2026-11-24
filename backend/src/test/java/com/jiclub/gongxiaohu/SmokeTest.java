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
        // 方案④：返回官方在读学生固定入口（无 sign/ts）+ entryId，凭证只留后端内存
        mvc.perform(post("/api/v1/lessons/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"net","sessionKey":"test-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joinUrl").value(
                        org.hamcrest.Matchers.containsString("/lesson/fullscreen/v3/")))
                .andExpect(jsonPath("$.data.joinUrl").value(
                        org.hamcrest.Matchers.containsString("source=5")))
                .andExpect(jsonPath("$.data.joinUrl").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("join_lesson"))))
                .andExpect(jsonPath("$.data.entryId").isNotEmpty())
                // 关键：登录态不得回传前端
                .andExpect(jsonPath("$.data.credential").doesNotExist())
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

    @Test
    void joinLesson_shouldNotCheckinByDefault() throws Exception {
        // §7 核心：默认进课堂不签到，credentialReady=false 且 hint 说明固定入口就绪
        mvc.perform(post("/api/v1/lessons/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"net","sessionKey":"test-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credentialReady").value(false))
                .andExpect(jsonPath("$.data.hint").value(
                        org.hamcrest.Matchers.containsString("无需扫码")))
                .andExpect(jsonPath("$.data.joinUrl").value(
                        org.hamcrest.Matchers.containsString("/lesson/fullscreen/v3/")));

        // enhance=true 才走自渲染增强（mock 模式下无凭证客户端，仍不阻断）
        mvc.perform(post("/api/v1/lessons/join?enhance=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"net","sessionKey":"test-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entryId").isNotEmpty());
    }

    @Test
    void entryCookie_shouldRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/lessons/entry-cookie"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void importCookie_shouldPersistFullL1Context() throws Exception {
        // §6.2 ①：整串 Cookie 导入，补齐 csrftoken / X-UID / uv-id
        mvc.perform(post("/api/v1/auth/import-cookie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cookie":"csrftoken=csrf-abc; sessionid=sid-xyz; university_id=1234; uv_id=5678",
                                 "userId":"20220001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                // 完整 Cookie 绝不回传前端，只返回 sessionId（前端续期用）
                .andExpect(jsonPath("$.data.csrftoken").doesNotExist());

        // 缺少 sessionid 应被拒
        mvc.perform(post("/api/v1/auth/import-cookie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cookie":"csrftoken=csrf-abc"}
                                """))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void selfRender_shouldReturnSlidesAndQuizzesWithoutLeakingCredential() throws Exception {
        // 方案④：先 join 拿 entryId，再凭它自渲染，全程不下发任何凭证
        String joinBody = mvc.perform(post("/api/v1/lessons/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"net","sessionKey":"test-session"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String entryId = mapper.readTree(joinBody).path("data").path("entryId").asText();
        assertThat(entryId).isNotBlank();

        mvc.perform(get("/api/v1/lessons/" + entryId + "/render"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lessonId").value("lesson-net"))
                .andExpect(jsonPath("$.data.presentations").isArray())
                .andExpect(jsonPath("$.data.quizzes").isArray())
                .andExpect(jsonPath("$.data.pptJwt").doesNotExist())
                .andExpect(jsonPath("$.data.credential").doesNotExist());

        // 无效 entryId 应 401
        mvc.perform(get("/api/v1/lessons/not-exist/render"))
                .andExpect(jsonPath("$.code").value(401));
    }
}
