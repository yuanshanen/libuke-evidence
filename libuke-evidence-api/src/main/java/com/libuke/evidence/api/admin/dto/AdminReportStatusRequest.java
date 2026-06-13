package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportStatusRequest {

    /**
     * 处置状态
     */
    @NotBlank
    private String status;

    /**
     * 后台处置说明
     */
    @Size(max = 500)
    private String adminNote;
}
