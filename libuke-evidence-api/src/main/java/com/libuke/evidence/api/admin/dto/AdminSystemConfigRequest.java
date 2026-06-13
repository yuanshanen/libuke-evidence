package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminSystemConfigRequest {

    private String configValue;

    private Boolean editable;

    @Size(max = 255)
    private String remark;
}
