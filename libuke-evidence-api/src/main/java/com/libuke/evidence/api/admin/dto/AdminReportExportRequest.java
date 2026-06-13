package com.libuke.evidence.api.admin.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 问题记录导出条件
 */
@Data
public class AdminReportExportRequest {

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 小区ID
     */
    private Long communityId;

    /**
     * 问题分类
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

    /**
     * 指定导出的记录ID
     */
    private List<Long> ids;
}
