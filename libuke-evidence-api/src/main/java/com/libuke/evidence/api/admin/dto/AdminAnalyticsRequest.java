package com.libuke.evidence.api.admin.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 统计分析查询条件
 */
@Data
public class AdminAnalyticsRequest {

    /**
     * 小区ID
     */
    private Long communityId;

    /**
     * 问题大类
     */
    private String category;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 开始日期
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
