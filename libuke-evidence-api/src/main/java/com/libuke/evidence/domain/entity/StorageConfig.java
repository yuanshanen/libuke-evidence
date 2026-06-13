package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("storage_configs")
public class StorageConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
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
    private Boolean systemConfig;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
