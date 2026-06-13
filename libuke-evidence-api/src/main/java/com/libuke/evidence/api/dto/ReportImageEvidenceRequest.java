package com.libuke.evidence.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上报图片元数据
 */
@Data
public class ReportImageEvidenceRequest {

    /**
     * OSS 原图对象 Key
     */
    @NotBlank
    private String objectKey;

    /**
     * 客户端上传时间，ISO-8601 字符串
     */
    @Size(max = 64)
    private String clientUploadedAt;

    /**
     * 客户端原始文件名
     */
    @Size(max = 255)
    private String originalFileName;
}
