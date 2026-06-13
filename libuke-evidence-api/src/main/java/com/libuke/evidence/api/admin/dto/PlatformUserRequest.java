package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PlatformUserRequest {

    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @Size(max = 50)
    private String phone;

    @Size(min = 6, max = 64)
    private String password;

    private Boolean enabled;
    private Boolean superAdmin;
    private List<Long> roleIds;
    private List<Long> communityIds;
}
