<script lang="ts" setup>
import type { MenuRecord, RoleRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeGridListeners,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createRoleApi,
  deleteRoleApi,
  getMenusApi,
  getRolesApi,
  updateRoleApi,
} from '#/api';

import { useColumns, useGridFormSchema } from './data';
import { loadMenuActionTree, useMenuActionTitles } from '../menu-actions';

const saving = ref(false);
const drawerOpen = ref(false);
const editingId = ref<string>();
const selectedCount = ref(0);
const menuTree = ref<MenuRecord[]>([]);
const { getActionTitle } = useMenuActionTitles('/evidence/roles');
void loadMenuActionTree();
const form = reactive({
  code: '',
  enabled: true,
  menuIds: [] as string[],
  name: '',
  remark: '',
});

const treeData = computed(() => mapMenuTree(menuTree.value));

const gridEvents: VxeGridListeners<RoleRecord> = {
  checkboxAll: updateSelectedCount,
  checkboxChange: updateSelectedCount,
};

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridEvents,
  gridOptions: {
    checkboxConfig: {
      highlight: true,
    },
    columns: useColumns(onActionClick, getActionTitle),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getRolesApi({
            enabled: formValues.enabled,
            keyword: formValues.keyword,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          });
          return {
            items: result.records,
            total: result.total,
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
  } as VxeTableGridOptions<RoleRecord>,
});

function updateSelectedCount() {
  selectedCount.value = gridApi.grid.getCheckboxRecords().length;
}

function onActionClick(e: OnActionClickParams<RoleRecord>) {
  if (e.code === 'edit') {
    openEdit(e.row);
  }
  if (e.code === 'delete') {
    deleteRole(e.row.id);
  }
}

function openCreate() {
  editingId.value = undefined;
  form.code = '';
  form.enabled = true;
  form.menuIds = [];
  form.name = '';
  form.remark = '';
  drawerOpen.value = true;
}

function openEdit(record: RoleRecord) {
  editingId.value = record.id;
  form.code = record.code;
  form.enabled = record.enabled;
  form.menuIds = record.menuIds ?? [];
  form.name = record.name;
  form.remark = record.remark ?? '';
  drawerOpen.value = true;
}

async function saveRole() {
  if (!form.name.trim() || !form.code.trim()) {
    message.warning('请填写角色名称和角色编码');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      code: form.code.trim(),
      enabled: form.enabled,
      menuIds: form.menuIds,
      name: form.name.trim(),
      remark: form.remark || undefined,
    };
    if (editingId.value) {
      await updateRoleApi(editingId.value, payload);
    } else {
      await createRoleApi(payload);
    }
    drawerOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function deleteRole(roleId: string) {
  await deleteRoleApi(roleId);
  gridApi.query();
}

async function batchDeleteRoles() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要删除的角色');
    return;
  }
  await Promise.all(rows.map((row) => deleteRoleApi(row.id)));
  message.success(`已处理 ${rows.length} 个角色`);
  selectedCount.value = 0;
  gridApi.query();
}

function mapMenuTree(items: MenuRecord[]): any[] {
  return items.map((item) => ({
    children: item.children?.length ? mapMenuTree(item.children) : undefined,
    key: item.id,
    title: `${item.title}${item.type === 'button' ? '（按钮）' : ''}`,
  }));
}

async function loadMenus() {
  menuTree.value = await getMenusApi({ includeButtons: true });
}

onMounted(() => {
  loadMenus();
});
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <a-space>
          <a-popconfirm
            v-if="selectedCount > 0"
            title="确认批量删除选中的角色？已绑定用户的角色会自动停用。"
            @confirm="batchDeleteRoles"
          >
            <a-button danger>
              <IconifyIcon class="size-4" icon="lucide:trash-2" />
              {{ getActionTitle('RolesDelete', '批量删除') }}
            </a-button>
          </a-popconfirm>
          <a-button type="primary" @click="openCreate">
            <Plus class="size-5" />
            {{ getActionTitle('RolesCreate', '新增角色') }}
          </a-button>
        </a-space>
      </template>
    </Grid>

    <a-drawer
      v-model:open="drawerOpen"
      :title="null"
      width="720"
    >
      <template #title>
        <div class="drawer-title-row">
          <span>{{ editingId ? getActionTitle('RolesUpdate', '编辑角色') : getActionTitle('RolesCreate', '新增角色') }}</span>
          <a-switch
            v-model:checked="form.enabled"
            checked-children="启用"
            un-checked-children="停用"
          />
        </div>
      </template>
      <template #extra>
        <a-button :loading="saving" type="primary" @click="saveRole">
          确定
        </a-button>
      </template>

      <a-form :model="form" layout="vertical">
        <a-form-item label="角色名称" required>
          <a-input v-model:value="form.name" maxlength="100" />
        </a-form-item>
        <a-form-item label="角色编码" required>
          <a-input v-model:value="form.code" maxlength="64" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="form.remark"
            :rows="3"
            maxlength="255"
            show-count
          />
        </a-form-item>
        <a-form-item label="菜单与按钮权限">
          <div class="permission-tree">
            <a-tree
              v-model:checked-keys="form.menuIds"
              checkable
              default-expand-all
              :tree-data="treeData"
            />
          </div>
        </a-form-item>
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

.permission-tree {
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}
</style>
