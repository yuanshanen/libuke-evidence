package com.libuke.evidence.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author libuke
 * @date 2026-06-07 00:00
 * @desc 当前后台账号资料修改请求
 */
@Data
public class AdminProfileUpdateRequest {

    /**
     * 显示名称
     */
    @NotBlank
    @Size(max = 100)
    private String displayName;

    /**
     * 联系电话
     */
    @Size(max = 50)
    private String phone;
}
