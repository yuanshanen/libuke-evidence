package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 统计分析总览数据
 */
@Data
@Builder
public class AdminAnalyticsOverviewResponse {

    /**
     * 上报总数
     */
    private long totalCount;

    /**
     * 待处理数
     */
    private long pendingCount;

    /**
     * 处理中数
     */
    private long processingCount;

    /**
     * 已处理数
     */
    private long resolvedCount;

    /**
     * 超时未处理数
     */
    private long overdueCount;

    /**
     * 平均处理时长，单位小时
     */
    private double avgProcessHours;

    /**
     * 每日趋势
     */
    private List<DailyTrendItem> dailyTrend;

    /**
     * 问题大类分布
     */
    private List<NameCountItem> categoryDistribution;

    /**
     * 问题小类 Top10
     */
    private List<NameCountItem> subCategoryTop;

    /**
     * 状态分布
     */
    private List<StatusCountItem> statusDistribution;

    /**
     * 超时未处理列表
     */
    private List<OverdueReportItem> overdueReports;

    @Data
    @Builder
    public static class DailyTrendItem {

        /**
         * 日期
         */
        private LocalDate date;

        /**
         * 上报数量
         */
        private long submittedCount;

        /**
         * 处理完成数量
         */
        private long resolvedCount;
    }

    @Data
    @Builder
    public static class NameCountItem {

        /**
         * 名称
         */
        private String name;

        /**
         * 数量
         */
        private long count;
    }

    @Data
    @Builder
    public static class StatusCountItem {

        /**
         * 状态值
         */
        private String status;

        /**
         * 状态名称
         */
        private String label;

        /**
         * 数量
         */
        private long count;
    }

    @Data
    @Builder
    public static class OverdueReportItem {

        /**
         * 记录ID
         */
        private Long id;

        /**
         * 记录编号
         */
        private String reportNo;

        /**
         * 小区名称
         */
        private String communityName;

        /**
         * 问题小类
         */
        private String subCategory;

        /**
         * 位置
         */
        private String locationAddress;

        /**
         * 当前状态
         */
        private String status;

        /**
         * 上报时间
         */
        private LocalDateTime submittedAt;
    }
}
