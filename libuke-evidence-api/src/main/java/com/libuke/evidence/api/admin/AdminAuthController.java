package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminLoginRequest;
import com.libuke.evidence.api.admin.dto.AdminLoginResponse;
import com.libuke.evidence.api.admin.dto.AdminMenuResponse;
import com.libuke.evidence.api.admin.dto.AdminPasswordUpdateRequest;
import com.libuke.evidence.api.admin.dto.AdminProfileUpdateRequest;
import com.libuke.evidence.api.admin.dto.AdminUserInfoResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 后台认证 提供登录、当前账号信息、菜单和个人中心能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/auth")
public class AdminAuthController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminService.login(request));
    }

    @GetMapping("/user-info")
    public ApiResponse<AdminUserInfoResponse> userInfo() {
        return ApiResponse.ok(adminService.currentUserInfo());
    }

    /**
     * 修改当前登录账号资料
     *
     * @param request 账号资料
     * @return 当前账号信息
     */
    @PutMapping("/profile")
    public ApiResponse<AdminUserInfoResponse> updateProfile(@Valid @RequestBody AdminProfileUpdateRequest request) {
        return ApiResponse.ok(adminService.updateCurrentProfile(request));
    }

    /**
     * 修改当前登录账号密码
     *
     * @param request 密码信息
     * @return 空结果
     */
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody AdminPasswordUpdateRequest request) {
        adminService.updateCurrentPassword(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/menus")
    public ApiResponse<List<AdminMenuResponse>> menus() {
        return ApiResponse.ok(adminService.currentMenus());
    }
}
