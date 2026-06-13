package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 当前后台账号密码修改请求
 */
@Data
public class AdminPasswordUpdateRequest {

    /**
     * 当前密码
     */
    @NotBlank
    @Size(min = 6, max = 64)
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;
}
