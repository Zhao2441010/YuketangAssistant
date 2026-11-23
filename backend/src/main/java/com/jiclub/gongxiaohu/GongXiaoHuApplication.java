package com.jiclub.gongxiaohu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 工小湖后端启动类。
 *
 * <p>第一版仅实现两个功能（对应开发者博客第 1、3 节）：
 * <ol>
 *     <li>免扫码秒进班：课表过滤 status==1 + session_key 换凭据 + 临时签名链接</li>
 *     <li>超时补答：倒计时后仍可提交（幂等覆盖）+ 草稿暂存 + 最多 9 张草稿图</li>
 * </ol>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GongXiaoHuApplication {
    public static void main(String[] args) {
        SpringApplication.run(GongXiaoHuApplication.class, args);
    }
}
