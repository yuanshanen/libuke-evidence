package com.libuke.evidence.api.admin.dto;

import com.libuke.evidence.api.dto.ReportAttachmentResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminReportResponse {

    private Long id;
    private String reportNo;
    private Long userId;
    private String openid;
    private String witnessInfo;
    private Long communityId;
    private String communityName;
    private String category;
    private String subCategory;
    private String remark;
    private String status;
    private String adminNote;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String locationAddress;
    private String firstImageUrl;
    private Integer imageCount;
    private List<ReportAttachmentResponse> attachments;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
