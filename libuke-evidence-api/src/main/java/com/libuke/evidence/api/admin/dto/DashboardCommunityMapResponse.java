package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardCommunityMapResponse {

    private Long communityId;
    private String communityName;
    private List<BigDecimal> center;
    private List<List<BigDecimal>> boundary;
    private String buildingColor;
    private Integer zoom;

    /**
     * 小区地图默认俯仰角
     */
    private Integer pitch;

    /**
     * 小区地图默认旋转角
     */
    private Integer rotation;

    private Long reportCount;
    private Long todayReportCount;
}
