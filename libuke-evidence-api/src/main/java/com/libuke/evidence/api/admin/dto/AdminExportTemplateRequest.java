package com.libuke.evidence.api.admin.dto;

import lombok.Data;

@Data
public class AdminExportTemplateRequest {
    private String name;
    private String scene;
    private String fieldsJson;
    private Boolean includeOriginalLinks;
    private Boolean includeWatermarkedLinks;
    private Integer fileRetentionDays;
    private Boolean enabled;
    private String remark;
}
