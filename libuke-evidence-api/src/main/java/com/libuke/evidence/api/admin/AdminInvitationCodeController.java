package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminInvitationCodeRequest;
import com.libuke.evidence.api.admin.dto.AdminInvitationCodeResponse;
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
@RequestMapping("/admin/v1/invitation-codes")
public class AdminInvitationCodeController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResponse<AdminInvitationCodeResponse>> pageInvitationCodes(
        @RequestParam(required = false) Long communityId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(adminService.pageInvitationCodes(communityId, keyword, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminInvitationCodeResponse> createInvitationCode(@Valid @RequestBody AdminInvitationCodeRequest request) {
        return ApiResponse.ok(adminService.createInvitationCode(request));
    }

    @PutMapping("/{invitationCodeId}")
    public ApiResponse<AdminInvitationCodeResponse> updateInvitationCode(
        @PathVariable Long invitationCodeId,
        @Valid @RequestBody AdminInvitationCodeRequest request
    ) {
        return ApiResponse.ok(adminService.updateInvitationCode(invitationCodeId, request));
    }

    @DeleteMapping("/{invitationCodeId}")
    public ApiResponse<Void> deleteInvitationCode(@PathVariable Long invitationCodeId) {
        adminService.deleteInvitationCode(invitationCodeId);
        return ApiResponse.ok(null);
    }
}
