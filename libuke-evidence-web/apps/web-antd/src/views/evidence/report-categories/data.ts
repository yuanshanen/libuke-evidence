import type { ReportCategoryRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export const categoryStatusOptions = [
  { color: 'success', label: '启用', value: true },
  { color: 'default', label: '停用', value: false },
];

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入分类名称、编码或备注',
      },
      fieldName: 'keyword',
      label: '关键词',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: categoryStatusOptions,
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<ReportCategoryRecord>,
): VxeTableGridColumns {
  return [
    { align: 'center', type: 'checkbox', width: 44 },
    { title: '序号', type: 'seq', width: 56 },
    {
      field: 'name',
      minWidth: 180,
      title: '分类名称',
      treeNode: true,
    },
    {
      field: 'code',
      minWidth: 180,
      title: '分类编码',
    },
    {
      field: 'sortOrder',
      title: '排序',
      width: 90,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: categoryStatusOptions,
      },
      field: 'enabled',
      title: '状态',
      width: 110,
    },
    {
      field: 'remark',
      minWidth: 220,
      title: '备注',
    },
    {
      field: 'updatedAt',
      title: '更新时间',
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          confirmTitle: '确认删除该分类？',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'createChild',
            show: (row: ReportCategoryRecord) => !row.parentId,
            text: '新增子类',
          },
          { code: 'edit', text: '编辑' },
          { code: 'delete', confirm: true },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 220,
    },
  ];
}
