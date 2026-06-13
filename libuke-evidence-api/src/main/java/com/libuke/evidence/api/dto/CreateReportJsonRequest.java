package com.libuke.evidence.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateReportJsonRequest {

    @NotBlank
    private String openid;

    @NotBlank
    private String category;

    @NotBlank
    private String subCategory;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @NotNull
    private BigDecimal longitude;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @NotNull
    private BigDecimal latitude;

    @Size(max = 100)
    private String remark;

    @Size(min = 1, max = 4)
    @NotEmpty
    private List<String> imageObjectKeys;

    /**
     * 图片证据元数据
     */
    @Size(max = 4)
    private List<ReportImageEvidenceRequest> imageEvidenceList;
}
