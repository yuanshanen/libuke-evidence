package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_policies")
public class UploadPolicy {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String scene;
    private Integer maxFileCount;
    private Integer maxFileSizeMb;
    private String allowedMimeTypes;
    private Boolean compressEnabled;
    private Boolean enabled;
    private Boolean systemConfig;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
