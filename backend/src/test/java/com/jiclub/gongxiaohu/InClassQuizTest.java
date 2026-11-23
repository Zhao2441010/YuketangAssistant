package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiclub.gongxiaohu.gateway.YktInClassQuizApi;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 随堂题提交协议单测：验证 result 字段按题型构造正确。 */
class InClassQuizTest {

    @Test
    void singleChoice_shouldBeStringArray() {
        JsonNode r = YktInClassQuizApi.buildResult(1, "B");
        assertThat(r.isArray()).isTrue();
        assertThat(r.get(0).asText()).isEqualTo("B");
    }

    @Test
    void multipleChoice_shouldBeStringArray() {
        JsonNode r = YktInClassQuizApi.buildResult(2, "A,C");
        assertThat(r.isArray()).isTrue();
        assertThat(r.size()).isEqualTo(2);
        assertThat(r.get(0).asText()).isEqualTo("A");
        assertThat(r.get(1).asText()).isEqualTo("C");
    }

    @Test
    void vote_shouldBeStringArray() {
        JsonNode r = YktInClassQuizApi.buildResult(3, "A");
        assertThat(r.isArray()).isTrue();
    }

    @Test
    void fillBlank_shouldBeNumberedMap() {
        JsonNode r = YktInClassQuizApi.buildResult(4, "边界网关协议,距离向量");
        assertThat(r.isObject()).isTrue();
        assertThat(r.get("1").asText()).isEqualTo("边界网关协议");
        assertThat(r.get("2").asText()).isEqualTo("距离向量");
    }

    @Test
    void subjective_shouldBeContentWithAttachments() {
        JsonNode r = YktInClassQuizApi.buildResult(5, "这是我的简答内容");
        assertThat(r.isObject()).isTrue();
        assertThat(r.get("content").asText()).isEqualTo("这是我的简答内容");
        assertThat(r.get("attachments").get("filelist").isArray()).isTrue();
    }
}
