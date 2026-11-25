package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 免扫码进班：验证 WSS hello 负载的字段与凭据携带。
 *
 * <p>对应「正确实现链路.md」的官方进班 hello 消息：
 * {@code {op:"hello", userid, avatar, role:"student", auth: userAuth, lessonid, presentation, ver:1}}。
 * 只断言结构，不触发任何网络。
 */
class LessonSessionClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void helloPayload_carriesStudentRoleAndCredential() throws Exception {
        String payload = LessonSessionClient.helloPayload(
                mapper, "55120220", "https://avatar", "auth-credential", "L12345", "P9", 1);

        JsonNode node = mapper.readTree(payload);
        assertThat(node.path("op").asText()).isEqualTo("hello");
        assertThat(node.path("role").asText()).isEqualTo("student");
        assertThat(node.path("userid").asText()).isEqualTo("55120220");
        assertThat(node.path("auth").asText()).isEqualTo("auth-credential");
        assertThat(node.path("lessonid").asText()).isEqualTo("L12345");
        assertThat(node.path("presentation").asText()).isEqualTo("P9");
        assertThat(node.path("ver").asInt()).isEqualTo(1);
    }

    @Test
    void helloPayload_nullFieldsBecomeEmptyString() throws Exception {
        String payload = LessonSessionClient.helloPayload(mapper, null, null, null, null, null, 1);

        JsonNode node = mapper.readTree(payload);
        assertThat(node.path("userid").asText()).isEmpty();
        assertThat(node.path("avatar").asText()).isEmpty();
        assertThat(node.path("auth").asText()).isEmpty();
        assertThat(node.path("lessonid").asText()).isEmpty();
        assertThat(node.path("presentation").asText()).isEmpty();
        assertThat(node.path("role").asText()).isEqualTo("student");
    }
}
