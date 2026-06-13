package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminCommunityRequest;
import com.libuke.evidence.api.admin.dto.AdminCommunityResponse;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/communities")
public class AdminCommunityController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResponse<AdminCommunityResponse>> pageCommunities(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(adminService.pageCommunities(keyword, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminCommunityResponse> createCommunity(@Valid @RequestBody AdminCommunityRequest request) {
        return ApiResponse.ok(adminService.createCommunity(request));
    }

    @PutMapping("/{communityId}")
    public ApiResponse<AdminCommunityResponse> updateCommunity(
        @PathVariable Long communityId,
        @Valid @RequestBody AdminCommunityRequest request
    ) {
        return ApiResponse.ok(adminService.updateCommunity(communityId, request));
    }

    @DeleteMapping("/{communityId}")
    public ApiResponse<Void> deleteCommunity(@PathVariable Long communityId) {
        adminService.deleteCommunity(communityId);
        return ApiResponse.ok(null);
    }
}
