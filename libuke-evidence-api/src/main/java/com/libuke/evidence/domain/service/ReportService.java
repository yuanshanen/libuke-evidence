package com.libuke.evidence.domain.service;

import com.libuke.evidence.api.dto.CreateReportRequest;
import com.libuke.evidence.api.dto.CreateReportJsonRequest;
import com.libuke.evidence.api.dto.DirectUploadPolicyResponse;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.api.dto.ReportImageUploadResponse;
import com.libuke.evidence.api.dto.ReportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReportService {

    ReportResponse createReport(CreateReportRequest request);

    ReportResponse createReport(CreateReportJsonRequest request);

    ReportImageUploadResponse uploadReportImage(String openid, MultipartFile file);

    DirectUploadPolicyResponse createReportImageDirectUploadPolicy(String openid, String fileName);

    PageResponse<ReportResponse> pageMyReports(String openid, String category, long pageNo, long pageSize);

    ReportResponse getMyReport(String openid, Long reportId);
}
