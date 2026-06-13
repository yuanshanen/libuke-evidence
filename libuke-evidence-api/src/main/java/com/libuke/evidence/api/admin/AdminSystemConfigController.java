package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminExportTemplateRequest;
import com.libuke.evidence.api.admin.dto.AdminMapConfigRequest;
import com.libuke.evidence.api.admin.dto.AdminStorageConfigRequest;
import com.libuke.evidence.api.admin.dto.AdminSystemConfigRequest;
import com.libuke.evidence.api.admin.dto.AdminSystemConfigResponse;
import com.libuke.evidence.api.admin.dto.AdminUploadPolicyRequest;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.ExportTemplate;
import com.libuke.evidence.domain.entity.MapConfig;
import com.libuke.evidence.domain.entity.StorageConfig;
import com.libuke.evidence.domain.entity.UploadPolicy;
import com.libuke.evidence.domain.mapper.ExportTemplateMapper;
import com.libuke.evidence.domain.mapper.MapConfigMapper;
import com.libuke.evidence.domain.mapper.StorageConfigMapper;
import com.libuke.evidence.domain.mapper.UploadPolicyMapper;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import com.libuke.evidence.domain.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/system-configs")
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;
    private final StorageConfigMapper storageConfigMapper;
    private final MapConfigMapper mapConfigMapper;
    private final UploadPolicyMapper uploadPolicyMapper;
    private final ExportTemplateMapper exportTemplateMapper;
    private final RuntimeConfigService runtimeConfigService;

    @GetMapping
    public ApiResponse<List<AdminSystemConfigResponse>> listConfigs(
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(systemConfigService.listConfigs(group, keyword));
    }

    @PutMapping("/{configId}")
    public ApiResponse<AdminSystemConfigResponse> updateConfig(
        @PathVariable Long configId,
        @Valid @RequestBody AdminSystemConfigRequest request
    ) {
        AdminSystemConfigResponse response = systemConfigService.updateConfig(configId, request);
        runtimeConfigService.refresh();
        return ApiResponse.ok(response);
    }

    @GetMapping("/storage")
    public ApiResponse<List<StorageConfig>> listStorageConfigs() {
        return ApiResponse.ok(storageConfigMapper.selectList(null).stream().map(this::maskStorageConfig).toList());
    }

    @PutMapping("/storage/{configId}")
    public ApiResponse<StorageConfig> updateStorageConfig(
        @PathVariable Long configId,
        @RequestBody AdminStorageConfigRequest request
    ) {
        StorageConfig config = storageConfigMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException("存储配置不存在");
        }
        config.setName(trimOrNull(request.getName()));
        config.setProvider(trimOrNull(request.getProvider()));
        config.setEndpoint(trimOrNull(request.getEndpoint()));
        config.setRegion(trimOrNull(request.getRegion()));
        config.setBucketName(trimOrNull(request.getBucketName()));
        if (StringUtils.hasText(request.getAccessKeyId())) {
            config.setAccessKeyId(request.getAccessKeyId().trim());
        }
        if (StringUtils.hasText(request.getAccessKeySecret())) {
            config.setAccessKeySecret(request.getAccessKeySecret().trim());
        }
        config.setUploadDir(trimOrNull(request.getUploadDir()));
        config.setOriginalDir(trimOrNull(request.getOriginalDir()));
        config.setWatermarkedDir(trimOrNull(request.getWatermarkedDir()));
        config.setAvatarDir(trimOrNull(request.getAvatarDir()));
        config.setPresignedUrlMinutes(request.getPresignedUrlMinutes());
        config.setEnabled(request.getEnabled());
        config.setRemark(trimOrNull(request.getRemark()));
        storageConfigMapper.updateById(config);
        runtimeConfigService.refresh();
        return ApiResponse.ok(maskStorageConfig(storageConfigMapper.selectById(configId)));
    }

    @GetMapping("/maps")
    public ApiResponse<List<MapConfig>> listMapConfigs() {
        return ApiResponse.ok(mapConfigMapper.selectList(null).stream().map(this::maskMapConfig).toList());
    }

    @PutMapping("/maps/{configId}")
    public ApiResponse<MapConfig> updateMapConfig(
        @PathVariable Long configId,
        @RequestBody AdminMapConfigRequest request
    ) {
        MapConfig config = mapConfigMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException("地图配置不存在");
        }
        if (StringUtils.hasText(request.getReverseGeocodeKey())) {
            config.setReverseGeocodeKey(request.getReverseGeocodeKey().trim());
        }
        if (StringUtils.hasText(request.getJsApiKey())) {
            config.setJsApiKey(request.getJsApiKey().trim());
        }
        if (StringUtils.hasText(request.getJsApiSecurityKey())) {
            config.setJsApiSecurityKey(request.getJsApiSecurityKey().trim());
        }
        config.setDefaultZoom(request.getDefaultZoom());
        config.setDefaultLongitude(request.getDefaultLongitude());
        config.setDefaultLatitude(request.getDefaultLatitude());
        config.setEnabled(request.getEnabled());
        config.setRemark(trimOrNull(request.getRemark()));
        mapConfigMapper.updateById(config);
        runtimeConfigService.refresh();
        return ApiResponse.ok(maskMapConfig(mapConfigMapper.selectById(configId)));
    }

    @GetMapping("/upload-policies")
    public ApiResponse<List<UploadPolicy>> listUploadPolicies() {
        return ApiResponse.ok(uploadPolicyMapper.selectList(null));
    }

    @PutMapping("/upload-policies/{policyId}")
    public ApiResponse<UploadPolicy> updateUploadPolicy(
        @PathVariable Long policyId,
        @RequestBody AdminUploadPolicyRequest request
    ) {
        UploadPolicy policy = uploadPolicyMapper.selectById(policyId);
        if (policy == null) {
            throw new BusinessException("上传策略不存在");
        }
        policy.setName(trimOrNull(request.getName()));
        policy.setScene(trimOrNull(request.getScene()));
        policy.setMaxFileCount(request.getMaxFileCount());
        policy.setMaxFileSizeMb(request.getMaxFileSizeMb());
        policy.setAllowedMimeTypes(trimOrNull(request.getAllowedMimeTypes()));
        policy.setCompressEnabled(request.getCompressEnabled());
        policy.setEnabled(request.getEnabled());
        policy.setRemark(trimOrNull(request.getRemark()));
        uploadPolicyMapper.updateById(policy);
        runtimeConfigService.refresh();
        return ApiResponse.ok(uploadPolicyMapper.selectById(policyId));
    }

    @GetMapping("/export-templates")
    public ApiResponse<List<ExportTemplate>> listExportTemplates() {
        return ApiResponse.ok(exportTemplateMapper.selectList(null));
    }

    @PutMapping("/export-templates/{templateId}")
    public ApiResponse<ExportTemplate> updateExportTemplate(
        @PathVariable Long templateId,
        @RequestBody AdminExportTemplateRequest request
    ) {
        ExportTemplate template = exportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("导出配置不存在");
        }
        template.setName(trimOrNull(request.getName()));
        template.setScene(trimOrNull(request.getScene()));
        template.setFieldsJson(trimOrNull(request.getFieldsJson()));
        template.setIncludeOriginalLinks(request.getIncludeOriginalLinks());
        template.setIncludeWatermarkedLinks(request.getIncludeWatermarkedLinks());
        template.setFileRetentionDays(request.getFileRetentionDays());
        template.setEnabled(request.getEnabled());
        template.setRemark(trimOrNull(request.getRemark()));
        exportTemplateMapper.updateById(template);
        return ApiResponse.ok(exportTemplateMapper.selectById(templateId));
    }

    private StorageConfig maskStorageConfig(StorageConfig config) {
        config.setAccessKeyId(maskValue(config.getAccessKeyId()));
        config.setAccessKeySecret(maskValue(config.getAccessKeySecret()));
        return config;
    }

    private MapConfig maskMapConfig(MapConfig config) {
        config.setReverseGeocodeKey(maskValue(config.getReverseGeocodeKey()));
        config.setJsApiKey(maskValue(config.getJsApiKey()));
        config.setJsApiSecurityKey(maskValue(config.getJsApiSecurityKey()));
        return config;
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String suffix = value.length() <= 4 ? "" : value.substring(value.length() - 4);
        return "******" + suffix;
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
