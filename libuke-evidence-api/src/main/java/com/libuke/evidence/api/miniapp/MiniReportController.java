package com.libuke.evidence.api.miniapp;

import com.libuke.evidence.api.dto.CreateReportJsonRequest;
import com.libuke.evidence.api.dto.CreateReportRequest;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.api.dto.ReportResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/miniapp/v1/reports")
public class MiniReportController {

    private final ReportService reportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReportResponse> createReport(@Valid @ModelAttribute CreateReportRequest request) {
        return ApiResponse.ok(reportService.createReport(request));
    }

    @PostMapping("/json")
    public ApiResponse<ReportResponse> createReportFromUploadedImages(@Valid @RequestBody CreateReportJsonRequest request) {
        return ApiResponse.ok(reportService.createReport(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReportResponse>> pageMyReports(
        @NotBlank @RequestParam String openid,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(reportService.pageMyReports(openid, category, pageNo, pageSize));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponse> getMyReport(
        @NotBlank @RequestParam String openid,
        @PathVariable Long reportId
    ) {
        return ApiResponse.ok(reportService.getMyReport(openid, reportId));
    }
}
