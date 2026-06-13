package com.libuke.evidence.domain.service;

import com.libuke.evidence.domain.entity.Report;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 问题记录实时推送服务
 */
public interface ReportStreamService {

    /**
     * 订阅指定小区的问题记录事件
     *
     * @param adminId 管理员ID
     * @param communityId 小区ID
     * @return SSE 连接
     */
    SseEmitter subscribe(Long adminId, Long communityId);

    /**
     * 推送新上报问题事件
     *
     * @param report 问题记录
     */
    void publishReportCreated(Report report);
}
