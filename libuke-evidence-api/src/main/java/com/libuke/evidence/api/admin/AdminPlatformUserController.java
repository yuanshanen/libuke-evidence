package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.PlatformUserRequest;
import com.libuke.evidence.api.admin.dto.PlatformUserResponse;
import com.libuke.evidence.api.admin.dto.ResetPasswordRequest;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/platform-users")
public class AdminPlatformUserController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResponse<PlatformUserResponse>> pageUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(adminService.pagePlatformUsers(keyword, enabled, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<PlatformUserResponse> createUser(@Valid @RequestBody PlatformUserRequest request) {
        return ApiResponse.ok(adminService.createPlatformUser(request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<PlatformUserResponse> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody PlatformUserRequest request
    ) {
        return ApiResponse.ok(adminService.updatePlatformUser(userId, request));
    }

    @PatchMapping("/{userId}/password")
    public ApiResponse<PlatformUserResponse> resetPassword(
        @PathVariable Long userId,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ApiResponse.ok(adminService.resetPlatformUserPassword(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deletePlatformUser(userId);
        return ApiResponse.ok(null);
    }
}
