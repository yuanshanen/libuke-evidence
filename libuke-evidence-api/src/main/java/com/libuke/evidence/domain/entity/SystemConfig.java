package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_configs")
public class SystemConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String configGroup;
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private Boolean encrypted;
    @TableField("`sensitive`")
    private Boolean sensitive;
    private Boolean editable;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
