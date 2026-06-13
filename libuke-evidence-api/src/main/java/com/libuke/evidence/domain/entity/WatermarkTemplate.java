package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("watermark_templates")
public class WatermarkTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Boolean enabled;
    private String position;
    private BigDecimal opacity;
    private BigDecimal backgroundOpacity;
    private Integer fontSize;
    private String textColor;
    private String backgroundColor;
    private String contentTemplate;
    private Boolean systemTemplate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
