package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author libuke
 * @date 2026-06-06 00:00
 * @desc 问题地图点位 用于首页地图展示待处理上报位置
 */
@Data
@Builder
public class AdminReportMapPointResponse {

    /**
     * 记录ID
     */
    private Long reportId;

    /**
     * 记录编号
     */
    private String reportNo;

    /**
     * 小区ID
     */
    private Long communityId;

    /**
     * 问题大类
     */
    private String category;

    /**
     * 问题小类
     */
    private String subCategory;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 位置地址
     */
    private String locationAddress;

    /**
     * 首图临时访问地址
     */
    private String firstImageUrl;

    /**
     * 上报时间
     */
    private LocalDateTime submittedAt;
}
