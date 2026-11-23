package com.jiclub.gongxiaohu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiclub.gongxiaohu.config.AppProperties;
import com.jiclub.gongxiaohu.gateway.YktPasswordLoginApi;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 账号密码登录单测：RSA 加密格式 + mock 登录端到端。 */
class PasswordLoginTest {

    private static final String KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCQBaPX7crEH6/jS4hRD7lZrsFRIdfwEhH30onFnrnWxiRATzP9WEneXJEZHopmzudkNS5bDp51SCnBUGGgfL/sUUrlrhV2xnTSe1jRl924ejV5rkVkiii85jp9G8eJrJN6klHs0PfYfp4EVJ8688qpi5iETtg+q4ITocyEyD1+7wIDAQAB";

    @Test
    void rsaEncryption_shouldProduceBase64Of128Bytes() throws Exception {
        AppProperties props = new AppProperties("real", null,
                new AppProperties.YktGateway("changjiang", "https://changjiang.yuketang.cn",
                        "wss://changjiang.yuketang.cn/wsapp/", 10000, 35000, Map.of()),
                new AppProperties.PasswordLogin(KEY, "2091064951", "/pc/login/verify_pwd_login/"),
                "./data");
        YktPasswordLoginApi api = new YktPasswordLoginApi(props, new ObjectMapper());
        String enc = api.encryptPassword("MyPass123");
        byte[] raw = Base64.getDecoder().decode(enc);
        assertThat(raw).hasSize(128); // 1024-bit RSA
        assertThat(Base64.getEncoder().encodeToString(raw)).isEqualTo(enc);
    }

    @Test
    void passwordRecordDefaults() {
        AppProperties.PasswordLogin cfg =
                new AppProperties.PasswordLogin(KEY, null, null);
        assertThat(cfg.endpointOrDefault()).isEqualTo("/pc/login/verify_pwd_login/");
        assertThat(cfg.captchaAppIdOrDefault()).isEqualTo("2091064951");
    }
}
