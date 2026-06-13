package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminWatermarkTemplateResponse {

    private Long id;
    private String name;
    private Boolean enabled;
    private String position;
    private BigDecimal opacity;
    private BigDecimal backgroundOpacity;
    private Integer fontSize;
    private String textColor;
    private String backgroundColor;
    private String contentTemplate;
    private Boolean systemTemplate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
