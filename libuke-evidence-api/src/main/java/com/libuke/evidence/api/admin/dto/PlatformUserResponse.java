package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PlatformUserResponse {

    private Long id;
    private String username;
    private String displayName;
    private String phone;
    private Boolean enabled;
    private Boolean superAdmin;
    private List<Long> roleIds;
    private List<AdminRoleResponse> roles;
    private List<Long> communityIds;
    private List<AdminCommunityResponse> communities;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
