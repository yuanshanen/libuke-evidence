package com.libuke.evidence.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AdminDashboardResponse {

    private long reportCount;
    private long todayReportCount;
    private long communityCount;
    private long userCount;
    private Map<String, Long> categoryCounts;
    private Map<String, Long> statusCounts;
}
