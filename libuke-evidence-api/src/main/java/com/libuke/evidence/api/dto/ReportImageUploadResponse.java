package com.libuke.evidence.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportImageUploadResponse {

    private String objectKey;
    private String url;
}
