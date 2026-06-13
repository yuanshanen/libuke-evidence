package com.libuke.evidence.api.miniapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.libuke.evidence.api.dto.MiniLoginRequest;
import com.libuke.evidence.api.dto.MiniLoginResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.config.WechatMiniappProperties;
import com.libuke.evidence.domain.entity.Community;
import com.libuke.evidence.domain.entity.Role;
import com.libuke.evidence.domain.entity.UserRole;
import com.libuke.evidence.domain.entity.WxUser;
import com.libuke.evidence.domain.mapper.CommunityMapper;
import com.libuke.evidence.domain.mapper.RoleMapper;
import com.libuke.evidence.domain.mapper.UserRoleMapper;
import com.libuke.evidence.domain.mapper.WxUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/miniapp/v1/auth")
public class MiniAuthController {

    private static final TypeReference<List<List<BigDecimal>>> COMMUNITY_BOUNDARY_TYPE = new TypeReference<>() {
    };

    private final WechatMiniappProperties wechatMiniappProperties;
    private final WxUserMapper wxUserMapper;
    private final CommunityMapper communityMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final ObjectMapper objectMapper;

    @PostMapping("/login")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<MiniLoginResponse> login(@Valid @RequestBody MiniLoginRequest request) {
        String openid = resolveOpenid(request.getCode());
        WxUser user = wxUserMapper.selectOne(
            new LambdaQueryWrapper<WxUser>()
                .eq(WxUser::getOpenid, openid)
                .last("limit 1")
        );
        if (user == null) {
            user = new WxUser();
            user.setOpenid(openid);
            user.setAuthStatus("pending");
            user.setLastLoginAt(LocalDateTime.now());
            wxUserMapper.insert(user);
            ensureOrdinaryUserRole(user.getId());
        } else {
            user.setLastLoginAt(LocalDateTime.now());
            wxUserMapper.updateById(user);
            ensureOrdinaryUserRole(user.getId());
        }

        Community community = user.getCommunityId() == null ? null : communityMapper.selectById(user.getCommunityId());
        return ApiResponse.ok(MiniLoginResponse.builder()
            .openid(user.getOpenid())
            .userId(user.getId())
            .communityId(user.getCommunityId())
            .communityName(community == null ? null : community.getName())
            .authStatus(user.getAuthStatus())
            .center(toCommunityCenter(community))
            .boundary(community == null ? null : parseCommunityBoundary(community.getBoundaryJson()))
            .mapZoom(community == null ? null : community.getMapZoom())
            .build());
    }

    private List<BigDecimal> toCommunityCenter(Community community) {
        if (community == null || community.getCenterLng() == null || community.getCenterLat() == null) {
            return null;
        }
        return List.of(community.getCenterLng(), community.getCenterLat());
    }

    private List<List<BigDecimal>> parseCommunityBoundary(String boundaryJson) {
        if (!StringUtils.hasText(boundaryJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(boundaryJson, COMMUNITY_BOUNDARY_TYPE);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String resolveOpenid(String code) {
        if (!StringUtils.hasText(wechatMiniappProperties.getAppId())
            || !StringUtils.hasText(wechatMiniappProperties.getAppSecret())) {
            throw new BusinessException(500, "微信小程序 appId 或 appSecret 未配置");
        }
        URI uri = UriComponentsBuilder.fromUriString(wechatMiniappProperties.getCode2sessionUrl())
            .queryParam("appid", wechatMiniappProperties.getAppId())
            .queryParam("secret", wechatMiniappProperties.getAppSecret())
            .queryParam("js_code", code)
            .queryParam("grant_type", "authorization_code")
            .build(true)
            .toUri();
        String responseBody = buildRestClient()
            .get()
            .uri(uri)
            .retrieve()
            .body(String.class);
        JsonNode response = parseWechatResponse(responseBody);
        if (response == null || !StringUtils.hasText(response.path("openid").asText(""))) {
            throw new BusinessException(500, "微信登录失败：" + (response == null ? "empty response" : response.path("errmsg").asText("unknown")));
        }
        return response.path("openid").asText();
    }

    private JsonNode parseWechatResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new BusinessException(500, "微信登录响应解析失败");
        }
    }

    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(resolveTimeout(wechatMiniappProperties.getConnectTimeoutMillis(), 3000)));
        requestFactory.setReadTimeout(Duration.ofMillis(resolveTimeout(wechatMiniappProperties.getReadTimeoutMillis(), 5000)));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private long resolveTimeout(Integer value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private void ensureOrdinaryUserRole(Long userId) {
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "ordinary_user")
                .last("limit 1")
        );
        if (role == null) {
            role = new Role();
            role.setCode("ordinary_user");
            role.setName("普通用户");
            role.setRemark("小程序默认注册用户");
            role.setEnabled(true);
            roleMapper.insert(role);
        }
        UserRole exists = userRoleMapper.selectOne(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, role.getId())
                .last("limit 1")
        );
        if (exists == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }
}
