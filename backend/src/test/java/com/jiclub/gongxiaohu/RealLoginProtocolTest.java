package com.jiclub.gongxiaohu;

import com.jiclub.gongxiaohu.gateway.YktWebSocketLogin;
import com.jiclub.gongxiaohu.model.AuthModels.LoginQr;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实协议测试：需要网络。使用 @Disabled 防止离线环境 CI 失败；
 * 手动验证时去掉 @Disabled 或运行 mvn -Dtest=RealLoginProtocolTest test。
 */
@SpringBootTest
@TestPropertySource(properties = {"gongxiaohu.mode=real"})
class RealLoginProtocolTest {

    @Autowired YktWebSocketLogin wsLogin;

    @Test
    void shouldFetchRealQrcodeFromYuketang() {
        LoginQr qr = wsLogin.createLoginQr();
        System.out.println("loginid=" + qr.ticket());
        System.out.println("qrContent=" + qr.qrContent());
        System.out.println("expireAt=" + qr.expiresAt());

        assertThat(qr.ticket()).isNotBlank();
        assertThat(qr.qrContent()).contains("mp.weixin.qq.com/cgi-bin/showqrcode");
        assertThat(qr.qrDataUrl()).startsWith("data:image/jpeg;base64,");

        byte[] image = Base64.getDecoder().decode(
                qr.qrDataUrl().substring("data:image/jpeg;base64,".length()));
        assertThat(image.length).isGreaterThan(1000);
        // JPEG 文件头 FF D8 FF
        assertThat(image[0]).isEqualTo((byte) 0xFF);
        assertThat(image[1]).isEqualTo((byte) 0xD8);
    }
}
