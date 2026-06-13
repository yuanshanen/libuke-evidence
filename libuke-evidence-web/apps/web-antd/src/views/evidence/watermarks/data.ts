import type { WatermarkTaskRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export const taskStatusOptions = [
  { color: 'default', label: '待处理', value: 'pending' },
  { color: 'processing', label: '处理中', value: 'processing' },
  { color: 'success', label: '成功', value: 'success' },
  { color: 'error', label: '失败', value: 'failed' },
];

export const positionOptions = [
  { label: '底部', value: 'bottom' },
  { label: '顶部', value: 'top' },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: taskStatusOptions,
      },
      fieldName: 'status',
      label: '任务状态',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<WatermarkTaskRecord>,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    { field: 'reportNo', title: '记录编号', width: 220 },
    { field: 'templateName', title: '模板', minWidth: 180 },
    {
      cellRender: {
        name: 'CellTag',
        options: taskStatusOptions,
      },
      field: 'status',
      title: '状态',
      width: 110,
    },
    { field: 'retryCount', title: '处理次数', width: 100 },
    { field: 'errorMessage', title: '失败原因', minWidth: 260 },
    { field: 'updatedAt', title: '更新时间', width: 180 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'retry',
            show: (row: WatermarkTaskRecord) => row.status === 'failed',
            text: '重试',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 110,
    },
  ];
}
