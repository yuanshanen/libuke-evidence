package com.libuke.evidence.domain.service.impl;

import com.libuke.evidence.api.admin.dto.AdminReportStreamEventResponse;
import com.libuke.evidence.domain.entity.Report;
import com.libuke.evidence.domain.service.ReportStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 问题记录实时推送服务实现
 */
@Service
@Slf4j
public class ReportStreamServiceImpl implements ReportStreamService {

    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L;
    private static final long RECONNECT_TIME = 5_000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> communityEmitters = new ConcurrentHashMap<>();
    private final Map<Long, StreamSubscription> adminSubscriptions = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long adminId, Long communityId) {
        closeAdminSubscription(adminId);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        StreamSubscription subscription = new StreamSubscription(adminId, communityId, emitter);
        adminSubscriptions.put(adminId, subscription);
        communityEmitters.computeIfAbsent(communityId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeSubscription(subscription));
        emitter.onTimeout(() -> removeSubscription(subscription));
        emitter.onError(error -> removeSubscription(subscription));
        sendEvent(
            communityId,
            emitter,
            "connected",
            AdminReportStreamEventResponse.builder()
                .eventType("connected")
                .communityId(communityId)
                .build()
        );
        log.info("订阅问题上报事件，adminId={}, communityId={}", adminId, communityId);
        return emitter;
    }

    @Override
    public void publishReportCreated(Report report) {
        if (report == null || report.getCommunityId() == null || !"pending".equals(report.getStatus())) {
            return;
        }
        List<SseEmitter> emitters = communityEmitters.getOrDefault(report.getCommunityId(), new CopyOnWriteArrayList<>());
        if (emitters.isEmpty()) {
            return;
        }
        AdminReportStreamEventResponse event = AdminReportStreamEventResponse.builder()
            .eventType("report_created")
            .reportId(report.getId())
            .communityId(report.getCommunityId())
            .reportNo(report.getReportNo())
            .status(report.getStatus())
            .submittedAt(report.getSubmittedAt())
            .build();
        emitters.forEach(emitter -> sendEvent(report.getCommunityId(), emitter, "report-created", event));
        log.info("推送新问题上报事件，reportId={}, communityId={}, subscriberCount={}", report.getId(), report.getCommunityId(), emitters.size());
    }

    private void sendEvent(Long communityId, SseEmitter emitter, String eventName, AdminReportStreamEventResponse event) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .reconnectTime(RECONNECT_TIME)
                .data(event));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(communityId, emitter);
        }
    }

    private void closeAdminSubscription(Long adminId) {
        StreamSubscription subscription = adminSubscriptions.remove(adminId);
        if (subscription == null) {
            return;
        }
        removeEmitter(subscription.communityId(), subscription.emitter());
        try {
            subscription.emitter().complete();
        } catch (IllegalStateException ignored) {
            // 连接可能已经由客户端关闭，服务端只需要确保本地引用已清理。
        }
    }

    private void removeSubscription(StreamSubscription subscription) {
        adminSubscriptions.remove(subscription.adminId(), subscription);
        removeEmitter(subscription.communityId(), subscription.emitter());
    }

    private void removeEmitter(Long communityId, SseEmitter emitter) {
        adminSubscriptions.entrySet().removeIf(entry -> entry.getValue().emitter() == emitter);
        List<SseEmitter> emitters = communityEmitters.get(communityId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            communityEmitters.remove(communityId);
        }
    }

    private record StreamSubscription(Long adminId, Long communityId, SseEmitter emitter) {
    }
}
