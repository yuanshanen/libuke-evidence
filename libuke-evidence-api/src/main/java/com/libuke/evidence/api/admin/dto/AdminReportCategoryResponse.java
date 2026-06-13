package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminReportCategoryResponse {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private List<AdminReportCategoryResponse> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
