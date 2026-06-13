package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminLoginResponse {

    private String token;
    private String username;
    private String displayName;
    private Boolean superAdmin;
}
