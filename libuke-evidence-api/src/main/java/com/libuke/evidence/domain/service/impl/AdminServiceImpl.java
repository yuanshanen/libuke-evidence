package com.libuke.evidence.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libuke.evidence.api.admin.dto.AdminAnalyticsOverviewResponse;
import com.libuke.evidence.api.admin.dto.AdminAnalyticsRequest;
import com.libuke.evidence.api.admin.dto.AdminCommunityRequest;
import com.libuke.evidence.api.admin.dto.AdminCommunityResponse;
import com.libuke.evidence.api.admin.dto.AdminDashboardResponse;
import com.libuke.evidence.api.admin.dto.AdminInvitationCodeRequest;
import com.libuke.evidence.api.admin.dto.AdminInvitationCodeResponse;
import com.libuke.evidence.api.admin.dto.AdminLoginRequest;
import com.libuke.evidence.api.admin.dto.AdminLoginResponse;
import com.libuke.evidence.api.admin.dto.AdminMenuRequest;
import com.libuke.evidence.api.admin.dto.AdminMenuResponse;
import com.libuke.evidence.api.admin.dto.AdminPasswordUpdateRequest;
import com.libuke.evidence.api.admin.dto.AdminProfileUpdateRequest;
import com.libuke.evidence.api.admin.dto.AdminReportCategoryRequest;
import com.libuke.evidence.api.admin.dto.AdminReportCategoryResponse;
import com.libuke.evidence.api.admin.dto.AdminReportEventResponse;
import com.libuke.evidence.api.admin.dto.AdminReportExportRequest;
import com.libuke.evidence.api.admin.dto.AdminReportMapPointResponse;
import com.libuke.evidence.api.admin.dto.AdminReportResponse;
import com.libuke.evidence.api.admin.dto.AdminReportStatusRequest;
import com.libuke.evidence.api.admin.dto.AdminRoleRequest;
import com.libuke.evidence.api.admin.dto.AdminRoleResponse;
import com.libuke.evidence.api.admin.dto.AdminUserRequest;
import com.libuke.evidence.api.admin.dto.AdminUserResponse;
import com.libuke.evidence.api.admin.dto.AdminUserInfoResponse;
import com.libuke.evidence.api.admin.dto.DashboardCommunityMapResponse;
import com.libuke.evidence.api.admin.dto.PlatformUserRequest;
import com.libuke.evidence.api.admin.dto.PlatformUserResponse;
import com.libuke.evidence.api.admin.dto.ResetPasswordRequest;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.api.dto.ReportAttachmentResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.Community;
import com.libuke.evidence.domain.entity.InvitationCode;
import com.libuke.evidence.domain.entity.Menu;
import com.libuke.evidence.domain.entity.PlatformUser;
import com.libuke.evidence.domain.entity.PlatformUserCommunity;
import com.libuke.evidence.domain.entity.PlatformUserRole;
import com.libuke.evidence.domain.entity.Report;
import com.libuke.evidence.domain.entity.ReportEvent;
import com.libuke.evidence.domain.entity.ReportCategory;
import com.libuke.evidence.domain.entity.ReportImage;
import com.libuke.evidence.domain.entity.Role;
import com.libuke.evidence.domain.entity.RoleMenu;
import com.libuke.evidence.domain.entity.WxUser;
import com.libuke.evidence.domain.mapper.CommunityMapper;
import com.libuke.evidence.domain.mapper.InvitationCodeMapper;
import com.libuke.evidence.domain.mapper.MenuMapper;
import com.libuke.evidence.domain.mapper.PlatformUserCommunityMapper;
import com.libuke.evidence.domain.mapper.PlatformUserMapper;
import com.libuke.evidence.domain.mapper.PlatformUserRoleMapper;
import com.libuke.evidence.domain.mapper.ReportCategoryMapper;
import com.libuke.evidence.domain.mapper.ReportEventMapper;
import com.libuke.evidence.domain.mapper.ReportImageMapper;
import com.libuke.evidence.domain.mapper.ReportMapper;
import com.libuke.evidence.domain.mapper.RoleMapper;
import com.libuke.evidence.domain.mapper.RoleMenuMapper;
import com.libuke.evidence.domain.mapper.WxUserMapper;
import com.libuke.evidence.domain.service.AdminService;
import com.libuke.evidence.domain.service.ReportStreamService;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import com.libuke.evidence.integration.map.GeoCodingService;
import com.libuke.evidence.integration.oss.OssUploadService;
import com.libuke.evidence.security.AdminAuthContext;
import com.libuke.evidence.security.AdminPrincipal;
import com.libuke.evidence.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Set<String> REPORT_STATUSES = Set.of("pending", "assigned", "processing", "resolved", "closed", "invalid", "duplicate");
    private static final Set<String> UNFINISHED_REPORT_STATUSES = Set.of("pending", "assigned", "processing");
    private static final Set<String> FINISHED_REPORT_STATUSES = Set.of("resolved", "closed");
    private static final Set<String> USER_AUTH_STATUSES = Set.of("pending", "verified", "disabled", "admin");
    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";
    private static final int REPORT_EXPORT_LIMIT = 5000;
    private static final String[] REPORT_EXPORT_HEADERS = {
        "记录编号",
        "小区",
        "上报人",
        "问题大类",
        "问题小类",
        "位置",
        "经度",
        "纬度",
        "问题说明",
        "上报时间",
        "当前状态",
        "处理备注",
        "附件数量",
        "首图链接",
        "全部附件链接"
    };
    private static final TypeReference<List<List<BigDecimal>>> COMMUNITY_BOUNDARY_TYPE = new TypeReference<>() {};

    private final ReportMapper reportMapper;
    private final ReportImageMapper reportImageMapper;
    private final CommunityMapper communityMapper;
    private final InvitationCodeMapper invitationCodeMapper;
    private final WxUserMapper wxUserMapper;
    private final RoleMapper roleMapper;
    private final PlatformUserMapper platformUserMapper;
    private final PlatformUserRoleMapper platformUserRoleMapper;
    private final PlatformUserCommunityMapper platformUserCommunityMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final ReportCategoryMapper reportCategoryMapper;
    private final ReportEventMapper reportEventMapper;
    private final OssUploadService ossUploadService;
    private final GeoCodingService geoCodingService;
    private final RuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;
    private final AdminAuthContext authContext;
    private final PasswordHasher passwordHasher;
    private final ReportStreamService reportStreamService;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        PlatformUser user = platformUserMapper.selectOne(
            new LambdaQueryWrapper<PlatformUser>().eq(PlatformUser::getUsername, request.getUsername())
        );
        if (user == null || Boolean.FALSE.equals(user.getEnabled()) || !passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (!StringUtils.hasText(user.getAccessToken())) {
            user.setAccessToken(UUID.randomUUID().toString().replace("-", ""));
        }
        user.setLastLoginAt(LocalDateTime.now());
        platformUserMapper.updateById(user);
        return AdminLoginResponse.builder()
            .token(user.getAccessToken())
            .username(user.getUsername())
            .displayName(user.getDisplayName())
            .superAdmin(user.getSuperAdmin())
            .build();
    }

    @Override
    public AdminUserInfoResponse currentUserInfo() {
        AdminPrincipal principal = authContext.currentUser();
        PlatformUser user = platformUserMapper.selectById(principal.id());
        if (user == null) {
            throw new BusinessException("后台用户不存在");
        }
        return AdminUserInfoResponse.builder()
            .userId(principal.id())
            .username(principal.username())
            .realName(user.getDisplayName())
            .displayName(user.getDisplayName())
            .phone(user.getPhone())
            .avatar("")
            .desc("物业取证管理端")
            .homePath("/evidence/dashboard")
            .superAdmin(principal.superAdmin())
            .roles(principal.roleCodes())
            .permissions(principal.permissionCodes())
            .communities(loadAuthorizedCommunities(principal).stream().map(this::toCommunityResponse).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserInfoResponse updateCurrentProfile(AdminProfileUpdateRequest request) {
        AdminPrincipal principal = authContext.currentUser();
        PlatformUser user = platformUserMapper.selectById(principal.id());
        if (user == null) {
            throw new BusinessException("后台用户不存在");
        }
        user.setDisplayName(request.getDisplayName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        platformUserMapper.updateById(user);
        return currentUserInfo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentPassword(AdminPasswordUpdateRequest request) {
        AdminPrincipal principal = authContext.currentUser();
        PlatformUser user = platformUserMapper.selectById(principal.id());
        if (user == null) {
            throw new BusinessException("后台用户不存在");
        }
        if (!passwordHasher.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("当前密码不正确");
        }
        user.setPasswordHash(passwordHasher.hash(request.getNewPassword()));
        user.setAccessToken(UUID.randomUUID().toString().replace("-", ""));
        platformUserMapper.updateById(user);
    }

    @Override
    public List<AdminMenuResponse> currentMenus() {
        return buildMenuTree(loadAccessibleMenus(authContext.currentUser(), false), null);
    }

    @Override
    public AdminDashboardResponse dashboard() {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        applyReportScope(wrapper, null);
        List<Report> reports = reportMapper.selectList(wrapper);
        LocalDate today = LocalDate.now();
        Map<String, Long> categoryCounts = reports.stream()
            .collect(Collectors.groupingBy(Report::getCategory, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> statusCounts = reports.stream()
            .collect(Collectors.groupingBy(Report::getStatus, LinkedHashMap::new, Collectors.counting()));

        return AdminDashboardResponse.builder()
            .reportCount(reports.size())
            .todayReportCount(reports.stream().filter(report -> isSameDay(report.getSubmittedAt(), today)).count())
            .communityCount(countAuthorizedCommunities())
            .userCount(countAuthorizedWxUsers())
            .categoryCounts(categoryCounts)
            .statusCounts(statusCounts)
            .build();
    }

    @Override
    public List<AdminCommunityResponse> dashboardMapCommunities() {
        return loadAuthorizedCommunities(authContext.currentUser()).stream()
            .filter(community -> community.getCenterLng() != null && community.getCenterLat() != null && StringUtils.hasText(community.getBoundaryJson()))
            .map(this::toCommunityResponse)
            .toList();
    }

    @Override
    public DashboardCommunityMapResponse dashboardCommunityMap(Long communityId) {
        Community community;
        if (communityId != null) {
            ensureCommunityAccessible(communityId);
            community = communityMapper.selectById(communityId);
        } else {
            community = dashboardMapCommunities().stream()
                .findFirst()
                .map(item -> communityMapper.selectById(item.getId()))
                .orElse(null);
        }
        if (community == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        List<Report> reports = reportMapper.selectList(
            new LambdaQueryWrapper<Report>().eq(Report::getCommunityId, community.getId())
        );
        return DashboardCommunityMapResponse.builder()
            .communityId(community.getId())
            .communityName(community.getName())
            .center(toCommunityCenter(community))
            .boundary(parseCommunityBoundary(community.getBoundaryJson()))
            .buildingColor(community.getBuildingColor())
            .zoom(resolveMapZoom(community))
            .pitch(resolveMapPitch(community))
            .rotation(resolveMapRotation(community))
            .reportCount((long) reports.size())
            .todayReportCount(reports.stream().filter(report -> isSameDay(report.getSubmittedAt(), today)).count())
            .build();
    }

    @Override
    public AdminAnalyticsOverviewResponse analyticsOverview(AdminAnalyticsRequest request) {
        LocalDate endDate = request.getEndDate() == null ? LocalDate.now() : request.getEndDate();
        LocalDate startDate = request.getStartDate() == null ? endDate.minusDays(29) : request.getStartDate();
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        ensureCommunityAccessibleIfPresent(request.getCommunityId());

        List<Report> reports = reportMapper.selectList(
            buildReportQuery(null, request.getCommunityId(), request.getCategory(), request.getStatus(), startDate, endDate)
                .orderByDesc(Report::getSubmittedAt)
        );
        Map<Long, Community> communityMap = loadCommunityMap(
            reports.stream().map(Report::getCommunityId).filter(Objects::nonNull).distinct().toList()
        );
        Map<Long, LocalDateTime> finishedAtMap = loadReportFinishedAtMap(
            reports.stream().map(Report::getId).filter(Objects::nonNull).toList()
        );
        LocalDateTime overdueLine = LocalDateTime.now().minusHours(24);

        return AdminAnalyticsOverviewResponse.builder()
            .totalCount(reports.size())
            .pendingCount(reports.stream().filter(report -> "pending".equals(report.getStatus())).count())
            .processingCount(reports.stream().filter(report -> "processing".equals(report.getStatus())).count())
            .resolvedCount(reports.stream().filter(report -> FINISHED_REPORT_STATUSES.contains(report.getStatus())).count())
            .overdueCount(reports.stream().filter(report -> isOverdueReport(report, overdueLine)).count())
            .avgProcessHours(calculateAverageProcessHours(reports, finishedAtMap))
            .dailyTrend(buildDailyTrend(reports, finishedAtMap, startDate, endDate))
            .categoryDistribution(buildNameCountItems(reports, Report::getCategory, Integer.MAX_VALUE))
            .subCategoryTop(buildNameCountItems(reports, Report::getSubCategory, 10))
            .statusDistribution(buildStatusDistribution(reports))
            .overdueReports(buildOverdueReports(reports, communityMap, overdueLine))
            .build();
    }

    @Override
    public PageResponse<PlatformUserResponse> pagePlatformUsers(String keyword, Boolean enabled, long pageNo, long pageSize) {
        Page<PlatformUser> page = platformUserMapper.selectPage(
            Page.of(pageNo, limitPageSize(pageSize)),
            new LambdaQueryWrapper<PlatformUser>()
                .eq(enabled != null, PlatformUser::getEnabled, enabled)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(PlatformUser::getUsername, keyword)
                    .or()
                    .like(PlatformUser::getDisplayName, keyword)
                    .or()
                    .like(PlatformUser::getPhone, keyword))
                .orderByDesc(PlatformUser::getCreatedAt)
        );
        return PageResponse.<PlatformUserResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(this::toPlatformUserResponse).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformUserResponse createPlatformUser(PlatformUserRequest request) {
        ensurePlatformUsernameUnique(request.getUsername(), null);
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("请填写登录密码");
        }
        PlatformUser user = new PlatformUser();
        user.setUsername(request.getUsername().trim());
        user.setDisplayName(request.getDisplayName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setAccessToken(UUID.randomUUID().toString().replace("-", ""));
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        user.setSuperAdmin(Boolean.TRUE.equals(request.getSuperAdmin()));
        platformUserMapper.insert(user);
        updatePlatformUserRoles(user.getId(), request.getRoleIds());
        updatePlatformUserCommunities(user.getId(), request.getCommunityIds());
        return toPlatformUserResponse(platformUserMapper.selectById(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformUserResponse updatePlatformUser(Long userId, PlatformUserRequest request) {
        PlatformUser user = platformUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("后台用户不存在");
        }
        ensurePlatformUsernameUnique(request.getUsername(), userId);
        user.setUsername(request.getUsername().trim());
        user.setDisplayName(request.getDisplayName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordHasher.hash(request.getPassword()));
            user.setAccessToken(UUID.randomUUID().toString().replace("-", ""));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getSuperAdmin() != null) {
            user.setSuperAdmin(request.getSuperAdmin());
        }
        platformUserMapper.updateById(user);
        updatePlatformUserRoles(userId, request.getRoleIds());
        updatePlatformUserCommunities(userId, request.getCommunityIds());
        return toPlatformUserResponse(platformUserMapper.selectById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformUserResponse resetPlatformUserPassword(Long userId, ResetPasswordRequest request) {
        PlatformUser user = platformUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("后台用户不存在");
        }
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setAccessToken(UUID.randomUUID().toString().replace("-", ""));
        platformUserMapper.updateById(user);
        return toPlatformUserResponse(platformUserMapper.selectById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatformUser(Long userId) {
        platformUserRoleMapper.delete(new LambdaQueryWrapper<PlatformUserRole>().eq(PlatformUserRole::getUserId, userId));
        platformUserCommunityMapper.delete(new LambdaQueryWrapper<PlatformUserCommunity>().eq(PlatformUserCommunity::getUserId, userId));
        platformUserMapper.deleteById(userId);
    }

    @Override
    public List<AdminMenuResponse> listMenus(Boolean includeButtons) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSortOrder);
        if (!Boolean.TRUE.equals(includeButtons)) {
            wrapper.ne(Menu::getType, "button");
        }
        return buildMenuTree(menuMapper.selectList(wrapper), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMenuResponse createMenu(AdminMenuRequest request) {
        Menu menu = new Menu();
        fillMenu(menu, request);
        menuMapper.insert(menu);
        return toMenuResponse(menu, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMenuResponse updateMenu(Long menuId, AdminMenuRequest request) {
        Menu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        fillMenu(menu, request);
        menuMapper.updateById(menu);
        return toMenuResponse(menuMapper.selectById(menuId), List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, menuId));
        menuMapper.deleteById(menuId);
    }

    @Override
    public PageResponse<AdminRoleResponse> pageRoles(String keyword, Boolean enabled, long pageNo, long pageSize) {
        Page<Role> page = roleMapper.selectPage(
            Page.of(pageNo, limitPageSize(pageSize)),
            new LambdaQueryWrapper<Role>()
                .eq(enabled != null, Role::getEnabled, enabled)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(Role::getName, keyword)
                    .or()
                    .like(Role::getCode, keyword)
                    .or()
                    .like(Role::getRemark, keyword)
                )
                .orderByAsc(Role::getCode)
        );
        return PageResponse.<AdminRoleResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(this::toRoleResponse).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleResponse createRole(AdminRoleRequest request) {
        ensureRoleCodeUnique(request.getCode(), null);
        Role role = new Role();
        role.setCode(request.getCode().trim());
        role.setName(request.getName().trim());
        role.setRemark(trimToNull(request.getRemark()));
        role.setEnabled(request.getEnabled() == null || request.getEnabled());
        roleMapper.insert(role);
        updateRoleMenus(role.getId(), request.getMenuIds());
        return toRoleResponse(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleResponse updateRole(Long roleId, AdminRoleRequest request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        ensureRoleCodeUnique(request.getCode(), roleId);
        role.setCode(request.getCode().trim());
        role.setName(request.getName().trim());
        role.setRemark(trimToNull(request.getRemark()));
        if (request.getEnabled() != null) {
            role.setEnabled(request.getEnabled());
        }
        roleMapper.updateById(role);
        updateRoleMenus(roleId, request.getMenuIds());
        return toRoleResponse(roleMapper.selectById(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        Long usageCount = platformUserRoleMapper.selectCount(
            new LambdaQueryWrapper<PlatformUserRole>().eq(PlatformUserRole::getRoleId, roleId)
        );
        if (usageCount != null && usageCount > 0) {
            role.setEnabled(false);
            roleMapper.updateById(role);
            return;
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        roleMapper.deleteById(roleId);
    }

    @Override
    public List<AdminReportCategoryResponse> listReportCategories(String keyword, Boolean enabled) {
        List<ReportCategory> categories = reportCategoryMapper.selectList(
            new LambdaQueryWrapper<ReportCategory>()
                .eq(enabled != null, ReportCategory::getEnabled, enabled)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(ReportCategory::getName, keyword)
                    .or()
                    .like(ReportCategory::getCode, keyword)
                    .or()
                    .like(ReportCategory::getRemark, keyword)
                )
                .orderByAsc(ReportCategory::getParentId)
                .orderByAsc(ReportCategory::getSortOrder)
                .orderByAsc(ReportCategory::getCreatedAt)
        );
        if (StringUtils.hasText(keyword)) {
            return categories.stream().map(category -> toReportCategoryResponse(category, List.of())).toList();
        }
        return buildReportCategoryTree(categories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminReportCategoryResponse createReportCategory(AdminReportCategoryRequest request) {
        ensureReportCategoryParentValid(null, request.getParentId());
        ReportCategory category = new ReportCategory();
        category.setParentId(request.getParentId());
        category.setName(request.getName().trim());
        category.setCode(trimToNull(request.getCode()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setEnabled(request.getEnabled() == null || request.getEnabled());
        category.setRemark(trimToNull(request.getRemark()));
        reportCategoryMapper.insert(category);
        return toReportCategoryResponse(category, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminReportCategoryResponse updateReportCategory(Long categoryId, AdminReportCategoryRequest request) {
        ReportCategory category = reportCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("问题分类不存在");
        }
        ensureReportCategoryParentValid(categoryId, request.getParentId());
        category.setParentId(request.getParentId());
        category.setName(request.getName().trim());
        category.setCode(trimToNull(request.getCode()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        if (request.getEnabled() != null) {
            category.setEnabled(request.getEnabled());
        }
        category.setRemark(trimToNull(request.getRemark()));
        reportCategoryMapper.updateById(category);
        return toReportCategoryResponse(reportCategoryMapper.selectById(categoryId), List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReportCategory(Long categoryId) {
        ReportCategory category = reportCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("问题分类不存在");
        }
        Long childCount = reportCategoryMapper.selectCount(
            new LambdaQueryWrapper<ReportCategory>().eq(ReportCategory::getParentId, categoryId)
        );
        if (childCount != null && childCount > 0) {
            throw new BusinessException("请先删除该分类下的子类");
        }
        reportCategoryMapper.deleteById(categoryId);
    }

    @Override
    public PageResponse<AdminReportResponse> pageReports(
        String keyword,
        Long communityId,
        String category,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        long pageNo,
        long pageSize
    ) {
        ensureCommunityAccessibleIfPresent(communityId);
        Page<Report> page = reportMapper.selectPage(
            Page.of(pageNo, limitPageSize(pageSize)),
            buildReportQuery(keyword, communityId, category, status, startDate, endDate)
                .orderByDesc(Report::getSubmittedAt)
        );
        return PageResponse.<AdminReportResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(this::toAdminReportResponse).toList())
            .build();
    }

    @Override
    public void exportReports(AdminReportExportRequest request, OutputStream outputStream) throws IOException {
        Long communityId = request.getCommunityId();
        ensureCommunityAccessibleIfPresent(communityId);
        LambdaQueryWrapper<Report> wrapper = buildReportQuery(
            request.getKeyword(),
            communityId,
            request.getCategory(),
            request.getStatus(),
            request.getStartDate(),
            request.getEndDate()
        );
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            wrapper.in(Report::getId, request.getIds().stream().distinct().toList());
        } else {
            wrapper.last("LIMIT " + REPORT_EXPORT_LIMIT);
        }
        List<Report> reports = reportMapper.selectList(wrapper.orderByDesc(Report::getSubmittedAt));
        writeReportExportWorkbook(reports, outputStream);
        log.info(
            "导出问题记录，adminId={}, count={}, selectedOnly={}",
            authContext.currentUser().id(),
            reports.size(),
            request.getIds() != null && !request.getIds().isEmpty()
        );
    }

    @Override
    public AdminReportResponse getReport(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("记录不存在");
        }
        ensureCommunityAccessible(report.getCommunityId());
        return toAdminReportResponse(report);
    }

    @Override
    public List<AdminReportEventResponse> listReportEvents(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("记录不存在");
        }
        ensureCommunityAccessible(report.getCommunityId());
        return reportEventMapper.selectList(
                new LambdaQueryWrapper<ReportEvent>()
                    .eq(ReportEvent::getReportId, reportId)
                    .orderByAsc(ReportEvent::getCreatedAt)
                    .orderByAsc(ReportEvent::getId)
            )
            .stream()
            .map(this::toReportEventResponse)
            .toList();
    }

    @Override
    public List<AdminReportMapPointResponse> listReportMapPoints(Long communityId, String status, long limit) {
        ensureCommunityAccessible(communityId);
        String reportStatus = StringUtils.hasText(status) ? status.trim() : "pending";
        if (!REPORT_STATUSES.contains(reportStatus)) {
            throw new BusinessException("记录状态不正确");
        }
        List<Report> reports = reportMapper.selectList(
            applyReportScope(new LambdaQueryWrapper<Report>(), communityId)
                .eq(Report::getStatus, reportStatus)
                .isNotNull(Report::getLongitude)
                .isNotNull(Report::getLatitude)
                .orderByDesc(Report::getSubmittedAt)
                .last("LIMIT " + limitMapPointSize(limit))
        );
        Map<Long, ReportImage> firstImageMap = loadFirstImageMap(reports.stream().map(Report::getId).toList());
        return reports.stream()
            .map(report -> toReportMapPointResponse(report, firstImageMap.get(report.getId())))
            .toList();
    }

    @Override
    public SseEmitter subscribeReportStream(Long communityId) {
        ensureCommunityAccessible(communityId);
        return reportStreamService.subscribe(authContext.currentUser().id(), communityId);
    }

    /*
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminReportResponse removedOriginalEdit(Long reportId, Object request) {
        if (!REPORT_STATUSES.contains(request.getStatus())) {
            throw new BusinessException("记录状态不正确");
        }
        ensureCommunityExists(request.getCommunityId());
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("记录不存在");
        }
        report.setCommunityId(request.getCommunityId());
        report.setCategory(request.getCategory().trim());
        report.setSubCategory(request.getSubCategory().trim());
        report.setLongitude(request.getLongitude());
        report.setLatitude(request.getLatitude());
        report.setLocationAddress(trimToNull(request.getLocationAddress()));
        report.setRemark(trimToNull(request.getRemark()));
        report.setStatus(request.getStatus());
        report.setAdminNote(trimToNull(request.getAdminNote()));
        reportMapper.updateById(report);
        return getReport(reportId);
    }
    */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminReportResponse updateReportStatus(Long reportId, AdminReportStatusRequest request) {
        if (!REPORT_STATUSES.contains(request.getStatus())) {
            throw new BusinessException("记录状态不正确");
        }
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("记录不存在");
        }
        ensureCommunityAccessible(report.getCommunityId());
        String oldStatus = report.getStatus();
        String oldAdminNote = trimToNull(report.getAdminNote());
        String newAdminNote = trimToNull(request.getAdminNote());
        report.setStatus(request.getStatus());
        report.setAdminNote(newAdminNote);
        reportMapper.updateById(report);
        boolean statusChanged = !Objects.equals(oldStatus, report.getStatus());
        boolean noteChanged = !Objects.equals(oldAdminNote, newAdminNote);
        if (statusChanged || noteChanged) {
            createStatusChangedEvent(report, oldStatus, request, statusChanged);
        }
        log.info(
            "更新问题记录状态，reportId={}, oldStatus={}, newStatus={}, adminId={}",
            report.getId(),
            oldStatus,
            report.getStatus(),
            authContext.currentUser().id()
        );
        return getReport(reportId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long reportId) {
        if (!"true".equals(runtimeConfigService.configValue("allow_report_delete", "true"))) {
            throw new BusinessException("后台未开启记录删除");
        }
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("记录不存在");
        }
        ensureCommunityAccessible(report.getCommunityId());
        reportImageMapper.delete(new LambdaQueryWrapper<ReportImage>().eq(ReportImage::getReportId, reportId));
        reportEventMapper.delete(new LambdaQueryWrapper<ReportEvent>().eq(ReportEvent::getReportId, reportId));
        reportMapper.deleteById(reportId);
    }

    @Override
    public PageResponse<AdminCommunityResponse> pageCommunities(String keyword, long pageNo, long pageSize) {
        Page<Community> page = communityMapper.selectPage(
            Page.of(pageNo, limitPageSize(pageSize)),
            applyCommunityScope(new LambdaQueryWrapper<Community>())
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(Community::getName, keyword)
                    .or()
                    .like(Community::getAddress, keyword)
                )
                .orderByDesc(Community::getCreatedAt)
        );
        return PageResponse.<AdminCommunityResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(this::toCommunityResponse).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCommunityResponse createCommunity(AdminCommunityRequest request) {
        Community community = new Community();
        fillCommunity(community, request, true);
        communityMapper.insert(community);
        return toCommunityResponse(community);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCommunityResponse updateCommunity(Long communityId, AdminCommunityRequest request) {
        ensureCommunityAccessible(communityId);
        Community community = communityMapper.selectById(communityId);
        if (community == null) {
            throw new BusinessException("小区不存在");
        }
        fillCommunity(community, request, false);
        communityMapper.updateById(community);
        return toCommunityResponse(communityMapper.selectById(communityId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommunity(Long communityId) {
        ensureCommunityAccessible(communityId);
        Community community = communityMapper.selectById(communityId);
        if (community == null) {
            throw new BusinessException("小区不存在");
        }
        communityMapper.deleteById(communityId);
    }

    @Override
    public PageResponse<AdminInvitationCodeResponse> pageInvitationCodes(Long communityId, String keyword, long pageNo, long pageSize) {
        ensureCommunityAccessibleIfPresent(communityId);
        Page<InvitationCode> page = invitationCodeMapper.selectPage(
            Page.of(pageNo, limitPageSize(pageSize)),
            applyInvitationScope(new LambdaQueryWrapper<InvitationCode>(), communityId)
                .like(StringUtils.hasText(keyword), InvitationCode::getCode, keyword)
                .orderByDesc(InvitationCode::getCreatedAt)
        );
        Map<Long, Community> communityMap = loadCommunityMap(page.getRecords().stream().map(InvitationCode::getCommunityId).toList());
        return PageResponse.<AdminInvitationCodeResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(code -> toInvitationCodeResponse(code, communityMap)).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminInvitationCodeResponse createInvitationCode(AdminInvitationCodeRequest request) {
        ensureCommunityExists(request.getCommunityId());
        ensureCommunityAccessible(request.getCommunityId());
        ensureInvitationCodeUnique(request.getCode(), null);
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setCommunityId(request.getCommunityId());
        invitationCode.setCode(request.getCode().trim());
        invitationCode.setMaxUsageCount(request.getMaxUsageCount());
        invitationCode.setUsedCount(0);
        invitationCode.setExpiresAt(request.getExpiresAt());
        invitationCode.setEnabled(request.getEnabled() == null || request.getEnabled());
        invitationCodeMapper.insert(invitationCode);
        return toInvitationCodeResponse(invitationCode, Map.of(request.getCommunityId(), communityMapper.selectById(request.getCommunityId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminInvitationCodeResponse updateInvitationCode(Long invitationCodeId, AdminInvitationCodeRequest request) {
        InvitationCode invitationCode = invitationCodeMapper.selectById(invitationCodeId);
        if (invitationCode == null) {
            throw new BusinessException("邀请码不存在");
        }
        ensureCommunityExists(request.getCommunityId());
        ensureCommunityAccessible(invitationCode.getCommunityId());
        ensureCommunityAccessible(request.getCommunityId());
        ensureInvitationCodeUnique(request.getCode(), invitationCodeId);
        invitationCode.setCommunityId(request.getCommunityId());
        invitationCode.setCode(request.getCode().trim());
        invitationCode.setMaxUsageCount(request.getMaxUsageCount());
        invitationCode.setExpiresAt(request.getExpiresAt());
        if (request.getEnabled() != null) {
            invitationCode.setEnabled(request.getEnabled());
        }
        invitationCodeMapper.updateById(invitationCode);
        return toInvitationCodeResponse(invitationCodeMapper.selectById(invitationCodeId), Map.of(request.getCommunityId(), communityMapper.selectById(request.getCommunityId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvitationCode(Long invitationCodeId) {
        InvitationCode invitationCode = invitationCodeMapper.selectById(invitationCodeId);
        if (invitationCode == null) {
            throw new BusinessException("邀请码不存在");
        }
        ensureCommunityAccessible(invitationCode.getCommunityId());
        if (invitationCode.getUsedCount() != null && invitationCode.getUsedCount() > 0) {
            invitationCode.setEnabled(false);
            invitationCodeMapper.updateById(invitationCode);
            return;
        }
        invitationCodeMapper.deleteById(invitationCodeId);
    }

    @Override
    public PageResponse<AdminUserResponse> pageUsers(Long communityId, String keyword, long pageNo, long pageSize) {
        ensureCommunityAccessibleIfPresent(communityId);
        Page<WxUser> page = wxUserMapper.selectPage(
            Page.of(pageNo, limitPageSize(pageSize)),
            applyWxUserScope(new LambdaQueryWrapper<WxUser>(), communityId)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(WxUser::getOpenid, keyword)
                    .or()
                    .like(WxUser::getWitnessInfo, keyword)
                    .or()
                    .like(WxUser::getNickname, keyword)
                )
                .orderByDesc(WxUser::getCreatedAt)
        );
        Map<Long, Community> communityMap = loadCommunityMap(page.getRecords().stream().map(WxUser::getCommunityId).toList());
        return PageResponse.<AdminUserResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(user -> toUserResponse(user, communityMap)).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse updateUser(Long userId, AdminUserRequest request) {
        if (!USER_AUTH_STATUSES.contains(request.getAuthStatus())) {
            throw new BusinessException("用户状态不正确");
        }
        WxUser user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getCommunityId() != null) {
            ensureCommunityAccessible(user.getCommunityId());
        }
        if (request.getCommunityId() != null) {
            ensureCommunityExists(request.getCommunityId());
            ensureCommunityAccessible(request.getCommunityId());
        }
        if (request.getNickname() != null) {
            user.setNickname(trimToNull(request.getNickname()));
        }
        if (request.getAvatarObjectKey() != null) {
            user.setAvatarUrl(trimToNull(request.getAvatarObjectKey()));
        }
        user.setCommunityId(request.getCommunityId());
        user.setWitnessInfo(trimToNull(request.getWitnessInfo()));
        user.setAuthStatus(request.getAuthStatus());
        wxUserMapper.updateById(user);
        Map<Long, Community> communityMap = loadCommunityMap(
            user.getCommunityId() == null ? List.of() : List.of(user.getCommunityId())
        );
        return toUserResponse(wxUserMapper.selectById(userId), communityMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        WxUser user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getCommunityId() != null) {
            ensureCommunityAccessible(user.getCommunityId());
        }
        wxUserMapper.deleteById(userId);
    }

    private AdminReportResponse toAdminReportResponse(Report report) {
        Community community = communityMapper.selectById(report.getCommunityId());
        WxUser user = wxUserMapper.selectById(report.getUserId());
        List<ReportImage> images = reportImageMapper.selectList(
            new LambdaQueryWrapper<ReportImage>()
                .eq(ReportImage::getReportId, report.getId())
                .orderByAsc(ReportImage::getSortOrder)
        );
        List<ReportAttachmentResponse> attachments = images.stream()
            .map(image -> {
                String objectKey = resolveDisplayObjectKey(image);
                return ReportAttachmentResponse.builder()
                    .id(image.getId())
                    .objectKey(objectKey)
                    .url(ossUploadService.generateTemporaryUrl(objectKey))
                    .type("image")
                    .sortOrder(image.getSortOrder())
                    .originalFileSize(image.getOriginalFileSize())
                    .originalMimeType(image.getOriginalMimeType())
                    .originalFileName(image.getOriginalFileName())
                    .imageWidth(image.getImageWidth())
                    .imageHeight(image.getImageHeight())
                    .clientUploadedAt(image.getClientUploadedAt())
                    .serverReceivedAt(image.getServerReceivedAt())
                    .build();
            })
            .toList();
        String firstImageUrl = attachments.isEmpty() ? null : attachments.getFirst().getUrl();
        return AdminReportResponse.builder()
            .id(report.getId())
            .reportNo(report.getReportNo())
            .userId(report.getUserId())
            .openid(user == null ? null : user.getOpenid())
            .witnessInfo(user == null ? null : user.getWitnessInfo())
            .communityId(report.getCommunityId())
            .communityName(community == null ? null : community.getName())
            .category(report.getCategory())
            .subCategory(report.getSubCategory())
            .remark(report.getRemark())
            .status(report.getStatus())
            .adminNote(report.getAdminNote())
            .longitude(report.getLongitude())
            .latitude(report.getLatitude())
            .locationAddress(report.getLocationAddress())
            .firstImageUrl(firstImageUrl)
            .imageCount(images.size())
            .attachments(attachments)
            .submittedAt(report.getSubmittedAt())
            .createdAt(report.getCreatedAt())
            .updatedAt(report.getUpdatedAt())
            .build();
    }

    private void writeReportExportWorkbook(List<Report> reports, OutputStream outputStream) throws IOException {
        Map<Long, Community> communityMap = loadCommunityMap(reports.stream().map(Report::getCommunityId).toList());
        Map<Long, WxUser> userMap = loadWxUserMap(reports.stream().map(Report::getUserId).toList());
        Map<Long, List<ReportImage>> imageMap = loadReportImageMap(reports.stream().map(Report::getId).toList());
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("问题记录");
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            Row header = sheet.createRow(0);
            for (int i = 0; i < REPORT_EXPORT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(REPORT_EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            for (int i = 0; i < reports.size(); i++) {
                Report report = reports.get(i);
                Community community = communityMap.get(report.getCommunityId());
                WxUser user = userMap.get(report.getUserId());
                List<ReportImage> images = imageMap.getOrDefault(report.getId(), List.of());
                List<String> imageUrls = images.stream()
                    .map(this::resolveDisplayObjectKey)
                    .filter(StringUtils::hasText)
                    .map(ossUploadService::generateTemporaryUrl)
                    .toList();
                Row row = sheet.createRow(i + 1);
                writeExportRow(row, List.of(
                    value(report.getReportNo()),
                    value(community == null ? null : community.getName()),
                    value(user == null ? null : (StringUtils.hasText(user.getWitnessInfo()) ? user.getWitnessInfo() : user.getOpenid())),
                    value(report.getCategory()),
                    value(report.getSubCategory()),
                    value(report.getLocationAddress()),
                    value(report.getLongitude()),
                    value(report.getLatitude()),
                    value(report.getRemark()),
                    value(report.getSubmittedAt()),
                    statusLabel(report.getStatus()),
                    value(report.getAdminNote()),
                    String.valueOf(images.size()),
                    imageUrls.isEmpty() ? "" : imageUrls.getFirst(),
                    String.join("\n", imageUrls)
                ));
            }
            applyReportExportColumnWidths(sheet);
            workbook.write(outputStream);
            workbook.dispose();
        }
    }

    private CellStyle createExportHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void writeExportRow(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }

    private void applyReportExportColumnWidths(Sheet sheet) {
        int[] widths = {22, 18, 16, 16, 18, 34, 14, 14, 32, 20, 12, 32, 10, 48, 64};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private AdminReportMapPointResponse toReportMapPointResponse(Report report, ReportImage firstImage) {
        String firstImageUrl = null;
        if (firstImage != null) {
            String objectKey = resolveDisplayObjectKey(firstImage);
            firstImageUrl = StringUtils.hasText(objectKey) ? ossUploadService.generateTemporaryUrl(objectKey) : null;
        }
        return AdminReportMapPointResponse.builder()
            .reportId(report.getId())
            .reportNo(report.getReportNo())
            .communityId(report.getCommunityId())
            .category(report.getCategory())
            .subCategory(report.getSubCategory())
            .status(report.getStatus())
            .longitude(report.getLongitude())
            .latitude(report.getLatitude())
            .locationAddress(report.getLocationAddress())
            .firstImageUrl(firstImageUrl)
            .submittedAt(report.getSubmittedAt())
            .build();
    }

    private AdminReportEventResponse toReportEventResponse(ReportEvent event) {
        return AdminReportEventResponse.builder()
            .id(event.getId())
            .reportId(event.getReportId())
            .eventType(event.getEventType())
            .fromStatus(event.getFromStatus())
            .toStatus(event.getToStatus())
            .operatorType(event.getOperatorType())
            .operatorId(event.getOperatorId())
            .operatorName(event.getOperatorName())
            .content(event.getContent())
            .createdAt(event.getCreatedAt())
            .build();
    }

    private void createStatusChangedEvent(Report report, String oldStatus, AdminReportStatusRequest request, boolean statusChanged) {
        AdminPrincipal principal = authContext.currentUser();
        ReportEvent event = new ReportEvent();
        event.setReportId(report.getId());
        event.setEventType(statusChanged ? resolveStatusEventType(report.getStatus()) : "note_updated");
        event.setFromStatus(oldStatus);
        event.setToStatus(report.getStatus());
        event.setOperatorType("admin");
        event.setOperatorId(principal.id());
        event.setOperatorName(principal.displayName());
        event.setContent(trimToNull(request.getAdminNote()));
        event.setCreatedAt(LocalDateTime.now());
        reportEventMapper.insert(event);
    }

    private String resolveStatusEventType(String status) {
        return switch (status) {
            case "assigned" -> "assigned";
            case "resolved" -> "resolved";
            case "closed" -> "closed";
            case "invalid" -> "invalidated";
            case "duplicate" -> "duplicated";
            default -> "status_changed";
        };
    }

    private Map<Long, ReportImage> loadFirstImageMap(List<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        List<ReportImage> images = reportImageMapper.selectList(
            new LambdaQueryWrapper<ReportImage>()
                .in(ReportImage::getReportId, reportIds)
                .orderByAsc(ReportImage::getReportId)
                .orderByAsc(ReportImage::getSortOrder)
        );
        return images.stream()
            .collect(Collectors.toMap(
                ReportImage::getReportId,
                Function.identity(),
                (existing, ignored) -> existing,
                LinkedHashMap::new
            ));
    }

    private AdminCommunityResponse toCommunityResponse(Community community) {
        return AdminCommunityResponse.builder()
            .id(community.getId())
            .name(community.getName())
            .communityName(community.getName())
            .address(community.getAddress())
            .principalName(community.getPrincipalName())
            .principalPhone(community.getPrincipalPhone())
            .enabled(community.getEnabled())
            .status(Boolean.FALSE.equals(community.getEnabled()) ? 0 : 1)
            .center(toCommunityCenter(community))
            .boundary(parseCommunityBoundary(community.getBoundaryJson()))
            .buildingColor(community.getBuildingColor())
            .mapZoom(resolveMapZoom(community))
            .mapPitch(resolveMapPitch(community))
            .mapRotation(resolveMapRotation(community))
            .userCount(wxUserMapper.selectCount(new LambdaQueryWrapper<WxUser>().eq(WxUser::getCommunityId, community.getId())))
            .reportCount(reportMapper.selectCount(new LambdaQueryWrapper<Report>().eq(Report::getCommunityId, community.getId())))
            .createdAt(community.getCreatedAt())
            .updatedAt(community.getUpdatedAt())
            .build();
    }

    private void fillCommunity(Community community, AdminCommunityRequest request, boolean create) {
        String name = StringUtils.hasText(request.getCommunityName()) ? request.getCommunityName() : request.getName();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("请填写小区名称");
        }
        community.setName(name.trim());
        community.setPrincipalName(trimToNull(request.getPrincipalName()));
        community.setPrincipalPhone(trimToNull(request.getPrincipalPhone()));
        if (StringUtils.hasText(request.getBuildingColor())) {
            community.setBuildingColor(request.getBuildingColor().trim());
        } else if (create && !StringUtils.hasText(community.getBuildingColor())) {
            community.setBuildingColor("#ffcc00");
        }
        community.setMapZoom(resolveMapZoom(request.getMapZoom()));
        community.setMapPitch(resolveMapPitch(request.getMapPitch()));
        community.setMapRotation(resolveMapRotation(request.getMapRotation()));
        if (request.getStatus() != null) {
            community.setEnabled(request.getStatus() == 1);
        } else if (request.getEnabled() != null) {
            community.setEnabled(request.getEnabled());
        } else if (create) {
            community.setEnabled(true);
        }
        if (request.getCenter() != null) {
            if (request.getCenter().size() != 2) {
                throw new BusinessException("小区中心点格式不正确");
            }
            community.setCenterLng(request.getCenter().get(0));
            community.setCenterLat(request.getCenter().get(1));
        }
        String resolvedAddress = trimToNull(geoCodingService.reverseGeocode(community.getCenterLng(), community.getCenterLat()));
        community.setAddress(StringUtils.hasText(resolvedAddress) ? resolvedAddress : trimToNull(request.getAddress()));
        if (request.getBoundary() != null) {
            if (request.getBoundary().size() < 3 || request.getBoundary().stream().anyMatch(point -> point == null || point.size() != 2)) {
                throw new BusinessException("小区边界至少需要 3 个坐标点");
            }
            community.setBoundaryJson(writeCommunityBoundary(request.getBoundary()));
        }
    }

    private List<BigDecimal> toCommunityCenter(Community community) {
        if (community.getCenterLng() == null || community.getCenterLat() == null) {
            return null;
        }
        return List.of(community.getCenterLng(), community.getCenterLat());
    }

    private String writeCommunityBoundary(List<List<BigDecimal>> boundary) {
        try {
            return objectMapper.writeValueAsString(boundary);
        } catch (JsonProcessingException e) {
            throw new BusinessException("小区边界坐标保存失败");
        }
    }

    private Integer resolveMapZoom(Community community) {
        return resolveMapZoom(community.getMapZoom());
    }

    private Integer resolveMapZoom(Integer zoom) {
        if (zoom == null) {
            return runtimeConfigService.mapConfig().defaultZoom();
        }
        return Math.max(3, Math.min(20, zoom));
    }

    private Integer resolveMapPitch(Community community) {
        return resolveMapPitch(community.getMapPitch());
    }

    private Integer resolveMapPitch(Integer pitch) {
        if (pitch == null) {
            return 58;
        }
        return Math.max(0, Math.min(80, pitch));
    }

    private Integer resolveMapRotation(Community community) {
        return resolveMapRotation(community.getMapRotation());
    }

    private Integer resolveMapRotation(Integer rotation) {
        if (rotation == null) {
            return -18;
        }
        return Math.max(-180, Math.min(180, rotation));
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

    private AdminInvitationCodeResponse toInvitationCodeResponse(InvitationCode code, Map<Long, Community> communityMap) {
        Community community = communityMap.get(code.getCommunityId());
        return AdminInvitationCodeResponse.builder()
            .id(code.getId())
            .communityId(code.getCommunityId())
            .communityName(community == null ? null : community.getName())
            .code(code.getCode())
            .maxUsageCount(code.getMaxUsageCount())
            .usedCount(code.getUsedCount())
            .expiresAt(code.getExpiresAt())
            .enabled(code.getEnabled())
            .createdAt(code.getCreatedAt())
            .updatedAt(code.getUpdatedAt())
            .build();
    }

    private PlatformUserResponse toPlatformUserResponse(PlatformUser user) {
        List<Long> roleIds = platformUserRoleMapper.selectList(
            new LambdaQueryWrapper<PlatformUserRole>().eq(PlatformUserRole::getUserId, user.getId())
        ).stream().map(PlatformUserRole::getRoleId).toList();
        List<Long> communityIds = platformUserCommunityMapper.selectList(
            new LambdaQueryWrapper<PlatformUserCommunity>().eq(PlatformUserCommunity::getUserId, user.getId())
        ).stream().map(PlatformUserCommunity::getCommunityId).toList();
        Map<Long, Community> communityMap = loadCommunityMap(communityIds);
        List<AdminRoleResponse> roles = roleIds.isEmpty()
            ? List.of()
            : roleMapper.selectBatchIds(roleIds).stream().map(this::toRoleResponse).toList();
        return PlatformUserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .displayName(user.getDisplayName())
            .phone(user.getPhone())
            .enabled(user.getEnabled())
            .superAdmin(user.getSuperAdmin())
            .roleIds(roleIds)
            .roles(roles)
            .communityIds(communityIds)
            .communities(communityIds.stream().map(communityMap::get).filter(item -> item != null).map(this::toCommunityResponse).toList())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    private AdminMenuResponse toMenuResponse(Menu menu, List<AdminMenuResponse> children) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", menu.getTitle());
        if (StringUtils.hasText(menu.getIcon())) {
            meta.put("icon", menu.getIcon());
        }
        if (StringUtils.hasText(menu.getActiveIcon())) {
            meta.put("activeIcon", menu.getActiveIcon());
        }
        if (menu.getSortOrder() != null) {
            meta.put("order", menu.getSortOrder());
        }
        if (Boolean.TRUE.equals(menu.getHidden())) {
            meta.put("hideInMenu", true);
        }
        if (Boolean.TRUE.equals(menu.getAffixTab())) {
            meta.put("affixTab", true);
        }
        if (Boolean.TRUE.equals(menu.getKeepAlive())) {
            meta.put("keepAlive", true);
        }
        if (Boolean.TRUE.equals(menu.getHideChildrenInMenu())) {
            meta.put("hideChildrenInMenu", true);
        }
        if (Boolean.TRUE.equals(menu.getHideInBreadcrumb())) {
            meta.put("hideInBreadcrumb", true);
        }
        if (Boolean.TRUE.equals(menu.getHideInTab())) {
            meta.put("hideInTab", true);
        }
        if (StringUtils.hasText(menu.getPermissionCode())) {
            meta.put("authCode", menu.getPermissionCode());
        }
        if (StringUtils.hasText(menu.getBadgeType())) {
            meta.put("badgeType", menu.getBadgeType());
        }
        if (StringUtils.hasText(menu.getBadge())) {
            meta.put("badge", menu.getBadge());
        }
        if (StringUtils.hasText(menu.getBadgeVariants())) {
            meta.put("badgeVariants", menu.getBadgeVariants());
        }
        if ("link".equals(menu.getType()) && StringUtils.hasText(menu.getLinkSrc())) {
            meta.put("link", menu.getLinkSrc());
        }
        if ("embedded".equals(menu.getType()) && StringUtils.hasText(menu.getLinkSrc())) {
            meta.put("iframeSrc", menu.getLinkSrc());
        }
        return AdminMenuResponse.builder()
            .id(menu.getId())
            .parentId(menu.getParentId())
            .type(menu.getType())
            .name(menu.getName())
            .title(menu.getTitle())
            .path(menu.getPath())
            .activePath(menu.getActivePath())
            .component(menu.getComponent())
            .icon(menu.getIcon())
            .activeIcon(menu.getActiveIcon())
            .permissionCode(menu.getPermissionCode())
            .authCode(menu.getPermissionCode())
            .sortOrder(menu.getSortOrder())
            .enabled(menu.getEnabled())
            .status(Boolean.TRUE.equals(menu.getEnabled()) ? 1 : 0)
            .hidden(menu.getHidden())
            .affixTab(menu.getAffixTab())
            .keepAlive(menu.getKeepAlive())
            .hideChildrenInMenu(menu.getHideChildrenInMenu())
            .hideInBreadcrumb(menu.getHideInBreadcrumb())
            .hideInTab(menu.getHideInTab())
            .badgeType(menu.getBadgeType())
            .badge(menu.getBadge())
            .badgeVariants(menu.getBadgeVariants())
            .linkSrc(menu.getLinkSrc())
            .meta(meta)
            .children(children)
            .createdAt(menu.getCreatedAt())
            .updatedAt(menu.getUpdatedAt())
            .build();
    }

    private List<AdminMenuResponse> buildMenuTree(List<Menu> menus, Long parentId) {
        return menus.stream()
            .filter(menu -> parentId == null ? menu.getParentId() == null : parentId.equals(menu.getParentId()))
            .sorted(Comparator.comparing(menu -> menu.getSortOrder() == null ? 0 : menu.getSortOrder()))
            .map(menu -> toMenuResponse(menu, buildMenuTree(menus, menu.getId())))
            .toList();
    }

    private List<Menu> loadAccessibleMenus(AdminPrincipal principal, boolean includeButtons) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<Menu>()
            .eq(Menu::getEnabled, true)
            .orderByAsc(Menu::getSortOrder);
        if (!includeButtons) {
            wrapper.ne(Menu::getType, "button");
        }
        if (principal.superAdmin()) {
            return menuMapper.selectList(wrapper);
        }
        List<Long> assignedRoleIds = platformUserRoleMapper.selectList(
            new LambdaQueryWrapper<PlatformUserRole>().eq(PlatformUserRole::getUserId, principal.id())
        ).stream().map(PlatformUserRole::getRoleId).distinct().toList();
        List<Long> roleIds = assignedRoleIds.isEmpty()
            ? List.of()
            : roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getId, assignedRoleIds)
                .eq(Role::getEnabled, true))
                .stream().map(Role::getId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(
            new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds)
        ).stream().map(RoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        menuIds = includeAncestorMenuIds(menuIds);
        wrapper.in(Menu::getId, menuIds);
        return menuMapper.selectList(wrapper);
    }

    private List<Long> includeAncestorMenuIds(List<Long> menuIds) {
        Set<Long> result = menuIds.stream().collect(Collectors.toSet());
        List<Long> pending = new ArrayList<>(menuIds);
        while (!pending.isEmpty()) {
            List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>().in(Menu::getId, pending));
            pending = menus.stream()
                .map(Menu::getParentId)
                .filter(parentId -> parentId != null && result.add(parentId))
                .toList();
        }
        return new ArrayList<>(result);
    }

    private void fillMenu(Menu menu, AdminMenuRequest request) {
        menu.setParentId(request.getParentId());
        menu.setType(request.getType().trim());
        menu.setName(request.getName().trim());
        menu.setTitle(request.getTitle().trim());
        menu.setPath(trimToNull(request.getPath()));
        menu.setActivePath(trimToNull(request.getActivePath()));
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setActiveIcon(trimToNull(request.getActiveIcon()));
        menu.setPermissionCode(trimToNull(StringUtils.hasText(request.getPermissionCode()) ? request.getPermissionCode() : request.getAuthCode()));
        menu.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        menu.setEnabled(request.getEnabled() == null || request.getEnabled());
        menu.setHidden(Boolean.TRUE.equals(request.getHidden()));
        menu.setAffixTab(Boolean.TRUE.equals(request.getAffixTab()));
        menu.setKeepAlive(Boolean.TRUE.equals(request.getKeepAlive()));
        menu.setHideChildrenInMenu(Boolean.TRUE.equals(request.getHideChildrenInMenu()));
        menu.setHideInBreadcrumb(Boolean.TRUE.equals(request.getHideInBreadcrumb()));
        menu.setHideInTab(Boolean.TRUE.equals(request.getHideInTab()));
        menu.setBadgeType(trimToNull(request.getBadgeType()));
        menu.setBadge(trimToNull(request.getBadge()));
        menu.setBadgeVariants(trimToNull(request.getBadgeVariants()));
        menu.setLinkSrc(trimToNull(request.getLinkSrc()));
    }

    private List<Long> loadMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
            .stream().map(RoleMenu::getMenuId).toList();
    }

    private void updateRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null) {
            return;
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        menuIds.stream().distinct().forEach(menuId -> {
            RoleMenu relation = new RoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        });
    }

    private void updatePlatformUserRoles(Long userId, List<Long> roleIds) {
        platformUserRoleMapper.delete(new LambdaQueryWrapper<PlatformUserRole>().eq(PlatformUserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        List<Long> distinctRoleIds = roleIds.stream().distinct().toList();
        ensureAssignableRoles(distinctRoleIds);
        distinctRoleIds.forEach(roleId -> {
            PlatformUserRole relation = new PlatformUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            platformUserRoleMapper.insert(relation);
        });
    }

    private void updatePlatformUserCommunities(Long userId, List<Long> communityIds) {
        platformUserCommunityMapper.delete(new LambdaQueryWrapper<PlatformUserCommunity>().eq(PlatformUserCommunity::getUserId, userId));
        if (communityIds == null) {
            return;
        }
        communityIds.stream().distinct().forEach(communityId -> {
            ensureCommunityExists(communityId);
            PlatformUserCommunity relation = new PlatformUserCommunity();
            relation.setUserId(userId);
            relation.setCommunityId(communityId);
            platformUserCommunityMapper.insert(relation);
        });
    }

    private AdminUserResponse toUserResponse(WxUser user, Map<Long, Community> communityMap) {
        Community community = user.getCommunityId() == null ? null : communityMap.get(user.getCommunityId());
        return AdminUserResponse.builder()
            .id(user.getId())
            .openid(user.getOpenid())
            .nickname(user.getNickname())
            .avatarObjectKey(user.getAvatarUrl())
            .avatarUrl(resolveAvatarUrl(user.getAvatarUrl()))
            .communityId(user.getCommunityId())
            .communityName(community == null ? null : community.getName())
            .witnessInfo(user.getWitnessInfo())
            .authStatus(user.getAuthStatus())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    private AdminRoleResponse toRoleResponse(Role role) {
        return AdminRoleResponse.builder()
            .id(role.getId())
            .code(role.getCode())
            .name(role.getName())
            .remark(role.getRemark())
            .enabled(role.getEnabled())
            .menuIds(loadMenuIdsByRoleId(role.getId()))
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .build();
    }

    private List<AdminReportCategoryResponse> buildReportCategoryTree(List<ReportCategory> categories) {
        Map<Long, List<ReportCategory>> childrenMap = categories.stream()
            .filter(category -> category.getParentId() != null)
            .collect(Collectors.groupingBy(ReportCategory::getParentId, LinkedHashMap::new, Collectors.toList()));
        return categories.stream()
            .filter(category -> category.getParentId() == null)
            .map(category -> toReportCategoryResponse(
                category,
                childrenMap.getOrDefault(category.getId(), List.of()).stream()
                    .map(child -> toReportCategoryResponse(child, List.of()))
                    .toList()
            ))
            .toList();
    }

    private AdminReportCategoryResponse toReportCategoryResponse(
        ReportCategory category,
        List<AdminReportCategoryResponse> children
    ) {
        return AdminReportCategoryResponse.builder()
            .id(category.getId())
            .parentId(category.getParentId())
            .name(category.getName())
            .code(category.getCode())
            .sortOrder(category.getSortOrder())
            .enabled(category.getEnabled())
            .remark(category.getRemark())
            .children(children)
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }

    private void ensureAssignableRoles(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("存在无效角色");
        }
        boolean containsSuperAdminRole = roles.stream()
            .anyMatch(role -> SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(role.getCode()));
        if (containsSuperAdminRole) {
            throw new BusinessException("不能选择超级管理员角色");
        }
    }

    private Map<Long, Community> loadCommunityMap(List<Long> communityIds) {
        List<Long> ids = communityIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return communityMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(Community::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, WxUser> loadWxUserMap(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return wxUserMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(WxUser::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, List<ReportImage>> loadReportImageMap(List<Long> reportIds) {
        List<Long> ids = reportIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return reportImageMapper.selectList(
            new LambdaQueryWrapper<ReportImage>()
                .in(ReportImage::getReportId, ids)
                .orderByAsc(ReportImage::getReportId)
                .orderByAsc(ReportImage::getSortOrder)
        ).stream().collect(Collectors.groupingBy(ReportImage::getReportId));
    }

    private void ensureCommunityExists(Long communityId) {
        Community community = communityMapper.selectById(communityId);
        if (community == null) {
            throw new BusinessException("小区不存在");
        }
    }

    private void ensureInvitationCodeUnique(String code, Long ignoredId) {
        InvitationCode exists = invitationCodeMapper.selectOne(
            new LambdaQueryWrapper<InvitationCode>()
                .eq(InvitationCode::getCode, code.trim())
                .last("limit 1")
        );
        if (exists != null && !exists.getId().equals(ignoredId)) {
            throw new BusinessException("邀请码已存在");
        }
    }

    private void ensureRoleCodeUnique(String code, Long ignoredId) {
        Role exists = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, code.trim())
                .last("limit 1")
        );
        if (exists != null && !exists.getId().equals(ignoredId)) {
            throw new BusinessException("角色编码已存在");
        }
    }

    private void ensurePlatformUsernameUnique(String username, Long ignoredId) {
        PlatformUser exists = platformUserMapper.selectOne(
            new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, username.trim())
                .last("limit 1")
        );
        if (exists != null && !exists.getId().equals(ignoredId)) {
            throw new BusinessException("登录账号已存在");
        }
    }

    private LambdaQueryWrapper<Report> applyReportScope(LambdaQueryWrapper<Report> wrapper, Long communityId) {
        if (communityId != null) {
            return wrapper.eq(Report::getCommunityId, communityId);
        }
        AdminPrincipal principal = authContext.currentUser();
        if (principal.superAdmin()) {
            return wrapper;
        }
        return principal.communityIds().isEmpty()
            ? wrapper.eq(Report::getCommunityId, -1L)
            : wrapper.in(Report::getCommunityId, principal.communityIds());
    }

    private LambdaQueryWrapper<Report> buildReportQuery(
        String keyword,
        Long communityId,
        String category,
        String status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return applyReportScope(new LambdaQueryWrapper<Report>(), communityId)
            .eq(StringUtils.hasText(category), Report::getCategory, category)
            .eq(StringUtils.hasText(status), Report::getStatus, status)
            .ge(startDate != null, Report::getSubmittedAt, startDate == null ? null : startDate.atStartOfDay())
            .lt(endDate != null, Report::getSubmittedAt, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(Report::getReportNo, keyword)
                .or()
                .like(Report::getLocationAddress, keyword)
                .or()
                .like(Report::getRemark, keyword)
            );
    }

    private Map<Long, LocalDateTime> loadReportFinishedAtMap(List<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        List<ReportEvent> events = reportEventMapper.selectList(
            new LambdaQueryWrapper<ReportEvent>()
                .in(ReportEvent::getReportId, reportIds)
                .in(ReportEvent::getToStatus, FINISHED_REPORT_STATUSES)
                .orderByAsc(ReportEvent::getCreatedAt)
                .orderByAsc(ReportEvent::getId)
        );
        Map<Long, LocalDateTime> result = new LinkedHashMap<>();
        for (ReportEvent event : events) {
            if (event.getReportId() != null && event.getCreatedAt() != null) {
                result.putIfAbsent(event.getReportId(), event.getCreatedAt());
            }
        }
        return result;
    }

    private double calculateAverageProcessHours(List<Report> reports, Map<Long, LocalDateTime> finishedAtMap) {
        double totalHours = 0;
        int count = 0;
        for (Report report : reports) {
            if (!FINISHED_REPORT_STATUSES.contains(report.getStatus()) || report.getSubmittedAt() == null) {
                continue;
            }
            LocalDateTime finishedAt = finishedAtMap.getOrDefault(report.getId(), report.getUpdatedAt());
            if (finishedAt == null || finishedAt.isBefore(report.getSubmittedAt())) {
                continue;
            }
            totalHours += Duration.between(report.getSubmittedAt(), finishedAt).toMinutes() / 60.0;
            count++;
        }
        if (count == 0) {
            return 0;
        }
        return Math.round(totalHours / count * 10) / 10.0;
    }

    private List<AdminAnalyticsOverviewResponse.DailyTrendItem> buildDailyTrend(
        List<Report> reports,
        Map<Long, LocalDateTime> finishedAtMap,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Map<LocalDate, long[]> dateMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(date, new long[] {0, 0});
        }
        for (Report report : reports) {
            if (report.getSubmittedAt() != null) {
                long[] counts = dateMap.get(report.getSubmittedAt().toLocalDate());
                if (counts != null) {
                    counts[0]++;
                }
            }
            LocalDateTime finishedAt = finishedAtMap.get(report.getId());
            if (finishedAt != null) {
                long[] counts = dateMap.get(finishedAt.toLocalDate());
                if (counts != null) {
                    counts[1]++;
                }
            }
        }
        return dateMap.entrySet().stream()
            .map(entry -> AdminAnalyticsOverviewResponse.DailyTrendItem.builder()
                .date(entry.getKey())
                .submittedCount(entry.getValue()[0])
                .resolvedCount(entry.getValue()[1])
                .build())
            .toList();
    }

    private List<AdminAnalyticsOverviewResponse.NameCountItem> buildNameCountItems(
        List<Report> reports,
        Function<Report, String> nameGetter,
        int limit
    ) {
        return reports.stream()
            .collect(Collectors.groupingBy(
                report -> StringUtils.hasText(nameGetter.apply(report)) ? nameGetter.apply(report) : "未分类",
                Collectors.counting()
            ))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> AdminAnalyticsOverviewResponse.NameCountItem.builder()
                .name(entry.getKey())
                .count(entry.getValue())
                .build())
            .toList();
    }

    private List<AdminAnalyticsOverviewResponse.StatusCountItem> buildStatusDistribution(List<Report> reports) {
        Map<String, Long> statusCounts = reports.stream()
            .collect(Collectors.groupingBy(Report::getStatus, Collectors.counting()));
        return REPORT_STATUSES.stream()
            .map(status -> AdminAnalyticsOverviewResponse.StatusCountItem.builder()
                .status(status)
                .label(statusLabel(status))
                .count(statusCounts.getOrDefault(status, 0L))
                .build())
            .toList();
    }

    private List<AdminAnalyticsOverviewResponse.OverdueReportItem> buildOverdueReports(
        List<Report> reports,
        Map<Long, Community> communityMap,
        LocalDateTime overdueLine
    ) {
        return reports.stream()
            .filter(report -> isOverdueReport(report, overdueLine))
            .sorted(Comparator.comparing(Report::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(10)
            .map(report -> {
                Community community = communityMap.get(report.getCommunityId());
                return AdminAnalyticsOverviewResponse.OverdueReportItem.builder()
                    .id(report.getId())
                    .reportNo(report.getReportNo())
                    .communityName(community == null ? "" : community.getName())
                    .subCategory(report.getSubCategory())
                    .locationAddress(report.getLocationAddress())
                    .status(report.getStatus())
                    .submittedAt(report.getSubmittedAt())
                    .build();
            })
            .toList();
    }

    private boolean isOverdueReport(Report report, LocalDateTime overdueLine) {
        return UNFINISHED_REPORT_STATUSES.contains(report.getStatus())
            && report.getSubmittedAt() != null
            && report.getSubmittedAt().isBefore(overdueLine);
    }

    private LambdaQueryWrapper<Community> applyCommunityScope(LambdaQueryWrapper<Community> wrapper) {
        AdminPrincipal principal = authContext.currentUser();
        if (principal.superAdmin()) {
            return wrapper;
        }
        return principal.communityIds().isEmpty()
            ? wrapper.eq(Community::getId, -1L)
            : wrapper.in(Community::getId, principal.communityIds());
    }

    private LambdaQueryWrapper<InvitationCode> applyInvitationScope(LambdaQueryWrapper<InvitationCode> wrapper, Long communityId) {
        if (communityId != null) {
            return wrapper.eq(InvitationCode::getCommunityId, communityId);
        }
        AdminPrincipal principal = authContext.currentUser();
        if (principal.superAdmin()) {
            return wrapper;
        }
        return principal.communityIds().isEmpty()
            ? wrapper.eq(InvitationCode::getCommunityId, -1L)
            : wrapper.in(InvitationCode::getCommunityId, principal.communityIds());
    }

    private LambdaQueryWrapper<WxUser> applyWxUserScope(LambdaQueryWrapper<WxUser> wrapper, Long communityId) {
        if (communityId != null) {
            return wrapper.eq(WxUser::getCommunityId, communityId);
        }
        AdminPrincipal principal = authContext.currentUser();
        if (principal.superAdmin()) {
            return wrapper;
        }
        return principal.communityIds().isEmpty()
            ? wrapper.eq(WxUser::getCommunityId, -1L)
            : wrapper.in(WxUser::getCommunityId, principal.communityIds());
    }

    private void ensureCommunityAccessibleIfPresent(Long communityId) {
        if (communityId != null) {
            ensureCommunityAccessible(communityId);
        }
    }

    private void ensureCommunityAccessible(Long communityId) {
        AdminPrincipal principal = authContext.currentUser();
        if (principal.superAdmin()) {
            return;
        }
        if (communityId == null || !principal.communityIds().contains(communityId)) {
            throw new BusinessException(403, "无权访问该小区数据");
        }
    }

    private List<Community> loadAuthorizedCommunities(AdminPrincipal principal) {
        LambdaQueryWrapper<Community> wrapper = new LambdaQueryWrapper<Community>()
            .eq(Community::getEnabled, true)
            .orderByDesc(Community::getCreatedAt);
        if (principal.superAdmin()) {
            return communityMapper.selectList(wrapper);
        }
        if (principal.communityIds().isEmpty()) {
            return List.of();
        }
        return communityMapper.selectList(wrapper.in(Community::getId, principal.communityIds()));
    }

    private long countAuthorizedCommunities() {
        AdminPrincipal principal = authContext.currentUser();
        if (principal.superAdmin()) {
            return communityMapper.selectCount(new LambdaQueryWrapper<Community>());
        }
        return principal.communityIds().size();
    }

    private long countAuthorizedWxUsers() {
        AdminPrincipal principal = authContext.currentUser();
        LambdaQueryWrapper<WxUser> wrapper = new LambdaQueryWrapper<>();
        if (!principal.superAdmin()) {
            if (principal.communityIds().isEmpty()) {
                wrapper.eq(WxUser::getCommunityId, -1L);
            } else {
                wrapper.in(WxUser::getCommunityId, principal.communityIds());
            }
        }
        return wxUserMapper.selectCount(wrapper);
    }

    private String resolveDisplayObjectKey(ReportImage image) {
        return image.getOriginalObjectKey();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "pending" -> "待处理";
            case "assigned" -> "已派单";
            case "processing" -> "处理中";
            case "resolved" -> "已处理";
            case "closed" -> "已关闭";
            case "invalid" -> "无效";
            case "duplicate" -> "重复";
            default -> value(status);
        };
    }

    private void ensureReportCategoryParentValid(Long categoryId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(categoryId)) {
            throw new BusinessException("父级分类不能选择自身");
        }
        ReportCategory parent = reportCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("父级分类不存在");
        }
        if (parent.getParentId() != null) {
            throw new BusinessException("暂仅支持大类和小类两级分类");
        }
    }

    private String resolveAvatarUrl(String avatarObjectKey) {
        if (!StringUtils.hasText(avatarObjectKey)) {
            return null;
        }
        if (avatarObjectKey.startsWith("http://") || avatarObjectKey.startsWith("https://")) {
            return avatarObjectKey;
        }
        return ossUploadService.generateTemporaryUrl(avatarObjectKey);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long limitPageSize(long pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private long limitMapPointSize(long limit) {
        if (limit <= 0) {
            return 200;
        }
        return Math.min(limit, 500);
    }

    private boolean isSameDay(LocalDateTime dateTime, LocalDate date) {
        return dateTime != null && dateTime.toLocalDate().equals(date);
    }
}
