package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.gateway.YktPreviewApi;
import com.jiclub.gongxiaohu.model.Models.QuestionType;
import com.jiclub.gongxiaohu.model.Models.Quiz;
import com.jiclub.gongxiaohu.model.Models.QuizOpenState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 课件题解析单测：确保提前获取题目对多种字段名版本都能宽松解析。 */
class PreviewParseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldParseSingleChoiceProblem() throws Exception {
        String json = """
                {"problem_id":123456,"type":"单选题",
                 "body":"<p>雨课堂使用的外部网关协议是？</p>",
                 "answer":"B",
                 "score":5,
                 "options":[{"key":"A","value":"RIP"},{"key":"B","value":"BGP-4"},{"key":"C","value":"OSPF"}]}
                """;
        Quiz q = YktPreviewApi.parseProblem(mapper.readTree(json), "net", 15);
        assertThat(q.quizId()).isEqualTo("123456");
        assertThat(q.question()).isEqualTo("雨课堂使用的外部网关协议是？"); // HTML 已剥离
        assertThat(q.questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(q.options()).containsExactly("A. RIP", "B. BGP-4", "C. OSPF");
        assertThat(q.correctAnswer()).isEqualTo("B");
        assertThat(q.score()).isEqualTo(5);
        assertThat(q.state()).isEqualTo(QuizOpenState.PUBLISHED);
    }

    @Test
    void shouldParseFillBlankAndUnpublished() throws Exception {
        String json = """
                {"problemId":789,"question_type":"填空题","stem":" BGP 的全称是 ____ ，RIP 基于 ____ 算法",
                 "status":"unpublish","correctAnswer":"边界网关协议,距离向量"}
                """;
        Quiz q = YktPreviewApi.parseProblem(mapper.readTree(json), "net", 30);
        assertThat(q.questionType()).isEqualTo(QuestionType.FILL_BLANK);
        assertThat(q.state()).isEqualTo(QuizOpenState.UNPUBLISHED);
        assertThat(q.question()).contains("BGP 的全称");
    }

    @Test
    void shouldUsePageAsFallbackId() throws Exception {
        Quiz q = YktPreviewApi.parseProblem(mapper.readTree("{\"question\":\"某题\"}"), "net", 42);
        assertThat(q.quizId()).isEqualTo("page-42");
    }
}
