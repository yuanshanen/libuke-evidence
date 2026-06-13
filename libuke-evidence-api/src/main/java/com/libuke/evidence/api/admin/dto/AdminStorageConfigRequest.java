package com.libuke.evidence.api.admin.dto;

import lombok.Data;

@Data
public class AdminStorageConfigRequest {
    private String name;
    private String provider;
    private String endpoint;
    private String region;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
    private String uploadDir;
    private String originalDir;
    private String watermarkedDir;
    private String avatarDir;
    private Integer presignedUrlMinutes;
    private Boolean enabled;
    private String remark;
}
