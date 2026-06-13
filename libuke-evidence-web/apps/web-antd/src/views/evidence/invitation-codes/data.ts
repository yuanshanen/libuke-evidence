import type { InvitationCodeRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

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
        placeholder: '请输入邀请码',
      },
      fieldName: 'keyword',
      label: '邀请码',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<InvitationCodeRecord>,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    {
      field: 'code',
      title: '邀请码',
      width: 180,
    },
    {
      field: 'communityName',
      minWidth: 220,
      title: '小区',
    },
    {
      field: 'usedCount',
      title: '已使用',
      width: 110,
    },
    {
      field: 'maxUsageCount',
      formatter: ({ cellValue }) => cellValue ?? '不限',
      title: '最大次数',
      width: 120,
    },
    {
      field: 'expiresAt',
      title: '过期时间',
      width: 180,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { color: 'success', label: '启用', value: true },
          { color: 'default', label: '停用', value: false },
        ],
      },
      field: 'enabled',
      title: '状态',
      width: 110,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          'edit',
          {
            code: 'delete',
            confirm: true,
            danger: true,
            text: ({ usedCount }: { usedCount: number }) =>
              usedCount > 0 ? '停用' : '删除',
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
