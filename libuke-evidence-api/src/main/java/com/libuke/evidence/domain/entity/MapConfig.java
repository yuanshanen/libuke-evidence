package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("map_configs")
public class MapConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String reverseGeocodeKey;
    private String jsApiKey;
    private String jsApiSecurityKey;
    private Integer defaultZoom;
    private BigDecimal defaultLongitude;
    private BigDecimal defaultLatitude;
    private Boolean enabled;
    private Boolean systemConfig;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
