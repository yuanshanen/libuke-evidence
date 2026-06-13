package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminCommunityResponse {

    private Long id;
    private String name;
    private String communityName;
    private String address;
    private String principalName;
    private String principalPhone;
    private Boolean enabled;
    private Integer status;
    private List<BigDecimal> center;
    private List<List<BigDecimal>> boundary;
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

    private Long userCount;
    private Long reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
