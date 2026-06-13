package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("communities")
public class Community {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String address;
    private String principalName;
    private String principalPhone;
    private Boolean enabled;
    private BigDecimal centerLng;
    private BigDecimal centerLat;
    private String boundaryJson;
    private String buildingColor;

    /**
     * 小区地图默认缩放级别
     */
    private Integer mapZoom;

    /**
     * 小区地图默认俯仰角
     */
    private Integer mapPitch;

    /**
     * 小区地图默认旋转角
     */
    private Integer mapRotation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
