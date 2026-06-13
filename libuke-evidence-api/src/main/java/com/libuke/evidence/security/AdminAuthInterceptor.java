package com.libuke.evidence.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.Menu;
import com.libuke.evidence.domain.entity.PlatformUser;
import com.libuke.evidence.domain.entity.PlatformUserCommunity;
import com.libuke.evidence.domain.entity.PlatformUserRole;
import com.libuke.evidence.domain.entity.Role;
import com.libuke.evidence.domain.entity.RoleMenu;
import com.libuke.evidence.domain.mapper.MenuMapper;
import com.libuke.evidence.domain.mapper.PlatformUserCommunityMapper;
import com.libuke.evidence.domain.mapper.PlatformUserMapper;
import com.libuke.evidence.domain.mapper.PlatformUserRoleMapper;
import com.libuke.evidence.domain.mapper.RoleMapper;
import com.libuke.evidence.domain.mapper.RoleMenuMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminAuthContext authContext;
    private final PlatformUserMapper platformUserMapper;
    private final PlatformUserRoleMapper platformUserRoleMapper;
    private final PlatformUserCommunityMapper platformUserCommunityMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = extractToken(request.getHeader("Authorization"));
        if (token == null && request.getRequestURI().endsWith("/admin/v1/reports/stream")) {
            token = extractToken(request.getParameter("token"));
        }
        if (token == null) {
            throw new BusinessException(401, "请先登录");
        }
        PlatformUser user = platformUserMapper.selectOne(
            new LambdaQueryWrapper<PlatformUser>().eq(PlatformUser::getAccessToken, token)
        );
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(401, "登录已失效");
        }
        List<Long> communityIds = platformUserCommunityMapper.selectList(
            new LambdaQueryWrapper<PlatformUserCommunity>().eq(PlatformUserCommunity::getUserId, user.getId())
        ).stream().map(PlatformUserCommunity::getCommunityId).distinct().toList();
        List<Long> roleIds = platformUserRoleMapper.selectList(
            new LambdaQueryWrapper<PlatformUserRole>().eq(PlatformUserRole::getUserId, user.getId())
        ).stream().map(PlatformUserRole::getRoleId).distinct().toList();
        List<Role> roles = roleIds.isEmpty()
            ? List.of()
            : roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getId, roleIds)
                .eq(Role::getEnabled, true));
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();
        List<Long> enabledRoleIds = roles.stream().map(Role::getId).toList();
        List<String> permissionCodes = loadPermissionCodes(user, enabledRoleIds);
        authContext.set(new AdminPrincipal(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            Boolean.TRUE.equals(user.getSuperAdmin()),
            communityIds,
            roleCodes,
            permissionCodes
        ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        authContext.clear();
    }

    private List<String> loadPermissionCodes(PlatformUser user, List<Long> roleIds) {
        if (Boolean.TRUE.equals(user.getSuperAdmin())) {
            return menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getEnabled, true)
                .isNotNull(Menu::getPermissionCode))
                .stream().map(Menu::getPermissionCode).filter(code -> !code.isBlank()).distinct().toList();
        }
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(
            new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds)
        ).stream().map(RoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<Menu>()
            .in(Menu::getId, menuIds)
            .eq(Menu::getEnabled, true)
            .isNotNull(Menu::getPermissionCode))
            .stream().map(Menu::getPermissionCode).filter(code -> !code.isBlank()).distinct().toList();
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
