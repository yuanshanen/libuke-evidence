package com.libuke.evidence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "amap.map")
public class AmapMapProperties {
    private String key;
    private String reverseGeocodeKey;
    private String jsApiKey;
    private String jsApiSecurityKey;
    private String reverseGeocoderUrl;
    private Integer connectTimeoutMillis;
    private Integer readTimeoutMillis;
}
