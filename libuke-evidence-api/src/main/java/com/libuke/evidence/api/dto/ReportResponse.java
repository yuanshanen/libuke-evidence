package com.libuke.evidence.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReportResponse {

    private Long id;
    private String reportNo;
    private String communityName;
    private String witnessInfo;
    private String category;
    private String subCategory;
    private String remark;
    private String status;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String locationAddress;
    private String firstImageObjectKey;
    private String firstImageUrl;
    private Integer imageCount;
    private List<ReportAttachmentResponse> attachments;
    private LocalDateTime submittedAt;
}
