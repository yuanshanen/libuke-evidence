import type { ReportRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

import { statusOptions } from '#/views/evidence/constants';

export function useGridFormSchema(
  communities: { label: string; value: string }[],
  categories: { label: string; value: string }[],
): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入记录编号、位置、备注',
      },
      fieldName: 'keyword',
      label: '关键词',
    },
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
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: categories,
      },
      fieldName: 'category',
      label: '问题大类',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: statusOptions,
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'RangePicker',
      fieldName: 'submittedDate',
      label: '提交时间',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<ReportRecord>,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    {
      cellRender: {
        name: 'CellImage',
        props: {
          height: 44,
          preview: true,
          width: 44,
        },
      },
      field: 'firstImageUrl',
      title: '附件',
      width: 80,
    },
    {
      field: 'reportNo',
      title: '记录编号',
      width: 180,
    },
    {
      field: 'category',
      title: '问题大类',
      width: 130,
    },
    {
      field: 'subCategory',
      title: '常见问题',
      width: 130,
    },
    {
      field: 'communityName',
      title: '小区',
      width: 180,
    },
    {
      field: 'locationAddress',
      minWidth: 220,
      title: '位置',
    },
    {
      cellRender: {
        name: 'CellTag',
        options: statusOptions,
      },
      field: 'status',
      title: '状态',
      width: 110,
    },
    {
      field: 'submittedAt',
      title: '提交时间',
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          confirmTitle: '确认删除该记录？',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'detail', text: '详情' },
          { code: 'process', text: '处理' },
          { code: 'delete', confirm: true },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 180,
    },
  ];
}
