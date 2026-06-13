package com.libuke.evidence.api.open;

import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.api.open.dto.OpenRuntimeConfigResponse;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/open/v1")
public class OpenHealthController {

    private final RuntimeConfigService runtimeConfigService;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
            "service", "libuke-evidence-api",
            "status", "UP",
            "time", LocalDateTime.now()
        ));
    }

    @GetMapping("/config")
    public ApiResponse<OpenRuntimeConfigResponse> config() {
        RuntimeConfigService.MapRuntimeConfig mapConfig = runtimeConfigService.mapConfig();
        RuntimeConfigService.UploadPolicyRuntimeConfig uploadPolicy = runtimeConfigService.uploadPolicy("report_image");
        return ApiResponse.ok(OpenRuntimeConfigResponse.builder()
            .platform(runtimeConfigService.basicConfigs())
            .map(OpenRuntimeConfigResponse.MapConfig.builder()
                .jsApiKey(mapConfig.jsApiKey())
                .jsApiSecurityKey(mapConfig.jsApiSecurityKey())
                .defaultZoom(mapConfig.defaultZoom())
                .defaultLongitude(mapConfig.defaultLongitude())
                .defaultLatitude(mapConfig.defaultLatitude())
                .build())
            .reportImageUpload(OpenRuntimeConfigResponse.UploadPolicyConfig.builder()
                .maxFileCount(uploadPolicy.maxFileCount())
                .maxFileSizeMb(uploadPolicy.maxFileSizeMb())
                .allowedMimeTypes(uploadPolicy.allowedMimeTypes())
                .compressEnabled(uploadPolicy.compressEnabled())
                .build())
            .build());
    }
}
