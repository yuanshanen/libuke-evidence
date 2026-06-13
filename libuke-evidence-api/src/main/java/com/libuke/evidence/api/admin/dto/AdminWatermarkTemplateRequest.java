package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminWatermarkTemplateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private Boolean enabled;

    @Size(max = 32)
    private String position;

    private BigDecimal opacity;

    private BigDecimal backgroundOpacity;

    private Integer fontSize;

    @Size(max = 20)
    private String textColor;

    @Size(max = 20)
    private String backgroundColor;

    @NotBlank
    private String contentTemplate;
}
