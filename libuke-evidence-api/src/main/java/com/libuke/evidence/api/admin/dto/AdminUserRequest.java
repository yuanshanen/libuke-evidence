package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserRequest {

    @Size(max = 100)
    private String nickname;

    @Size(max = 500)
    private String avatarObjectKey;

    private Long communityId;

    @Size(max = 200)
    private String witnessInfo;

    @NotBlank
    private String authStatus;
}
