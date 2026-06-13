package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminSystemConfigResponse {

    private Long id;
    private String configGroup;
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private Boolean encrypted;
    private Boolean sensitive;
    private Boolean editable;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
