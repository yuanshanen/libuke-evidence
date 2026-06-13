package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminAnalyticsOverviewResponse;
import com.libuke.evidence.api.admin.dto.AdminAnalyticsRequest;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 后台统计分析 提供物业问题上报与处理情况统计
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/analytics")
public class AdminAnalyticsController {

    private final AdminService adminService;

    /**
     * 查询统计分析总览
     *
     * @param request 查询条件
     * @return 统计分析数据
     */
    @GetMapping("/overview")
    public ApiResponse<AdminAnalyticsOverviewResponse> overview(@ModelAttribute AdminAnalyticsRequest request) {
        return ApiResponse.ok(adminService.analyticsOverview(request));
    }
}
