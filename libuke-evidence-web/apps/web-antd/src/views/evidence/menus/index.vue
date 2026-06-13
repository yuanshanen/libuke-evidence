<script lang="ts" setup>
import type { MenuRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { createMenuApi, deleteMenuApi, getMenusApi, updateMenuApi } from '#/api';
import { componentKeys } from '#/router/routes';

import {
  badgeVariantOptions,
  menuTypeOptions,
  useColumns,
  useGridFormSchema,
} from './data';
import { loadMenuActionTree, useMenuActionTitles } from '../menu-actions';

const saving = ref(false);
const drawerOpen = ref(false);
const editingId = ref<string>();
const menuTree = ref<MenuRecord[]>([]);
const { getActionTitle } = useMenuActionTitles('/evidence/menus');
void loadMenuActionTree();

const form = reactive({
  activeIcon: '',
  activePath: '',
  affixTab: false,
  badge: '',
  badgeType: undefined as string | undefined,
  badgeVariants: undefined as string | undefined,
  component: '',
  enabled: true,
  hidden: false,
  hideChildrenInMenu: false,
  hideInBreadcrumb: false,
  hideInTab: false,
  icon: '',
  keepAlive: false,
  linkSrc: '',
  name: '',
  parentId: undefined as string | undefined,
  path: '',
  permissionCode: '',
  sortOrder: 0,
  title: '',
  type: 'menu' as MenuRecord['type'],
});

const parentOptions = computed(() =>
  flattenMenus(menuTree.value)
    .filter((item) => item.type !== 'button' && item.id !== editingId.value)
    .map((item) => ({ label: item.title, value: item.id })),
);

const componentOptions = computed(() =>
  componentKeys.map((value) => ({ label: value, value })),
);

const showRouteFields = computed(() =>
  ['catalog', 'embedded', 'menu'].includes(form.type),
);
const showLinkField = computed(() => ['embedded', 'link'].includes(form.type));
const showComponentField = computed(() => form.type === 'menu');
const showMenuVisualFields = computed(() => form.type !== 'button');
const showAdvancedFields = computed(() => !['button', 'link'].includes(form.type));

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useColumns(onActionClick, getActionTitle),
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        query: async (_params, formValues) => {
          const records = await getMenusApi({ includeButtons: true });
          menuTree.value = records;
          const filtered = filterMenus(records, formValues.keyword, formValues.type);
          return {
            items: filtered,
            total: flattenMenus(filtered).length,
          };
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
    treeConfig: {
      childrenField: 'children',
      expandAll: true,
      rowField: 'id',
    },
  } as VxeTableGridOptions<MenuRecord>,
});

function flattenMenus(items: MenuRecord[]): MenuRecord[] {
  return items.flatMap((item) => [item, ...flattenMenus(item.children ?? [])]);
}

function filterMenus(items: MenuRecord[], keyword?: string, type?: string): MenuRecord[] {
  const normalizedKeyword = keyword?.trim().toLowerCase();
  return items
    .map((item) => {
      const children = filterMenus(item.children ?? [], keyword, type);
      const matchesKeyword =
        !normalizedKeyword ||
        [item.title, item.name, item.path, item.component, item.linkSrc, item.permissionCode]
          .filter(Boolean)
          .some((value) => value!.toLowerCase().includes(normalizedKeyword));
      const matchesType = !type || item.type === type;
      if ((matchesKeyword && matchesType) || children.length > 0) {
        return { ...item, children };
      }
      return null;
    })
    .filter(Boolean) as MenuRecord[];
}

function resetForm() {
  editingId.value = undefined;
  form.activeIcon = '';
  form.activePath = '';
  form.affixTab = false;
  form.badge = '';
  form.badgeType = undefined;
  form.badgeVariants = undefined;
  form.component = '';
  form.enabled = true;
  form.hidden = false;
  form.hideChildrenInMenu = false;
  form.hideInBreadcrumb = false;
  form.hideInTab = false;
  form.icon = '';
  form.keepAlive = false;
  form.linkSrc = '';
  form.name = '';
  form.parentId = undefined;
  form.path = '';
  form.permissionCode = '';
  form.sortOrder = 0;
  form.title = '';
  form.type = 'menu';
}

function openCreate(parent?: MenuRecord) {
  resetForm();
  form.parentId = parent?.id;
  form.type = parent?.type === 'menu' ? 'button' : 'menu';
  drawerOpen.value = true;
}

function openEdit(record: MenuRecord) {
  editingId.value = record.id;
  form.activeIcon = record.activeIcon ?? record.meta?.activeIcon ?? '';
  form.activePath = record.activePath ?? '';
  form.affixTab = Boolean(record.affixTab ?? record.meta?.affixTab);
  form.badge = record.badge ?? record.meta?.badge ?? '';
  form.badgeType = record.badgeType ?? record.meta?.badgeType;
  form.badgeVariants = record.badgeVariants ?? record.meta?.badgeVariants;
  form.component = record.component ?? '';
  form.enabled = record.enabled;
  form.hidden = Boolean(record.hidden ?? record.meta?.hideInMenu);
  form.hideChildrenInMenu = Boolean(record.hideChildrenInMenu ?? record.meta?.hideChildrenInMenu);
  form.hideInBreadcrumb = Boolean(record.hideInBreadcrumb ?? record.meta?.hideInBreadcrumb);
  form.hideInTab = Boolean(record.hideInTab ?? record.meta?.hideInTab);
  form.icon = record.icon ?? record.meta?.icon ?? '';
  form.keepAlive = Boolean(record.keepAlive ?? record.meta?.keepAlive);
  form.linkSrc = record.linkSrc ?? record.meta?.link ?? record.meta?.iframeSrc ?? '';
  form.name = record.name;
  form.parentId = record.parentId;
  form.path = record.path ?? '';
  form.permissionCode = record.permissionCode ?? record.authCode ?? record.meta?.authCode ?? '';
  form.sortOrder = record.sortOrder ?? 0;
  form.title = record.title ?? record.meta?.title ?? '';
  form.type = record.type;
  drawerOpen.value = true;
}

function onActionClick(e: OnActionClickParams<MenuRecord>) {
  if (e.code === 'createChild') {
    openCreate(e.row);
  }
  if (e.code === 'edit') {
    openEdit(e.row);
  }
  if (e.code === 'delete') {
    removeMenu(e.row.id);
  }
}

async function saveMenu() {
  if (!form.name.trim() || !form.title.trim()) {
    message.warning('请填写菜单标识和菜单名称');
    return;
  }
  if (showRouteFields.value && form.path.trim() && !form.path.trim().startsWith('/')) {
    message.warning('路由路径需要以 / 开头');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      activeIcon: form.activeIcon.trim() || undefined,
      activePath: form.activePath.trim() || undefined,
      affixTab: form.affixTab,
      badge: form.badge.trim() || undefined,
      badgeType: form.badgeType,
      badgeVariants: form.badgeVariants,
      component: form.component.trim() || undefined,
      enabled: form.enabled,
      hidden: form.hidden,
      hideChildrenInMenu: form.hideChildrenInMenu,
      hideInBreadcrumb: form.hideInBreadcrumb,
      hideInTab: form.hideInTab,
      icon: form.icon.trim() || undefined,
      keepAlive: form.keepAlive,
      linkSrc: form.linkSrc.trim() || undefined,
      name: form.name.trim(),
      parentId: form.parentId,
      path: form.path.trim() || undefined,
      permissionCode: form.permissionCode.trim() || undefined,
      sortOrder: form.sortOrder,
      title: form.title.trim(),
      type: form.type,
    };
    if (editingId.value) {
      await updateMenuApi(editingId.value, payload);
    } else {
      await createMenuApi(payload);
    }
    drawerOpen.value = false;
    await loadMenuActionTree();
    gridApi.query();
    message.success('菜单已保存，页面即将刷新');
    window.setTimeout(() => window.location.reload(), 300);
  } finally {
    saving.value = false;
  }
}

async function removeMenu(menuId: string) {
  await deleteMenuApi(menuId);
  message.success('已删除菜单');
  await loadMenuActionTree();
  gridApi.query();
}

</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <a-button type="primary" @click="openCreate()">
          <Plus class="size-5" />
          {{ getActionTitle('MenusCreate', '新增菜单') }}
        </a-button>
      </template>
      <template #title="{ row }">
        <div class="flex w-full items-center gap-1">
          <IconifyIcon
            v-if="row.type === 'button'"
            class="size-4 shrink-0"
            icon="lucide:shield"
          />
          <IconifyIcon
            v-else-if="row.icon || row.meta?.icon"
            class="size-4 shrink-0"
            :icon="row.icon || row.meta?.icon"
          />
          <span>{{ row.title || row.meta?.title }}</span>
        </div>
      </template>
    </Grid>

    <a-drawer v-model:open="drawerOpen" :title="null" width="760">
      <template #title>
        <div class="drawer-title-row">
          <span>{{ editingId ? getActionTitle('MenusUpdate', '编辑菜单') : getActionTitle('MenusCreate', '新增菜单') }}</span>
          <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" />
        </div>
      </template>
      <template #extra>
        <a-button :loading="saving" type="primary" @click="saveMenu">确定</a-button>
      </template>

      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="类型" required>
              <a-radio-group v-model:value="form.type" button-style="solid" option-type="button" :options="menuTypeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="父级">
              <a-select v-model:value="form.parentId" allow-clear show-search :options="parentOptions" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="菜单标识" required>
              <a-input v-model:value="form.name" maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单名称" required>
              <a-input v-model:value="form.title" maxlength="100" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item v-if="showRouteFields" label="路由路径">
          <a-input v-model:value="form.path" maxlength="255" placeholder="/evidence/dashboard" />
        </a-form-item>
        <a-form-item v-if="showRouteFields && form.type !== 'catalog'" label="激活路径">
          <a-input v-model:value="form.activePath" maxlength="255" placeholder="/evidence/dashboard" />
        </a-form-item>

        <a-form-item v-if="showComponentField" label="组件路径">
          <a-auto-complete
            v-model:value="form.component"
            :options="componentOptions"
            placeholder="/evidence/dashboard/index"
          />
        </a-form-item>
        <a-form-item v-if="showLinkField" label="链接地址">
          <a-input v-model:value="form.linkSrc" maxlength="500" placeholder="https://example.com" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item v-if="showMenuVisualFields" label="图标">
              <a-input v-model:value="form.icon" maxlength="100" placeholder="lucide:layout-dashboard" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item v-if="showMenuVisualFields && form.type !== 'link'" label="激活图标">
              <a-input v-model:value="form.activeIcon" maxlength="100" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="权限码">
              <a-input v-model:value="form.permissionCode" maxlength="100" placeholder="menus:update" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="form.sortOrder" class="w-full" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider v-if="showMenuVisualFields">徽标</a-divider>
        <a-row v-if="showMenuVisualFields" :gutter="16">
          <a-col :span="8">
            <a-form-item label="徽标类型">
              <a-select
                v-model:value="form.badgeType"
                allow-clear
                :options="[
                  { label: '点', value: 'dot' },
                  { label: '文本', value: 'normal' },
                ]"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="徽标文本">
              <a-input v-model:value="form.badge" :disabled="form.badgeType !== 'normal'" maxlength="50" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="徽标样式">
              <a-select v-model:value="form.badgeVariants" allow-clear :options="badgeVariantOptions" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider v-if="showAdvancedFields">高级设置</a-divider>
        <a-row v-if="showAdvancedFields" :gutter="[16, 12]">
          <a-col :span="8">
            <a-checkbox v-model:checked="form.keepAlive">缓存页面</a-checkbox>
          </a-col>
          <a-col :span="8">
            <a-checkbox v-model:checked="form.affixTab">固定标签页</a-checkbox>
          </a-col>
          <a-col :span="8">
            <a-checkbox v-model:checked="form.hidden">隐藏菜单</a-checkbox>
          </a-col>
          <a-col v-if="form.type === 'catalog' || form.type === 'menu'" :span="8">
            <a-checkbox v-model:checked="form.hideChildrenInMenu">隐藏子菜单</a-checkbox>
          </a-col>
          <a-col :span="8">
            <a-checkbox v-model:checked="form.hideInBreadcrumb">隐藏面包屑</a-checkbox>
          </a-col>
          <a-col :span="8">
            <a-checkbox v-model:checked="form.hideInTab">隐藏标签页</a-checkbox>
          </a-col>
        </a-row>
      </a-form>
    </a-drawer>
  </Page>
</template>

<style scoped>
.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
