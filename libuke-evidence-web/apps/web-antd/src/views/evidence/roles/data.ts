import type { RoleRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export const roleStatusOptions = [
  { color: 'success', label: '启用', value: true },
  { color: 'default', label: '停用', value: false },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入角色名称、编码或备注',
      },
      fieldName: 'keyword',
      label: '关键词',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: roleStatusOptions,
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<RoleRecord>,
  getActionTitle: (key: string, fallback: string) => string,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    {
      field: 'name',
      title: '角色名称',
      width: 180,
    },
    {
      field: 'code',
      title: '角色编码',
      width: 180,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: roleStatusOptions,
      },
      field: 'enabled',
      title: '状态',
      width: 120,
    },
    {
      field: 'remark',
      minWidth: 260,
      title: '备注',
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
          confirmTitle: '确认删除该角色？已绑定用户的角色会自动停用。',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', text: getActionTitle('RolesUpdate', '编辑') },
          {
            code: 'delete',
            confirm: true,
            text: getActionTitle('RolesDelete', '删除'),
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 150,
    },
  ];
}
