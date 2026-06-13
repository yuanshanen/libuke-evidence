package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminDashboardResponse;
import com.libuke.evidence.api.admin.dto.AdminCommunityResponse;
import com.libuke.evidence.api.admin.dto.DashboardCommunityMapResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/dashboard")
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/map-communities")
    public ApiResponse<List<AdminCommunityResponse>> mapCommunities() {
        return ApiResponse.ok(adminService.dashboardMapCommunities());
    }

    @GetMapping("/community-map")
    public ApiResponse<DashboardCommunityMapResponse> communityMap(@RequestParam(required = false) Long communityId) {
        return ApiResponse.ok(adminService.dashboardCommunityMap(communityId));
    }
}
