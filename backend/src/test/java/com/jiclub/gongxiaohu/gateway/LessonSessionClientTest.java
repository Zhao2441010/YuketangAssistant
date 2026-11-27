package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 免扫码进班：hello 负载字段回归测试。
 *
 * <p>依据 example1.py 与 course_helper 两个独立实现：hello 必须是 5 字段
 * {@code {op,userid,role:"student",auth,lessonid}}，auth=lessonToken、userid=登录态 user_id
 * （以 course_helper 为准；userId 缺失时退回 identityId），无 ver/avatar/presentation。字段错漏会导致服务端拒绝建立会话。
 */
class LessonSessionClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static AppProperties props() {
        return new AppProperties("real", null,
                new AppProperties.YktGateway("changjiang",
                        "https://changjiang.yuketang.cn",
                        "wss://changjiang.yuketang.cn/wsapp/",
                        10000, 35000, Map.of()),
                null, null, null);
    }

    @Test
    void helloPayload_hasExactlyFiveFieldsWithStudentRole() throws Exception {
        String payload = LessonSessionClient.helloPayload(
                mapper, "identity-123", "lesson-token-abc", "1827351428626030848");

        var node = mapper.readTree(payload);
        assertThat(node.path("op").asText()).isEqualTo("hello");
        assertThat(node.path("userid").asText()).isEqualTo("identity-123");
        assertThat(node.path("role").asText()).isEqualTo("student");
        assertThat(node.path("auth").asText()).isEqualTo("lesson-token-abc");
        assertThat(node.path("lessonid").asText()).isEqualTo("1827351428626030848");
        // 5 字段，无 ver/avatar/presentation
        assertThat(node.size()).isEqualTo(5);
        assertThat(node.has("ver")).isFalse();
        assertThat(node.has("avatar")).isFalse();
        assertThat(node.has("presentation")).isFalse();
    }

    @Test
    void enter_failsWhenLessonIdBlank() {
        LessonSessionClient client = new LessonSessionClient(null, mapper, props());
        LessonSessionClient.EnterResult r = client.enter(
                com.jiclub.gongxiaohu.model.AuthModels.SessionContext.minimal("sid"),
                new YktGateway.CourseInfo("", "c1", "课", 1, 0, "", ""));

        assertThat(r.entered()).isFalse();
        assertThat(r.code()).isEqualTo(409);
        assertThat(r.note()).contains("未开课");
    }

    @Test
    void enter_failsWhenLessonIdIsFakePrefix() {
        LessonSessionClient client = new LessonSessionClient(null, mapper, props());
        LessonSessionClient.EnterResult r = client.enter(
                com.jiclub.gongxiaohu.model.AuthModels.SessionContext.minimal("sid"),
                new YktGateway.CourseInfo("lesson-c1", "c1", "课", 1, 0, "", ""));

        assertThat(r.entered()).isFalse();
        assertThat(r.code()).isEqualTo(409);
        assertThat(r.note()).contains("未开课");
    }
}
