import type { UserInfo } from '@vben/types';

import { requestClient } from '#/api/request';

export interface AdminUserInfoResult extends UserInfo {
  displayName?: string;
  permissions?: string[];
  phone?: string;
  superAdmin?: boolean;
}

/**
 * 获取当前后台用户信息
 */
export async function getUserInfoApi() {
  const userInfo = await requestClient.get<AdminUserInfoResult>(
    '/admin/v1/auth/user-info',
  );

  return {
    ...userInfo,
    realName: userInfo.realName || userInfo.displayName || userInfo.username,
    userId: String(userInfo.userId),
  };
}

/**
 * 修改当前后台账号资料
 */
export async function updateCurrentProfileApi(data: {
  displayName: string;
  phone?: string;
}) {
  const userInfo = await requestClient.put<AdminUserInfoResult>(
    '/admin/v1/auth/profile',
    data,
  );

  return {
    ...userInfo,
    realName: userInfo.realName || userInfo.displayName || userInfo.username,
    userId: String(userInfo.userId),
  };
}

/**
 * 修改当前后台账号密码
 */
export function updateCurrentPasswordApi(data: {
  newPassword: string;
  oldPassword: string;
}) {
  return requestClient.request<void>('/admin/v1/auth/password', {
    data,
    method: 'PATCH',
  });
}
