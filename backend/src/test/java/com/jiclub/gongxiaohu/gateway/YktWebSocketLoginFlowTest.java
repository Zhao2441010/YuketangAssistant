package com.jiclub.gongxiaohu.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：扫码登录的"一次性凭据"不能被状态通知提前消费/覆盖。
 *
 * <p>2026-11-24 的真实故障：{@code subscribe_status}（不含 Auth）先 complete 了 future，
 * 真正的 {@code loginsuccess} 再 complete 无效 → 用户扫码后永远登不上。
 * 2026-11-25 追加：换 sessionid 失败后凭据必须还在，供下次 poll 重试。
 */
class YktWebSocketLoginFlowTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void subscribeStatusWithoutAuth_mustNotConsumeLogin() throws Exception {
        YktWebSocketLogin.LoginFlow flow = new YktWebSocketLogin.LoginFlow();
        var status = mapper.readTree("{\"op\":\"subscribe_status\",\"subscribe_status\":true}");

        assertThat(flow.cacheCredential(status)).isFalse();
        assertThat(flow.credential).isNull();
        assertThat(flow.loginFuture).isNotDone();
    }

    @Test
    void loginSuccessWithAuth_isCachedAndCannotBeOverwritten() throws Exception {
        YktWebSocketLogin.LoginFlow flow = new YktWebSocketLogin.LoginFlow();
        var success = mapper.readTree(
                "{\"op\":\"loginsuccess\",\"UserID\":55120220,\"Auth\":\"real-auth-value\",\"subscribe_status\":true}");

        assertThat(flow.cacheCredential(success)).isTrue();
        assertThat(flow.credential).isSameAs(success);
        assertThat(flow.loginFuture).isDone();
        assertThat(flow.loginFuture.get().path("Auth").asText()).isEqualTo("real-auth-value");

        // 迟到的状态通知不能覆盖已缓存的扫码凭据
        var late = mapper.readTree("{\"op\":\"loginsuccess\",\"subscribe_status\":true}");
        assertThat(flow.cacheCredential(late)).isFalse();
        assertThat(flow.credential).isSameAs(success);

        // 即使再来一条带 Auth 的重复消息，也保留首次凭据
        var dup = mapper.readTree("{\"op\":\"loginsuccess\",\"Auth\":\"another-auth\"}");
        assertThat(flow.cacheCredential(dup)).isTrue();
        assertThat(flow.credential).isSameAs(success);
    }
}
