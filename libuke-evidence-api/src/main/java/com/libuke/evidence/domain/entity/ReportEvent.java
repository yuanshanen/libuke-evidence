package com.libuke.evidence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author libuke
 * @date 2026-06-06 00:00
 * @desc 问题记录事件 用于记录物业处置过程时间线
 */
@Data
@TableName("report_events")
public class ReportEvent {

    /**
     * 事件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 问题记录ID
     */
    private Long reportId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 变更前状态
     */
    private String fromStatus;

    /**
     * 变更后状态
     */
    private String toStatus;

    /**
     * 操作人类型
     */
    private String operatorType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 事件内容
     */
    private String content;

    /**
     * 事件时间
     */
    private LocalDateTime createdAt;
}
