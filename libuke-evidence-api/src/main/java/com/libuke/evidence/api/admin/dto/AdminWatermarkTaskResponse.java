package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminWatermarkTaskResponse {

    private Long id;
    private Long reportId;
    private String reportNo;
    private Long imageId;
    private Long templateId;
    private String templateName;
    private String status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
