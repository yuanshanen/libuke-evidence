package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminRoleRequest;
import com.libuke.evidence.api.admin.dto.AdminRoleResponse;
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
@RequestMapping("/admin/v1/roles")
public class AdminRoleController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResponse<AdminRoleResponse>> pageRoles(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(adminService.pageRoles(keyword, enabled, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminRoleResponse> createRole(@Valid @RequestBody AdminRoleRequest request) {
        return ApiResponse.ok(adminService.createRole(request));
    }

    @PutMapping("/{roleId}")
    public ApiResponse<AdminRoleResponse> updateRole(
        @PathVariable Long roleId,
        @Valid @RequestBody AdminRoleRequest request
    ) {
        return ApiResponse.ok(adminService.updateRole(roleId, request));
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId) {
        adminService.deleteRole(roleId);
        return ApiResponse.ok(null);
    }
}
