package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class AdminUserResponse {

    private Long id;
    private String openid;
    private String nickname;
    private String avatarObjectKey;
    private String avatarUrl;
    private Long communityId;
    private String communityName;
    private String witnessInfo;
    private String authStatus;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
