package com.libuke.evidence.api.open.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class OpenRuntimeConfigResponse {

    private Map<String, String> platform;
    private MapConfig map;
    private UploadPolicyConfig reportImageUpload;

    @Data
    @Builder
    public static class MapConfig {
        private String jsApiKey;
        private String jsApiSecurityKey;
        private Integer defaultZoom;
        private BigDecimal defaultLongitude;
        private BigDecimal defaultLatitude;
    }

    @Data
    @Builder
    public static class UploadPolicyConfig {
        private Integer maxFileCount;
        private Integer maxFileSizeMb;
        private String allowedMimeTypes;
        private Boolean compressEnabled;
    }
}
