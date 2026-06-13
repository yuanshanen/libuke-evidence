package com.libuke.evidence.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.libuke.evidence.api.admin.dto.AdminSystemConfigRequest;
import com.libuke.evidence.api.admin.dto.AdminSystemConfigResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.SystemConfig;
import com.libuke.evidence.domain.mapper.SystemConfigMapper;
import com.libuke.evidence.domain.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public List<AdminSystemConfigResponse> listConfigs(String group, String keyword) {
        return systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                    .eq(StringUtils.hasText(group), SystemConfig::getConfigGroup, group)
                    .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SystemConfig::getConfigName, keyword)
                        .or()
                        .like(SystemConfig::getConfigKey, keyword)
                        .or()
                        .like(SystemConfig::getRemark, keyword)
                    )
                    .orderByAsc(SystemConfig::getConfigGroup)
                    .orderByAsc(SystemConfig::getId)
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSystemConfigResponse updateConfig(Long configId, AdminSystemConfigRequest request) {
        SystemConfig config = systemConfigMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException("系统配置不存在");
        }
        if (!Boolean.TRUE.equals(config.getEditable())) {
            throw new BusinessException("该配置不允许编辑");
        }
        if (StringUtils.hasText(request.getConfigValue())) {
            config.setConfigValue(request.getConfigValue().trim());
        } else if (!Boolean.TRUE.equals(config.getSensitive())) {
            config.setConfigValue(null);
        }
        if (request.getEditable() != null) {
            config.setEditable(request.getEditable());
        }
        config.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        systemConfigMapper.updateById(config);
        return toResponse(systemConfigMapper.selectById(configId));
    }

    private AdminSystemConfigResponse toResponse(SystemConfig config) {
        return AdminSystemConfigResponse.builder()
            .id(config.getId())
            .configGroup(config.getConfigGroup())
            .configKey(config.getConfigKey())
            .configName(config.getConfigName())
            .configValue(maskValue(config))
            .valueType(config.getValueType())
            .encrypted(config.getEncrypted())
            .sensitive(config.getSensitive())
            .editable(config.getEditable())
            .remark(config.getRemark())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
    }

    private String maskValue(SystemConfig config) {
        if (!Boolean.TRUE.equals(config.getSensitive())) {
            return config.getConfigValue();
        }
        if (!StringUtils.hasText(config.getConfigValue())) {
            return null;
        }
        String value = config.getConfigValue();
        String suffix = value.length() <= 4 ? "" : value.substring(value.length() - 4);
        return "******" + suffix;
    }
}
