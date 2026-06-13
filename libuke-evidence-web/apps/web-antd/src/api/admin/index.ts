import { useAppConfig } from '@vben/hooks';
import { useAccessStore } from '@vben/stores';

import { requestClient } from '#/api/request';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

export interface PageResponse<T> {
  pageNo: number;
  pageSize: number;
  records: T[];
  total: number;
}

export interface Attachment {
  clientUploadedAt?: string;
  id: string;
  imageHeight?: number;
  imageWidth?: number;
  objectKey: string;
  originalFileSize?: number;
  originalFileName?: string;
  originalMimeType?: string;
  serverReceivedAt?: string;
  sortOrder: number;
  type: string;
  url: string;
}

export interface DashboardData {
  categoryCounts: Record<string, number>;
  communityCount: number;
  reportCount: number;
  statusCounts: Record<string, number>;
  todayReportCount: number;
  userCount: number;
}

export interface DashboardCommunityMapData {
  boundary?: [number, number][];
  buildingColor?: string;
  center?: [number, number];
  communityId: string;
  communityName: string;
  pitch?: number;
  reportCount: number;
  rotation?: number;
  todayReportCount: number;
  zoom?: number;
}

export interface AnalyticsQuery {
  category?: string;
  communityId?: string;
  endDate?: string;
  startDate?: string;
  status?: string;
}

export interface AnalyticsNameCountItem {
  count: number;
  name: string;
}

export interface AnalyticsStatusCountItem {
  count: number;
  label: string;
  status: string;
}

export interface AnalyticsDailyTrendItem {
  date: string;
  resolvedCount: number;
  submittedCount: number;
}

export interface AnalyticsOverdueReportItem {
  communityName?: string;
  id: string;
  locationAddress?: string;
  reportNo: string;
  status: string;
  subCategory?: string;
  submittedAt: string;
}

export interface AnalyticsOverviewData {
  avgProcessHours: number;
  categoryDistribution: AnalyticsNameCountItem[];
  dailyTrend: AnalyticsDailyTrendItem[];
  overdueCount: number;
  overdueReports: AnalyticsOverdueReportItem[];
  pendingCount: number;
  processingCount: number;
  resolvedCount: number;
  statusDistribution: AnalyticsStatusCountItem[];
  subCategoryTop: AnalyticsNameCountItem[];
  totalCount: number;
}

export interface ReportRecord {
  adminNote?: string;
  attachments?: Attachment[];
  category: string;
  communityId: string;
  communityName?: string;
  createdAt: string;
  firstImageUrl?: string;
  id: string;
  imageCount: number;
  latitude?: number;
  locationAddress?: string;
  longitude?: number;
  openid?: string;
  remark?: string;
  reportNo: string;
  status: string;
  subCategory: string;
  submittedAt: string;
  updatedAt: string;
  userId: string;
  witnessInfo?: string;
}

export interface ReportMapPointRecord {
  category: string;
  communityId: string;
  firstImageUrl?: string;
  latitude: number;
  locationAddress?: string;
  longitude: number;
  reportId: string;
  reportNo: string;
  status: string;
  subCategory: string;
  submittedAt: string;
}

export interface ReportEventRecord {
  content?: string;
  createdAt: string;
  eventType: string;
  fromStatus?: string;
  id: string;
  operatorId?: string;
  operatorName?: string;
  operatorType: string;
  reportId: string;
  toStatus?: string;
}

export interface CommunityRecord {
  address?: string;
  boundary?: [number, number][];
  buildingColor?: string;
  center?: [number, number];
  communityName?: string;
  createdAt: string;
  enabled: boolean;
  id: string;
  mapPitch?: number;
  mapRotation?: number;
  mapZoom?: number;
  name: string;
  principalName?: string;
  principalPhone?: string;
  reportCount: number;
  status?: number;
  updatedAt: string;
  userCount: number;
}

export interface InvitationCodeRecord {
  code: string;
  communityId: string;
  communityName?: string;
  createdAt: string;
  enabled: boolean;
  expiresAt?: string;
  id: string;
  maxUsageCount?: number;
  updatedAt: string;
  usedCount: number;
}

export interface RoleRecord {
  code: string;
  createdAt: string;
  enabled: boolean;
  id: string;
  menuIds?: string[];
  name: string;
  remark?: string;
  updatedAt: string;
}

export interface MenuRecord {
  activeIcon?: string;
  activePath?: string;
  affixTab?: boolean;
  authCode?: string;
  badge?: string;
  badgeType?: string;
  badgeVariants?: string;
  children?: MenuRecord[];
  component?: string;
  createdAt?: string;
  enabled: boolean;
  hideChildrenInMenu?: boolean;
  hideInBreadcrumb?: boolean;
  hideInTab?: boolean;
  hidden?: boolean;
  icon?: string;
  id: string;
  keepAlive?: boolean;
  linkSrc?: string;
  meta?: Record<string, any>;
  name: string;
  parentId?: string;
  path?: string;
  permissionCode?: string;
  sortOrder?: number;
  status?: number;
  title: string;
  type: 'button' | 'catalog' | 'embedded' | 'link' | 'menu';
  updatedAt?: string;
}

export interface PlatformUserRecord {
  communityIds?: string[];
  communities?: CommunityRecord[];
  createdAt: string;
  displayName: string;
  enabled: boolean;
  id: string;
  lastLoginAt?: string;
  phone?: string;
  roleIds?: string[];
  roles?: RoleRecord[];
  superAdmin: boolean;
  updatedAt: string;
  username: string;
}

export interface ReportCategoryRecord {
  children?: ReportCategoryRecord[];
  code?: string;
  createdAt: string;
  enabled: boolean;
  id: string;
  name: string;
  parentId?: string;
  remark?: string;
  sortOrder: number;
  updatedAt: string;
}

export interface WatermarkTemplateRecord {
  backgroundColor: string;
  backgroundOpacity: number;
  contentTemplate: string;
  createdAt: string;
  enabled: boolean;
  fontSize?: number;
  id: string;
  name: string;
  opacity: number;
  position: string;
  systemTemplate?: boolean;
  textColor: string;
  updatedAt: string;
}

export interface WatermarkTaskRecord {
  createdAt: string;
  errorMessage?: string;
  id: string;
  imageId: string;
  reportId: string;
  reportNo?: string;
  retryCount: number;
  status: string;
  templateId?: string;
  templateName?: string;
  updatedAt: string;
}

export interface SystemConfigRecord {
  configGroup: string;
  configKey: string;
  configName: string;
  configValue?: string;
  createdAt: string;
  editable: boolean;
  encrypted: boolean;
  id: string;
  remark?: string;
  sensitive: boolean;
  updatedAt: string;
  valueType: string;
}

export interface RuntimeConfigRecord {
  map: {
    defaultLatitude?: number;
    defaultLongitude?: number;
    defaultZoom: number;
    jsApiKey?: string;
    jsApiSecurityKey?: string;
  };
  platform: Record<string, string>;
  reportImageUpload: {
    allowedMimeTypes: string;
    compressEnabled: boolean;
    maxFileCount: number;
    maxFileSizeMb: number;
  };
}

export interface StorageConfigRecord {
  accessKeyId?: string;
  accessKeySecret?: string;
  avatarDir?: string;
  bucketName?: string;
  enabled: boolean;
  endpoint?: string;
  id: string;
  name: string;
  originalDir?: string;
  presignedUrlMinutes?: number;
  provider: string;
  region?: string;
  remark?: string;
  systemConfig: boolean;
  updatedAt: string;
  uploadDir?: string;
  watermarkedDir?: string;
}

export interface MapConfigRecord {
  defaultLatitude?: number;
  defaultLongitude?: number;
  defaultZoom?: number;
  enabled: boolean;
  id: string;
  jsApiKey?: string;
  jsApiSecurityKey?: string;
  reverseGeocodeKey?: string;
  remark?: string;
  systemConfig: boolean;
  updatedAt: string;
}

export interface UploadPolicyRecord {
  allowedMimeTypes: string;
  compressEnabled: boolean;
  enabled: boolean;
  id: string;
  maxFileCount: number;
  maxFileSizeMb: number;
  name: string;
  remark?: string;
  scene: string;
  systemConfig: boolean;
  updatedAt: string;
}

export interface ExportTemplateRecord {
  enabled: boolean;
  fieldsJson: string;
  fileRetentionDays: number;
  id: string;
  includeOriginalLinks: boolean;
  includeWatermarkedLinks: boolean;
  name: string;
  remark?: string;
  scene: string;
  systemConfig: boolean;
  updatedAt: string;
}

export interface UserRecord {
  authStatus: string;
  avatarObjectKey?: string;
  avatarUrl?: string;
  communityId?: string;
  communityName?: string;
  createdAt: string;
  id: string;
  lastLoginAt?: string;
  nickname?: string;
  openid: string;
  updatedAt: string;
  witnessInfo?: string;
}

export interface AdminUploadResponse {
  objectKey: string;
  url: string;
}

export interface ReportQuery {
  category?: string;
  communityId?: string;
  endDate?: string;
  ids?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
  startDate?: string;
  status?: string;
}

export function getDashboardApi() {
  return requestClient.get<DashboardData>('/admin/v1/dashboard');
}

export function getDashboardMapCommunitiesApi() {
  return requestClient.get<CommunityRecord[]>(
    '/admin/v1/dashboard/map-communities',
  );
}

export function getDashboardCommunityMapApi(communityId?: string) {
  return requestClient.get<DashboardCommunityMapData>(
    '/admin/v1/dashboard/community-map',
    { params: { communityId } },
  );
}

export function getAnalyticsOverviewApi(params: AnalyticsQuery) {
  return requestClient.get<AnalyticsOverviewData>(
    '/admin/v1/analytics/overview',
    { params },
  );
}

export function getReportsApi(params: ReportQuery) {
  return requestClient.get<PageResponse<ReportRecord>>('/admin/v1/reports', {
    params,
  });
}

export function exportReportsApi(params: ReportQuery) {
  return requestClient.download<Blob>('/admin/v1/reports/export', {
    params,
  });
}

export function getReportDetailApi(reportId: string) {
  return requestClient.get<ReportRecord>(`/admin/v1/reports/${reportId}`);
}

export function getReportMapPointsApi(params: {
  communityId: string;
  limit?: number;
  status?: string;
}) {
  return requestClient.get<ReportMapPointRecord[]>(
    '/admin/v1/reports/map-points',
    { params },
  );
}

export function getReportEventsApi(reportId: string) {
  return requestClient.get<ReportEventRecord[]>(
    `/admin/v1/reports/${reportId}/events`,
  );
}

export function createReportStreamSource(communityId: string) {
  const accessStore = useAccessStore();
  const baseURL = apiURL.replace(/\/$/, '');
  const url = new URL(
    `${baseURL}/admin/v1/reports/stream`,
    window.location.origin,
  );
  url.searchParams.set('communityId', communityId);
  if (accessStore.accessToken) {
    url.searchParams.set('token', accessStore.accessToken);
  }
  return new EventSource(url.toString(), { withCredentials: true });
}

export function updateReportStatusApi(
  reportId: string,
  data: { adminNote?: string; status: string },
) {
  return requestClient.request<ReportRecord>(
    `/admin/v1/reports/${reportId}/status`,
    {
      data,
      method: 'PATCH',
    },
  );
}

export function deleteReportApi(reportId: string) {
  return requestClient.delete<void>(`/admin/v1/reports/${reportId}`);
}

export function getCommunitiesApi(params: {
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  return requestClient.get<PageResponse<CommunityRecord>>(
    '/admin/v1/communities',
    { params },
  );
}

export function createCommunityApi(data: {
  address?: string;
  boundary?: [number, number][];
  buildingColor?: string;
  center?: [number, number];
  mapPitch?: number;
  mapRotation?: number;
  mapZoom?: number;
  communityName?: string;
  enabled?: boolean;
  name?: string;
  principalName?: string;
  principalPhone?: string;
  status?: number;
}) {
  return requestClient.post<CommunityRecord>('/admin/v1/communities', data);
}

export function updateCommunityApi(
  communityId: string,
  data: {
    address?: string;
    boundary?: [number, number][];
    buildingColor?: string;
    center?: [number, number];
    mapPitch?: number;
    mapRotation?: number;
    mapZoom?: number;
    communityName?: string;
    enabled?: boolean;
    name?: string;
    principalName?: string;
    principalPhone?: string;
    status?: number;
  },
) {
  return requestClient.put<CommunityRecord>(
    `/admin/v1/communities/${communityId}`,
    data,
  );
}

export function deleteCommunityApi(communityId: string) {
  return requestClient.delete<void>(`/admin/v1/communities/${communityId}`);
}

export function getInvitationCodesApi(params: {
  communityId?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  return requestClient.get<PageResponse<InvitationCodeRecord>>(
    '/admin/v1/invitation-codes',
    { params },
  );
}

export function createInvitationCodeApi(data: {
  code: string;
  communityId: string;
  enabled?: boolean;
  expiresAt?: string;
  maxUsageCount?: number;
}) {
  return requestClient.post<InvitationCodeRecord>(
    '/admin/v1/invitation-codes',
    data,
  );
}

export function updateInvitationCodeApi(
  invitationCodeId: string,
  data: {
    code: string;
    communityId: string;
    enabled?: boolean;
    expiresAt?: string;
    maxUsageCount?: number;
  },
) {
  return requestClient.put<InvitationCodeRecord>(
    `/admin/v1/invitation-codes/${invitationCodeId}`,
    data,
  );
}

export function deleteInvitationCodeApi(invitationCodeId: string) {
  return requestClient.delete<void>(
    `/admin/v1/invitation-codes/${invitationCodeId}`,
  );
}

export function getRolesApi(params: {
  enabled?: boolean;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  return requestClient.get<PageResponse<RoleRecord>>('/admin/v1/roles', {
    params,
  });
}

export function createRoleApi(data: {
  code: string;
  enabled?: boolean;
  menuIds?: string[];
  name: string;
  remark?: string;
}) {
  return requestClient.post<RoleRecord>('/admin/v1/roles', data);
}

export function updateRoleApi(
  roleId: string,
  data: {
    code: string;
    enabled?: boolean;
    menuIds?: string[];
    name: string;
    remark?: string;
  },
) {
  return requestClient.put<RoleRecord>(`/admin/v1/roles/${roleId}`, data);
}

export function deleteRoleApi(roleId: string) {
  return requestClient.delete<void>(`/admin/v1/roles/${roleId}`);
}

export function getMenusApi(params?: { includeButtons?: boolean }) {
  return requestClient.get<MenuRecord[]>('/admin/v1/menus', { params });
}

export function createMenuApi(data: Partial<MenuRecord>) {
  return requestClient.post<MenuRecord>('/admin/v1/menus', data);
}

export function updateMenuApi(menuId: string, data: Partial<MenuRecord>) {
  return requestClient.put<MenuRecord>(`/admin/v1/menus/${menuId}`, data);
}

export function deleteMenuApi(menuId: string) {
  return requestClient.delete<void>(`/admin/v1/menus/${menuId}`);
}

export function getPlatformUsersApi(params: {
  enabled?: boolean;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  return requestClient.get<PageResponse<PlatformUserRecord>>(
    '/admin/v1/platform-users',
    { params },
  );
}

export function createPlatformUserApi(data: {
  communityIds?: string[];
  displayName: string;
  enabled?: boolean;
  password?: string;
  phone?: string;
  roleIds?: string[];
  superAdmin?: boolean;
  username: string;
}) {
  return requestClient.post<PlatformUserRecord>('/admin/v1/platform-users', data);
}

export function updatePlatformUserApi(
  userId: string,
  data: {
    communityIds?: string[];
    displayName: string;
    enabled?: boolean;
    password?: string;
    phone?: string;
    roleIds?: string[];
    superAdmin?: boolean;
    username: string;
  },
) {
  return requestClient.put<PlatformUserRecord>(
    `/admin/v1/platform-users/${userId}`,
    data,
  );
}

export function resetPlatformUserPasswordApi(
  userId: string,
  data: { password: string },
) {
  return requestClient.request<PlatformUserRecord>(
    `/admin/v1/platform-users/${userId}/password`,
    { data, method: 'PATCH' },
  );
}

export function deletePlatformUserApi(userId: string) {
  return requestClient.delete<void>(`/admin/v1/platform-users/${userId}`);
}

export function getReportCategoriesApi(params?: {
  enabled?: boolean;
  keyword?: string;
}) {
  return requestClient.get<ReportCategoryRecord[]>(
    '/admin/v1/report-categories',
    { params },
  );
}

export function createReportCategoryApi(data: {
  code?: string;
  enabled?: boolean;
  name: string;
  parentId?: string;
  remark?: string;
  sortOrder?: number;
}) {
  return requestClient.post<ReportCategoryRecord>(
    '/admin/v1/report-categories',
    data,
  );
}

export function updateReportCategoryApi(
  categoryId: string,
  data: {
    code?: string;
    enabled?: boolean;
    name: string;
    parentId?: string;
    remark?: string;
    sortOrder?: number;
  },
) {
  return requestClient.put<ReportCategoryRecord>(
    `/admin/v1/report-categories/${categoryId}`,
    data,
  );
}

export function deleteReportCategoryApi(categoryId: string) {
  return requestClient.delete<void>(
    `/admin/v1/report-categories/${categoryId}`,
  );
}

export function getWatermarkTemplatesApi() {
  return requestClient.get<WatermarkTemplateRecord[]>(
    '/admin/v1/watermarks/templates',
  );
}

export function createWatermarkTemplateApi(data: {
  backgroundColor?: string;
  backgroundOpacity?: number;
  contentTemplate: string;
  enabled?: boolean;
  fontSize?: number;
  name: string;
  opacity?: number;
  position?: string;
  textColor?: string;
}) {
  return requestClient.post<WatermarkTemplateRecord>(
    '/admin/v1/watermarks/templates',
    data,
  );
}

export function updateWatermarkTemplateApi(
  templateId: string,
  data: {
    backgroundColor?: string;
    backgroundOpacity?: number;
    contentTemplate: string;
    enabled?: boolean;
    fontSize?: number;
    name: string;
    opacity?: number;
    position?: string;
    textColor?: string;
  },
) {
  return requestClient.put<WatermarkTemplateRecord>(
    `/admin/v1/watermarks/templates/${templateId}`,
    data,
  );
}

export function deleteWatermarkTemplateApi(templateId: string) {
  return requestClient.delete<void>(
    `/admin/v1/watermarks/templates/${templateId}`,
  );
}

export function getWatermarkTasksApi(params: {
  pageNo?: number;
  pageSize?: number;
  status?: string;
}) {
  return requestClient.get<PageResponse<WatermarkTaskRecord>>(
    '/admin/v1/watermarks/tasks',
    { params },
  );
}

export function retryWatermarkTaskApi(taskId: string) {
  return requestClient.request<void>(
    `/admin/v1/watermarks/tasks/${taskId}/retry`,
    { method: 'PATCH' },
  );
}

export function getSystemConfigsApi(params?: {
  group?: string;
  keyword?: string;
}) {
  return requestClient.get<SystemConfigRecord[]>('/admin/v1/system-configs', {
    params,
  });
}

export function updateSystemConfigApi(
  configId: string,
  data: {
    configValue?: string;
    editable?: boolean;
    remark?: string;
  },
) {
  return requestClient.put<SystemConfigRecord>(
    `/admin/v1/system-configs/${configId}`,
    data,
  );
}

export function getRuntimeConfigApi() {
  return requestClient.get<RuntimeConfigRecord>('/open/v1/config');
}

export function getStorageConfigsApi() {
  return requestClient.get<StorageConfigRecord[]>(
    '/admin/v1/system-configs/storage',
  );
}

export function updateStorageConfigApi(
  configId: string,
  data: Partial<StorageConfigRecord>,
) {
  return requestClient.put<StorageConfigRecord>(
    `/admin/v1/system-configs/storage/${configId}`,
    data,
  );
}

export function getMapConfigsApi() {
  return requestClient.get<MapConfigRecord[]>('/admin/v1/system-configs/maps');
}

export function updateMapConfigApi(
  configId: string,
  data: Partial<MapConfigRecord>,
) {
  return requestClient.put<MapConfigRecord>(
    `/admin/v1/system-configs/maps/${configId}`,
    data,
  );
}

export function getUploadPoliciesApi() {
  return requestClient.get<UploadPolicyRecord[]>(
    '/admin/v1/system-configs/upload-policies',
  );
}

export function updateUploadPolicyApi(
  policyId: string,
  data: Partial<UploadPolicyRecord>,
) {
  return requestClient.put<UploadPolicyRecord>(
    `/admin/v1/system-configs/upload-policies/${policyId}`,
    data,
  );
}

export function getExportTemplatesApi() {
  return requestClient.get<ExportTemplateRecord[]>(
    '/admin/v1/system-configs/export-templates',
  );
}

export function updateExportTemplateApi(
  templateId: string,
  data: Partial<ExportTemplateRecord>,
) {
  return requestClient.put<ExportTemplateRecord>(
    `/admin/v1/system-configs/export-templates/${templateId}`,
    data,
  );
}

export function uploadAvatarApi(file: File) {
  return requestClient.upload<AdminUploadResponse>('/admin/v1/uploads/avatar', {
    file,
  });
}

export function getUsersApi(params: {
  communityId?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  return requestClient.get<PageResponse<UserRecord>>('/admin/v1/users', {
    params,
  });
}

export function updateUserApi(
  userId: string,
  data: {
    authStatus: string;
    avatarObjectKey?: string;
    communityId?: string;
    nickname?: string;
    witnessInfo?: string;
  },
) {
  return requestClient.put<UserRecord>(`/admin/v1/users/${userId}`, data);
}

export function deleteUserApi(userId: string) {
  return requestClient.delete<void>(`/admin/v1/users/${userId}`);
}
