package com.libuke.evidence.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DirectUploadPolicyRequest {

    @NotBlank
    private String openid;

    private String fileName;
}
