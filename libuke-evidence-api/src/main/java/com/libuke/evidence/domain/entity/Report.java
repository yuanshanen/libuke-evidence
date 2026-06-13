package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reports")
public class Report {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String reportNo;
    private Long userId;
    private Long communityId;
    private String category;
    private String subCategory;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String locationAddress;
    private String remark;
    private String status;
    private String adminNote;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
