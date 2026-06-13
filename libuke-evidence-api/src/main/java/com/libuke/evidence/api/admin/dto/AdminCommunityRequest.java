package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminCommunityRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String communityName;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String principalName;

    @Size(max = 50)
    private String principalPhone;

    private Boolean enabled;
    private Integer status;
    private List<BigDecimal> center;
    private List<List<BigDecimal>> boundary;

    @Size(max = 20)
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
}
