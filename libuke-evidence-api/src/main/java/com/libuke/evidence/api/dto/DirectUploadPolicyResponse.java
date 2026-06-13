package com.libuke.evidence.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DirectUploadPolicyResponse {

    private String host;
    private String objectKey;
    private String accessKeyId;
    private String policy;
    private String signature;
    private String successActionStatus;
    private Long expireAt;
}
