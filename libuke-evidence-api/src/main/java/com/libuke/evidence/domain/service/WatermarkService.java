package com.libuke.evidence.domain.service;

import com.libuke.evidence.api.admin.dto.AdminWatermarkTaskResponse;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTemplateRequest;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTemplateResponse;
import com.libuke.evidence.api.dto.PageResponse;

import java.util.List;

public interface WatermarkService {

    void createTask(Long imageId);

    void processReportAsync(Long reportId);

    void retryTask(Long taskId);

    PageResponse<AdminWatermarkTaskResponse> pageTasks(String status, long pageNo, long pageSize);

    List<AdminWatermarkTemplateResponse> listTemplates();

    AdminWatermarkTemplateResponse createTemplate(AdminWatermarkTemplateRequest request);

    AdminWatermarkTemplateResponse updateTemplate(Long templateId, AdminWatermarkTemplateRequest request);

    void deleteTemplate(Long templateId);
}
