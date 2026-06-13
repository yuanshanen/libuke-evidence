package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_images")
public class ReportImage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long reportId;
    private String originalObjectKey;
    private Long originalFileSize;
    private String originalMimeType;
    private String originalFileName;
    private String watermarkedObjectKey;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer sortOrder;
    private String processStatus;
    private LocalDateTime clientUploadedAt;
    private LocalDateTime serverReceivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
