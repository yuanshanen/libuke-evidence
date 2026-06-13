package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminInvitationCodeResponse {

    private Long id;
    private Long communityId;
    private String communityName;
    private String code;
    private Integer maxUsageCount;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
