package com.libuke.evidence.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libuke.evidence.api.dto.BindCommunityRequest;
import com.libuke.evidence.api.dto.CommunityBindingResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.Community;
import com.libuke.evidence.domain.entity.InvitationCode;
import com.libuke.evidence.domain.entity.Role;
import com.libuke.evidence.domain.entity.UserRole;
import com.libuke.evidence.domain.entity.WxUser;
import com.libuke.evidence.domain.mapper.CommunityMapper;
import com.libuke.evidence.domain.mapper.InvitationCodeMapper;
import com.libuke.evidence.domain.mapper.RoleMapper;
import com.libuke.evidence.domain.mapper.UserRoleMapper;
import com.libuke.evidence.domain.mapper.WxUserMapper;
import com.libuke.evidence.domain.service.CommunityBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityBindingServiceImpl implements CommunityBindingService {

    private static final TypeReference<List<List<BigDecimal>>> COMMUNITY_BOUNDARY_TYPE = new TypeReference<>() {
    };

    private final InvitationCodeMapper invitationCodeMapper;
    private final CommunityMapper communityMapper;
    private final WxUserMapper wxUserMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityBindingResponse bindByInvitationCode(BindCommunityRequest request) {
        InvitationCode invitationCode = invitationCodeMapper.selectOne(
            new LambdaQueryWrapper<InvitationCode>()
                .eq(InvitationCode::getCode, request.getInvitationCode())
                .eq(InvitationCode::getEnabled, true)
                .last("limit 1")
        );
        if (invitationCode == null) {
            throw new BusinessException("小区邀请码无效");
        }
        if (invitationCode.getExpiresAt() != null && invitationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("小区邀请码已过期");
        }
        if (invitationCode.getMaxUsageCount() != null
            && invitationCode.getUsedCount() != null
            && invitationCode.getUsedCount() >= invitationCode.getMaxUsageCount()) {
            throw new BusinessException("小区邀请码使用次数已满");
        }

        Community community = communityMapper.selectById(invitationCode.getCommunityId());
        if (community == null || !Boolean.TRUE.equals(community.getEnabled())) {
            throw new BusinessException("小区暂不可用");
        }

        WxUser user = wxUserMapper.selectOne(
            new LambdaQueryWrapper<WxUser>()
                .eq(WxUser::getOpenid, request.getOpenid())
                .last("limit 1")
        );
        if (user == null) {
            user = new WxUser();
            user.setOpenid(request.getOpenid());
            user.setAuthStatus("verified");
            user.setLastLoginAt(LocalDateTime.now());
            user.setCommunityId(community.getId());
            user.setWitnessInfo(trimToNull(request.getWitnessInfo()));
            wxUserMapper.insert(user);
            ensureOrdinaryUserRole(user.getId());
        } else {
            user.setAuthStatus("verified");
            user.setCommunityId(community.getId());
            user.setWitnessInfo(trimToNull(request.getWitnessInfo()));
            user.setUpdatedAt(LocalDateTime.now());
            wxUserMapper.updateById(user);
            ensureOrdinaryUserRole(user.getId());
        }

        invitationCode.setUsedCount((invitationCode.getUsedCount() == null ? 0 : invitationCode.getUsedCount()) + 1);
        invitationCodeMapper.updateById(invitationCode);

        return CommunityBindingResponse.builder()
            .userId(user.getId())
            .communityId(community.getId())
            .communityName(community.getName())
            .authStatus(user.getAuthStatus())
            .center(toCommunityCenter(community))
            .boundary(parseCommunityBoundary(community.getBoundaryJson()))
            .mapZoom(community.getMapZoom())
            .build();
    }

    private List<BigDecimal> toCommunityCenter(Community community) {
        if (community.getCenterLng() == null || community.getCenterLat() == null) {
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
