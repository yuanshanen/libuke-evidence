package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminRoleResponse {

    private Long id;
    private String code;
    private String name;
    private String remark;
    private Boolean enabled;
    private List<Long> menuIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
