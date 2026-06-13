package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminInvitationCodeRequest {

    @NotNull
    private Long communityId;

    @NotBlank
    @Size(max = 64)
    private String code;

    private Integer maxUsageCount;
    private LocalDateTime expiresAt;
    private Boolean enabled;
}
