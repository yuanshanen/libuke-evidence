package com.libuke.evidence.domain.service;

import java.math.BigDecimal;
import java.util.Map;

public interface RuntimeConfigService {

    Map<String, String> basicConfigs();

    String configValue(String key, String fallback);

    StorageRuntimeConfig storageConfig();

    MapRuntimeConfig mapConfig();

    UploadPolicyRuntimeConfig uploadPolicy(String scene);

    void refresh();

    record StorageRuntimeConfig(
        String endpoint,
        String bucketName,
        String accessKeyId,
        String accessKeySecret,
        String uploadDir,
        String originalDir,
        String watermarkedDir,
        String avatarDir,
        Integer presignedUrlMinutes
    ) {
    }

    record MapRuntimeConfig(
        String reverseGeocodeKey,
        String jsApiKey,
        String jsApiSecurityKey,
        Integer defaultZoom,
        BigDecimal defaultLongitude,
        BigDecimal defaultLatitude
    ) {
    }

    record UploadPolicyRuntimeConfig(
        String scene,
        Integer maxFileCount,
        Integer maxFileSizeMb,
        String allowedMimeTypes,
        Boolean compressEnabled
    ) {
    }
}
