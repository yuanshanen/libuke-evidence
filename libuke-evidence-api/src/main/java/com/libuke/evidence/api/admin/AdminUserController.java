package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminUserRequest;
import com.libuke.evidence.api.admin.dto.AdminUserResponse;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/users")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> pageUsers(
        @RequestParam(required = false) Long communityId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(adminService.pageUsers(communityId, keyword, pageNo, pageSize));
    }

    @PutMapping("/{userId}")
    public ApiResponse<AdminUserResponse> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody AdminUserRequest request
    ) {
        return ApiResponse.ok(adminService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ApiResponse.ok(null);
    }
}
