package com.libuke.evidence.api.admin.dto;

import lombok.Data;

@Data
public class AdminUploadPolicyRequest {
    private String name;
    private String scene;
    private Integer maxFileCount;
    private Integer maxFileSizeMb;
    private String allowedMimeTypes;
    private Boolean compressEnabled;
    private Boolean enabled;
    private String remark;
}
