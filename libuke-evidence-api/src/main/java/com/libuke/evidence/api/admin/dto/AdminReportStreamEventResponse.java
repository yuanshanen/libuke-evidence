package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author libuke
 * @date 2026-06-06 00:00
 * @desc 问题记录实时事件响应 用于后台首页 SSE 刷新待处理点位
 */
@Data
@Builder
public class AdminReportStreamEventResponse {

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 问题记录ID
     */
    private Long reportId;

    /**
     * 小区ID
     */
    private Long communityId;

    /**
     * 问题编号
     */
    private String reportNo;

    /**
     * 处置状态
     */
    private String status;

    /**
     * 上报时间
     */
    private LocalDateTime submittedAt;
}
