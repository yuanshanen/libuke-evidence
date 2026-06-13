package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminWatermarkTaskResponse;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTemplateRequest;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTemplateResponse;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.WatermarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/admin/v1/watermarks")
public class AdminWatermarkController {

    private final WatermarkService watermarkService;

    @GetMapping("/templates")
    public ApiResponse<List<AdminWatermarkTemplateResponse>> listTemplates() {
        return ApiResponse.ok(watermarkService.listTemplates());
    }

    @PostMapping("/templates")
    public ApiResponse<AdminWatermarkTemplateResponse> createTemplate(
        @Valid @RequestBody AdminWatermarkTemplateRequest request
    ) {
        return ApiResponse.ok(watermarkService.createTemplate(request));
    }

    @PutMapping("/templates/{templateId}")
    public ApiResponse<AdminWatermarkTemplateResponse> updateTemplate(
        @PathVariable Long templateId,
        @Valid @RequestBody AdminWatermarkTemplateRequest request
    ) {
        return ApiResponse.ok(watermarkService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/templates/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long templateId) {
        watermarkService.deleteTemplate(templateId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResponse<AdminWatermarkTaskResponse>> pageTasks(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(watermarkService.pageTasks(status, pageNo, pageSize));
    }

    @PatchMapping("/tasks/{taskId}/retry")
    public ApiResponse<Void> retryTask(@PathVariable Long taskId) {
        watermarkService.retryTask(taskId);
        return ApiResponse.ok(null);
    }
}
