package com.libuke.evidence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "watermark.font")
public class WatermarkProperties {

    private String name;
    private String path;
}
