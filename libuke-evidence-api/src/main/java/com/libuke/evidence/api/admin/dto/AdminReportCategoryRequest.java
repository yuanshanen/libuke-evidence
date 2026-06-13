package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportCategoryRequest {

    private Long parentId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 64)
    private String code;

    private Integer sortOrder;

    private Boolean enabled;

    @Size(max = 255)
    private String remark;
}
