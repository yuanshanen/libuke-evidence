package com.libuke.evidence.api.miniapp;

import com.libuke.evidence.api.dto.DirectUploadPolicyRequest;
import com.libuke.evidence.api.dto.DirectUploadPolicyResponse;
import com.libuke.evidence.api.dto.ReportImageUploadResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.ReportService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/miniapp/v1/report-images")
public class MiniReportImageController {

    private final ReportService reportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReportImageUploadResponse> uploadReportImage(
        @NotBlank @RequestParam String openid,
        @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(reportService.uploadReportImage(openid, file));
    }

    @PostMapping("/policy")
    public ApiResponse<DirectUploadPolicyResponse> createDirectUploadPolicy(
        @jakarta.validation.Valid @RequestBody DirectUploadPolicyRequest request
    ) {
        return ApiResponse.ok(reportService.createReportImageDirectUploadPolicy(request.getOpenid(), request.getFileName()));
    }
}
