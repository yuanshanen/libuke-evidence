package com.libuke.evidence.domain.service;

import com.libuke.evidence.api.admin.dto.AdminSystemConfigRequest;
import com.libuke.evidence.api.admin.dto.AdminSystemConfigResponse;

import java.util.List;

public interface SystemConfigService {

    List<AdminSystemConfigResponse> listConfigs(String group, String keyword);

    AdminSystemConfigResponse updateConfig(Long configId, AdminSystemConfigRequest request);
}
