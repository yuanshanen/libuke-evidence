package com.libuke.evidence.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportAttachmentResponse {

    private Long id;
    private String objectKey;
    private String url;
    private String type;
    private Integer sortOrder;
    private Long originalFileSize;
    private String originalMimeType;
    private String originalFileName;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime clientUploadedAt;
    private LocalDateTime serverReceivedAt;
}
