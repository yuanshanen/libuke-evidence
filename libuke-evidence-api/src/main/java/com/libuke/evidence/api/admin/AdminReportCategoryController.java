package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminReportCategoryRequest;
import com.libuke.evidence.api.admin.dto.AdminReportCategoryResponse;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/report-categories")
public class AdminReportCategoryController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<List<AdminReportCategoryResponse>> listReportCategories(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(adminService.listReportCategories(keyword, enabled));
    }

    @PostMapping
    public ApiResponse<AdminReportCategoryResponse> createReportCategory(
        @Valid @RequestBody AdminReportCategoryRequest request
    ) {
        return ApiResponse.ok(adminService.createReportCategory(request));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<AdminReportCategoryResponse> updateReportCategory(
        @PathVariable Long categoryId,
        @Valid @RequestBody AdminReportCategoryRequest request
    ) {
        return ApiResponse.ok(adminService.updateReportCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteReportCategory(@PathVariable Long categoryId) {
        adminService.deleteReportCategory(categoryId);
        return ApiResponse.ok(null);
    }
}
