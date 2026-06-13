package com.libuke.evidence.api.miniapp;

import com.libuke.evidence.api.admin.dto.AdminReportCategoryResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/miniapp/v1/report-categories")
public class MiniReportCategoryController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<List<AdminReportCategoryResponse>> listReportCategories() {
        return ApiResponse.ok(adminService.listReportCategories(null, true));
    }
}
