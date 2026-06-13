import { baseRequestClient, requestClient } from '#/api/request';

import { getUserInfoApi } from './user';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
    displayName?: string;
    username?: string;
  }

  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const result = await requestClient.post<{
    displayName?: string;
    token: string;
    username?: string;
  }>('/admin/v1/auth/login', data);

  return {
    accessToken: result.token,
    displayName: result.displayName,
    username: result.username,
  };
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi() {
  return baseRequestClient.post<AuthApi.RefreshTokenResult>('/auth/refresh', {
    withCredentials: true,
  });
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return Promise.resolve();
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  const userInfo = await getUserInfoApi();
  return userInfo.permissions ?? [];
}
