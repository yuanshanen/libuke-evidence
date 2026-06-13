import type { UserRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export const userStatusOptions = [
  { color: 'default', label: '未认证', value: 'pending' },
  { color: 'success', label: '已认证', value: 'verified' },
  { color: 'error', label: '已禁用', value: 'disabled' },
  { color: 'processing', label: '管理员', value: 'admin' },
];

export function useGridFormSchema(
  communities: { label: string; value: string }[],
): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: communities,
      },
      fieldName: 'communityId',
      label: '小区',
    },
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入 openid、昵称、取证人信息',
      },
      fieldName: 'keyword',
      label: '关键词',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<UserRecord>,
  getActionTitle: (key: string, fallback: string) => string,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    {
      cellRender: {
        name: 'CellImage',
        props: {
          height: 36,
          preview: true,
          style: { borderRadius: '50%', objectFit: 'cover' },
          width: 36,
        },
      },
      field: 'avatarUrl',
      title: '头像',
      width: 80,
    },
    {
      field: 'openid',
      minWidth: 240,
      title: 'openid',
    },
    {
      field: 'nickname',
      title: '昵称',
      width: 140,
    },
    {
      field: 'communityName',
      title: '绑定小区',
      width: 180,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: userStatusOptions,
      },
      field: 'authStatus',
      title: '认证状态',
      width: 120,
    },
    {
      field: 'witnessInfo',
      title: '取证人信息',
      width: 180,
    },
    {
      field: 'lastLoginAt',
      title: '最近登录',
      width: 180,
    },
    {
      field: 'createdAt',
      title: '创建时间',
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [{ code: 'edit', text: getActionTitle('WxUsersUpdate', '编辑') }],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 120,
    },
  ];
}
