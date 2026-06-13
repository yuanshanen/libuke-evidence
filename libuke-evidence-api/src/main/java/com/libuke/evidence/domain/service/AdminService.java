package com.libuke.evidence.domain.service;

import com.libuke.evidence.api.admin.dto.AdminCommunityRequest;
import com.libuke.evidence.api.admin.dto.AdminCommunityResponse;
import com.libuke.evidence.api.admin.dto.AdminAnalyticsOverviewResponse;
import com.libuke.evidence.api.admin.dto.AdminAnalyticsRequest;
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
import com.libuke.evidence.api.admin.dto.AdminRoleRequest;
import com.libuke.evidence.api.admin.dto.AdminRoleResponse;
import com.libuke.evidence.api.admin.dto.AdminReportResponse;
import com.libuke.evidence.api.admin.dto.AdminReportStatusRequest;
import com.libuke.evidence.api.admin.dto.AdminUserRequest;
import com.libuke.evidence.api.admin.dto.AdminUserResponse;
import com.libuke.evidence.api.admin.dto.AdminUserInfoResponse;
import com.libuke.evidence.api.admin.dto.DashboardCommunityMapResponse;
import com.libuke.evidence.api.admin.dto.PlatformUserRequest;
import com.libuke.evidence.api.admin.dto.PlatformUserResponse;
import com.libuke.evidence.api.admin.dto.ResetPasswordRequest;
import com.libuke.evidence.api.dto.PageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    AdminLoginResponse login(AdminLoginRequest request);

    AdminUserInfoResponse currentUserInfo();

    AdminUserInfoResponse updateCurrentProfile(AdminProfileUpdateRequest request);

    void updateCurrentPassword(AdminPasswordUpdateRequest request);

    List<AdminMenuResponse> currentMenus();

    AdminDashboardResponse dashboard();

    List<AdminCommunityResponse> dashboardMapCommunities();

    DashboardCommunityMapResponse dashboardCommunityMap(Long communityId);

    AdminAnalyticsOverviewResponse analyticsOverview(AdminAnalyticsRequest request);

    PageResponse<PlatformUserResponse> pagePlatformUsers(String keyword, Boolean enabled, long pageNo, long pageSize);

    PlatformUserResponse createPlatformUser(PlatformUserRequest request);

    PlatformUserResponse updatePlatformUser(Long userId, PlatformUserRequest request);

    PlatformUserResponse resetPlatformUserPassword(Long userId, ResetPasswordRequest request);

    void deletePlatformUser(Long userId);

    List<AdminMenuResponse> listMenus(Boolean includeButtons);

    AdminMenuResponse createMenu(AdminMenuRequest request);

    AdminMenuResponse updateMenu(Long menuId, AdminMenuRequest request);

    void deleteMenu(Long menuId);

    PageResponse<AdminRoleResponse> pageRoles(String keyword, Boolean enabled, long pageNo, long pageSize);

    AdminRoleResponse createRole(AdminRoleRequest request);

    AdminRoleResponse updateRole(Long roleId, AdminRoleRequest request);

    void deleteRole(Long roleId);

    List<AdminReportCategoryResponse> listReportCategories(String keyword, Boolean enabled);

    AdminReportCategoryResponse createReportCategory(AdminReportCategoryRequest request);

    AdminReportCategoryResponse updateReportCategory(Long categoryId, AdminReportCategoryRequest request);

    void deleteReportCategory(Long categoryId);

    PageResponse<AdminReportResponse> pageReports(
        String keyword,
        Long communityId,
        String category,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        long pageNo,
        long pageSize
    );

    AdminReportResponse getReport(Long reportId);

    List<AdminReportEventResponse> listReportEvents(Long reportId);

    List<AdminReportMapPointResponse> listReportMapPoints(Long communityId, String status, long limit);

    void exportReports(AdminReportExportRequest request, OutputStream outputStream) throws IOException;

    SseEmitter subscribeReportStream(Long communityId);

    AdminReportResponse updateReportStatus(Long reportId, AdminReportStatusRequest request);

    void deleteReport(Long reportId);

    PageResponse<AdminCommunityResponse> pageCommunities(String keyword, long pageNo, long pageSize);

    AdminCommunityResponse createCommunity(AdminCommunityRequest request);

    AdminCommunityResponse updateCommunity(Long communityId, AdminCommunityRequest request);

    void deleteCommunity(Long communityId);

    PageResponse<AdminInvitationCodeResponse> pageInvitationCodes(Long communityId, String keyword, long pageNo, long pageSize);

    AdminInvitationCodeResponse createInvitationCode(AdminInvitationCodeRequest request);

    AdminInvitationCodeResponse updateInvitationCode(Long invitationCodeId, AdminInvitationCodeRequest request);

    void deleteInvitationCode(Long invitationCodeId);

    PageResponse<AdminUserResponse> pageUsers(Long communityId, String keyword, long pageNo, long pageSize);

    AdminUserResponse updateUser(Long userId, AdminUserRequest request);

    void deleteUser(Long userId);
}
