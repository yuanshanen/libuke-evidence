package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("export_templates")
public class ExportTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String scene;
    private String fieldsJson;
    private Boolean includeOriginalLinks;
    private Boolean includeWatermarkedLinks;
    private Integer fileRetentionDays;
    private Boolean enabled;
    private Boolean systemConfig;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
