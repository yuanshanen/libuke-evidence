package com.libuke.evidence.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindCommunityRequest {

    @NotBlank
    private String openid;

    @NotBlank
    private String invitationCode;

    private String witnessInfo;
}
