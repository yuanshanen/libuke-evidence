package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUploadResponse {

    private String objectKey;
    private String url;
}
