import type { CommunityRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入小区名称或地址',
      },
      fieldName: 'keyword',
      label: '关键词',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<CommunityRecord>,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    {
      field: 'name',
      title: '小区名称',
      width: 220,
    },
    {
      field: 'address',
      minWidth: 260,
      title: '地址',
    },
    {
      field: 'principalName',
      title: '负责人',
      width: 140,
    },
    {
      field: 'principalPhone',
      title: '联系方式',
      width: 160,
    },
    {
      field: 'userCount',
      title: '用户数',
      width: 120,
    },
    {
      field: 'reportCount',
      title: '记录数',
      width: 120,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { color: 'success', label: '启用', value: true },
          { color: 'default', label: '禁用', value: false },
        ],
      },
      field: 'enabled',
      title: '状态',
      width: 120,
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
          confirmTitle: '确认删除该小区？',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', { code: 'delete', confirm: true }],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 150,
    },
  ];
}
