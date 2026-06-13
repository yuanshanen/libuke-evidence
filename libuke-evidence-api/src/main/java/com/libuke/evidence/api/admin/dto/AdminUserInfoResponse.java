package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminUserInfoResponse {

    private Long userId;
    private String username;
    private String realName;
    private String displayName;
    private String phone;
    private String avatar;
    private String desc;
    private String homePath;
    private Boolean superAdmin;
    private List<String> roles;
    private List<String> permissions;
    private List<AdminCommunityResponse> communities;
}
