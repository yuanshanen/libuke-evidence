package com.libuke.evidence.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.libuke.evidence.config.AmapMapProperties;
import com.libuke.evidence.config.OssProperties;
import com.libuke.evidence.domain.entity.MapConfig;
import com.libuke.evidence.domain.entity.StorageConfig;
import com.libuke.evidence.domain.entity.SystemConfig;
import com.libuke.evidence.domain.entity.UploadPolicy;
import com.libuke.evidence.domain.mapper.MapConfigMapper;
import com.libuke.evidence.domain.mapper.StorageConfigMapper;
import com.libuke.evidence.domain.mapper.SystemConfigMapper;
import com.libuke.evidence.domain.mapper.UploadPolicyMapper;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuntimeConfigServiceImpl implements RuntimeConfigService {

    private static final int DEFAULT_PRESIGNED_URL_MINUTES = 7 * 24 * 60;

    private final SystemConfigMapper systemConfigMapper;
    private final StorageConfigMapper storageConfigMapper;
    private final MapConfigMapper mapConfigMapper;
    private final UploadPolicyMapper uploadPolicyMapper;
    private final OssProperties ossProperties;
    private final AmapMapProperties amapMapProperties;

    private final Map<String, String> basicCache = new ConcurrentHashMap<>();
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private volatile StorageRuntimeConfig storageCache;
    private volatile MapRuntimeConfig mapCache;
    private final Map<String, UploadPolicyRuntimeConfig> uploadPolicyCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> basicConfigs() {
        if (basicCache.isEmpty()) {
            loadBasicConfigs();
        }
        return Map.copyOf(basicCache);
    }

    @Override
    public String configValue(String key, String fallback) {
        if (configCache.isEmpty()) {
            loadAllSystemConfigs();
        }
        return configCache.getOrDefault(key, fallback);
    }

    @Override
    public StorageRuntimeConfig storageConfig() {
        if (storageCache == null) {
            storageCache = loadStorageConfig();
        }
        return storageCache;
    }

    @Override
    public MapRuntimeConfig mapConfig() {
        if (mapCache == null) {
            mapCache = loadMapConfig();
        }
        return mapCache;
    }

    @Override
    public UploadPolicyRuntimeConfig uploadPolicy(String scene) {
        return uploadPolicyCache.computeIfAbsent(scene, this::loadUploadPolicy);
    }

    @Override
    public void refresh() {
        basicCache.clear();
        configCache.clear();
        storageCache = null;
        mapCache = null;
        uploadPolicyCache.clear();
    }

    private void loadBasicConfigs() {
        basicCache.putAll(systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigGroup, "basic")
                    .orderByAsc(SystemConfig::getId)
            )
            .stream()
            .collect(Collectors.toMap(SystemConfig::getConfigKey, item -> value(item.getConfigValue(), ""), (left, right) -> right)));
    }

    private void loadAllSystemConfigs() {
        configCache.putAll(systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId)
            )
            .stream()
            .collect(Collectors.toMap(SystemConfig::getConfigKey, item -> value(item.getConfigValue(), ""), (left, right) -> right)));
    }

    private StorageRuntimeConfig loadStorageConfig() {
        StorageConfig config = storageConfigMapper.selectOne(
            new LambdaQueryWrapper<StorageConfig>()
                .eq(StorageConfig::getEnabled, true)
                .orderByDesc(StorageConfig::getSystemConfig)
                .orderByAsc(StorageConfig::getId)
                .last("limit 1")
        );
        if (config == null) {
            return new StorageRuntimeConfig(
                ossProperties.getEndpoint(),
                ossProperties.getBucketName(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret(),
                value(ossProperties.getUploadDir(), "reports/original"),
                "reports/original",
                "reports/watermarked",
                "avatars",
                DEFAULT_PRESIGNED_URL_MINUTES
            );
        }
        return new StorageRuntimeConfig(
            value(config.getEndpoint(), ossProperties.getEndpoint()),
            value(config.getBucketName(), ossProperties.getBucketName()),
            value(config.getAccessKeyId(), ossProperties.getAccessKeyId()),
            value(config.getAccessKeySecret(), ossProperties.getAccessKeySecret()),
            value(config.getUploadDir(), ossProperties.getUploadDir()),
            value(config.getOriginalDir(), "reports/original"),
            value(config.getWatermarkedDir(), "reports/watermarked"),
            value(config.getAvatarDir(), "avatars"),
            config.getPresignedUrlMinutes() == null ? DEFAULT_PRESIGNED_URL_MINUTES : config.getPresignedUrlMinutes()
        );
    }

    private MapRuntimeConfig loadMapConfig() {
        MapConfig config = mapConfigMapper.selectOne(
            new LambdaQueryWrapper<MapConfig>()
                .eq(MapConfig::getEnabled, true)
                .orderByDesc(MapConfig::getSystemConfig)
                .orderByAsc(MapConfig::getId)
                .last("limit 1")
        );
        if (config == null) {
            return new MapRuntimeConfig(
                firstText(amapMapProperties.getReverseGeocodeKey(), amapMapProperties.getKey()),
                amapMapProperties.getJsApiKey(),
                amapMapProperties.getJsApiSecurityKey(),
                17,
                BigDecimal.valueOf(116.397128),
                BigDecimal.valueOf(39.916527)
            );
        }
        return new MapRuntimeConfig(
            value(config.getReverseGeocodeKey(), firstText(amapMapProperties.getReverseGeocodeKey(), amapMapProperties.getKey())),
            value(config.getJsApiKey(), amapMapProperties.getJsApiKey()),
            value(config.getJsApiSecurityKey(), amapMapProperties.getJsApiSecurityKey()),
            config.getDefaultZoom() == null ? 17 : config.getDefaultZoom(),
            config.getDefaultLongitude() == null ? BigDecimal.valueOf(116.397128) : config.getDefaultLongitude(),
            config.getDefaultLatitude() == null ? BigDecimal.valueOf(39.916527) : config.getDefaultLatitude()
        );
    }

    private UploadPolicyRuntimeConfig loadUploadPolicy(String scene) {
        UploadPolicy policy = uploadPolicyMapper.selectOne(
            new LambdaQueryWrapper<UploadPolicy>()
                .eq(UploadPolicy::getScene, scene)
                .eq(UploadPolicy::getEnabled, true)
                .orderByDesc(UploadPolicy::getSystemConfig)
                .orderByAsc(UploadPolicy::getId)
                .last("limit 1")
        );
        if (policy == null) {
            return new UploadPolicyRuntimeConfig(scene, 4, 20, "image/jpeg,image/png", false);
        }
        return new UploadPolicyRuntimeConfig(
            scene,
            policy.getMaxFileCount() == null ? 4 : policy.getMaxFileCount(),
            policy.getMaxFileSizeMb() == null ? 20 : policy.getMaxFileSizeMb(),
            value(policy.getAllowedMimeTypes(), "image/jpeg,image/png"),
            Boolean.TRUE.equals(policy.getCompressEnabled())
        );
    }

    private String value(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
