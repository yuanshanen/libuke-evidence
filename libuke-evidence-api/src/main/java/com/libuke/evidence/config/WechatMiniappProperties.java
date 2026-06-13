package com.libuke.evidence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatMiniappProperties {

    private String appId;
    private String appSecret;
    private String code2sessionUrl;
    private Integer connectTimeoutMillis;
    private Integer readTimeoutMillis;
}
