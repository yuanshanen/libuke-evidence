package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminUploadResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.integration.oss.OssUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/uploads")
public class AdminUploadController {

    private final OssUploadService ossUploadService;

    @PostMapping("/avatar")
    public ApiResponse<AdminUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String objectKey = ossUploadService.uploadAvatarImage(file);
        return ApiResponse.ok(AdminUploadResponse.builder()
            .objectKey(objectKey)
            .url(ossUploadService.generateTemporaryUrl(objectKey))
            .build());
    }
}
