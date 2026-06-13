package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminMenuRequest;
import com.libuke.evidence.api.admin.dto.AdminMenuResponse;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/menus")
public class AdminMenuController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<List<AdminMenuResponse>> listMenus(
        @RequestParam(defaultValue = "true") Boolean includeButtons
    ) {
        return ApiResponse.ok(adminService.listMenus(includeButtons));
    }

    @PostMapping
    public ApiResponse<AdminMenuResponse> createMenu(@Valid @RequestBody AdminMenuRequest request) {
        return ApiResponse.ok(adminService.createMenu(request));
    }

    @PutMapping("/{menuId}")
    public ApiResponse<AdminMenuResponse> updateMenu(
        @PathVariable Long menuId,
        @Valid @RequestBody AdminMenuRequest request
    ) {
        return ApiResponse.ok(adminService.updateMenu(menuId, request));
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long menuId) {
        adminService.deleteMenu(menuId);
        return ApiResponse.ok(null);
    }
}
