import type { PlatformUserRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export const platformUserStatusOptions = [
  { color: 'success', label: '启用', value: true },
  { color: 'default', label: '停用', value: false },
];

export const superAdminOptions = [
  { color: 'error', label: '是', value: true },
  { color: 'default', label: '否', value: false },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入账号、显示名或手机号',
      },
      fieldName: 'keyword',
      label: '关键词',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: platformUserStatusOptions,
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<PlatformUserRecord>,
  getActionTitle: (key: string, fallback: string) => string,
): VxeTableGridColumns {
  return [
    { title: '序号', type: 'seq', width: 56 },
    {
      field: 'username',
      minWidth: 160,
      title: '账号',
    },
    {
      field: 'displayName',
      minWidth: 140,
      title: '显示名',
    },
    {
      field: 'phone',
      minWidth: 140,
      title: '手机号',
    },
    {
      field: 'roles',
      formatter: ({ row }) =>
        row.roles?.map((role: { name: string }) => role.name).join('、') || '-',
      minWidth: 180,
      title: '角色',
    },
    {
      field: 'communities',
      formatter: ({ row }) =>
        row.superAdmin
          ? '全部小区'
          : row.communities?.map((community: { name: string }) => community.name).join('、') || '-',
      minWidth: 220,
      title: '可管小区',
    },
    {
      cellRender: {
        name: 'CellTag',
        options: superAdminOptions,
      },
      field: 'superAdmin',
      title: '超级管理员',
      width: 120,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: platformUserStatusOptions,
      },
      field: 'enabled',
      title: '状态',
      width: 100,
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
          confirmTitle: '确认删除该 Web 用户？',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', text: getActionTitle('PlatformUsersUpdate', '编辑') },
          {
            code: 'resetPassword',
            text: getActionTitle('PlatformUsersResetPassword', '重置密码'),
          },
          {
            code: 'delete',
            confirm: true,
            text: getActionTitle('PlatformUsersDelete', '删除'),
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 220,
    },
  ];
}
