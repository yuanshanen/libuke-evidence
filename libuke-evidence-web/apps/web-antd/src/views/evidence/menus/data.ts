import type { MenuRecord } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

export const menuTypeOptions = [
  { color: 'processing', label: '目录', value: 'catalog' },
  { color: 'success', label: '菜单', value: 'menu' },
  { color: 'warning', label: '按钮', value: 'button' },
  { color: 'default', label: '内嵌', value: 'embedded' },
  { color: 'error', label: '外链', value: 'link' },
];

export const menuStatusOptions = [
  { color: 'success', label: '启用', value: true },
  { color: 'default', label: '停用', value: false },
];

export const badgeVariantOptions = [
  'default',
  'destructive',
  'primary',
  'success',
  'warning',
].map((value) => ({ label: value, value }));

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入菜单名称、路由或权限码',
      },
      fieldName: 'keyword',
      label: '关键字',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: menuTypeOptions,
      },
      fieldName: 'type',
      label: '类型',
    },
  ];
}

export function useColumns(
  onActionClick: OnActionClickFn<MenuRecord>,
  getActionTitle: (key: string, fallback: string) => string,
): VxeTableGridColumns {
  return [
    {
      field: 'title',
      fixed: 'left',
      minWidth: 220,
      slots: { default: 'title' },
      title: '菜单名称',
      treeNode: true,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: menuTypeOptions,
      },
      field: 'type',
      title: '类型',
      width: 100,
    },
    {
      field: 'permissionCode',
      minWidth: 180,
      title: '权限码',
    },
    {
      field: 'path',
      minWidth: 220,
      title: '路由路径',
    },
    {
      field: 'component',
      formatter: ({ row }) => {
        if (row.type === 'embedded' || row.type === 'link') {
          return row.linkSrc ?? '';
        }
        return row.component ?? '';
      },
      minWidth: 240,
      title: '组件/链接',
    },
    {
      field: 'sortOrder',
      title: '排序',
      width: 90,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: menuStatusOptions,
      },
      field: 'enabled',
      title: '状态',
      width: 100,
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
          confirmTitle: '确认删除该菜单？',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'createChild',
            show: (row: MenuRecord) => row.type !== 'button',
            text: getActionTitle('MenusCreate', '新增下级'),
          },
          {
            code: 'edit',
            text: getActionTitle('MenusUpdate', '编辑'),
          },
          {
            code: 'delete',
            confirm: true,
            text: getActionTitle('MenusDelete', '删除'),
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
