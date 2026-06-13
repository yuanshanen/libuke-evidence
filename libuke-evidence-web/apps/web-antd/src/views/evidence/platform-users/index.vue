<script lang="ts" setup>
import type { PlatformUserRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createPlatformUserApi,
  deletePlatformUserApi,
  getCommunitiesApi,
  getPlatformUsersApi,
  getRolesApi,
  resetPlatformUserPasswordApi,
  updatePlatformUserApi,
} from '#/api';

import { useColumns, useGridFormSchema } from './data';
import { loadMenuActionTree, useMenuActionTitles } from '../menu-actions';

const saving = ref(false);
const drawerOpen = ref(false);
const resetOpen = ref(false);
const editingId = ref<string>();
const communities = ref<{ label: string; value: string }[]>([]);
const roles = ref<{ code: string; label: string; value: string }[]>([]);
const { getActionTitle } = useMenuActionTitles('/evidence/platform-users');
void loadMenuActionTree();

const form = reactive({
  communityIds: [] as string[],
  displayName: '',
  enabled: true,
  password: '',
  phone: '',
  roleIds: [] as string[],
  superAdmin: false,
  username: '',
});

const resetForm = reactive({
  password: '',
  userId: '',
});

const roleOptions = computed(() =>
  roles.value.map((item) => ({
    ...item,
    disabled: item.code === 'super_admin',
  })),
);
const communityOptions = computed(() => communities.value);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useColumns(onActionClick, getActionTitle),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getPlatformUsersApi({
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
  } as VxeTableGridOptions<PlatformUserRecord>,
});

function resetFormValues() {
  editingId.value = undefined;
  form.communityIds = [];
  form.displayName = '';
  form.enabled = true;
  form.password = '';
  form.phone = '';
  form.roleIds = [];
  form.superAdmin = false;
  form.username = '';
}

function openCreate() {
  resetFormValues();
  drawerOpen.value = true;
}

function openEdit(record: PlatformUserRecord) {
  editingId.value = record.id;
  form.communityIds = record.communityIds ?? [];
  form.displayName = record.displayName;
  form.enabled = record.enabled;
  form.password = '';
  form.phone = record.phone ?? '';
  form.roleIds = record.roleIds ?? [];
  form.superAdmin = record.superAdmin;
  form.username = record.username;
  drawerOpen.value = true;
}

function openReset(record: PlatformUserRecord) {
  resetForm.userId = record.id;
  resetForm.password = '';
  resetOpen.value = true;
}

function onActionClick(e: OnActionClickParams<PlatformUserRecord>) {
  if (e.code === 'edit') {
    openEdit(e.row);
  }
  if (e.code === 'resetPassword') {
    openReset(e.row);
  }
  if (e.code === 'delete') {
    deleteUser(e.row.id);
  }
}

async function loadOptions() {
  const [communityResult, roleResult] = await Promise.all([
    getCommunitiesApi({ pageNo: 1, pageSize: 500 }),
    getRolesApi({ enabled: true, pageNo: 1, pageSize: 500 }),
  ]);
  communities.value = communityResult.records.map((item) => ({
    label: item.name,
    value: item.id,
  }));
  roles.value = roleResult.records.map((item) => ({
    code: item.code,
    label: item.name,
    value: item.id,
  }));
}

async function saveUser() {
  if (!form.username.trim() || !form.displayName.trim()) {
    message.warning('请填写账号和显示名');
    return;
  }
  if (!editingId.value && !form.password.trim()) {
    message.warning('新增用户需要填写初始密码');
    return;
  }
  saving.value = true;
  try {
    const assignableRoleIds = form.roleIds.filter((roleId) => {
      return roles.value.find((role) => role.value === roleId)?.code !== 'super_admin';
    });
    const payload = {
      communityIds: form.communityIds,
      displayName: form.displayName.trim(),
      enabled: form.enabled,
      password: form.password.trim() || undefined,
      phone: form.phone.trim() || undefined,
      roleIds: assignableRoleIds,
      superAdmin: form.superAdmin,
      username: form.username.trim(),
    };
    if (editingId.value) {
      await updatePlatformUserApi(editingId.value, payload);
    } else {
      await createPlatformUserApi(payload);
    }
    drawerOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function resetPassword() {
  if (resetForm.password.trim().length < 6) {
    message.warning('新密码至少 6 位');
    return;
  }
  await resetPlatformUserPasswordApi(resetForm.userId, {
    password: resetForm.password.trim(),
  });
  resetOpen.value = false;
  message.success('密码已重置');
}

async function deleteUser(userId: string) {
  await deletePlatformUserApi(userId);
  message.success('已删除 Web 用户');
  gridApi.query();
}

onMounted(() => {
  loadOptions();
});
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <a-button type="primary" @click="openCreate">
          <Plus class="size-5" />
          {{ getActionTitle('PlatformUsersCreate', '新增后台用户') }}
        </a-button>
      </template>
    </Grid>

    <a-drawer
      v-model:open="drawerOpen"
      :title="null"
      width="640"
    >
      <template #title>
        <div class="drawer-title-row">
          <span>{{ editingId ? getActionTitle('PlatformUsersUpdate', '编辑后台用户') : getActionTitle('PlatformUsersCreate', '新增后台用户') }}</span>
          <a-switch
            v-model:checked="form.enabled"
            checked-children="启用"
            un-checked-children="停用"
          />
        </div>
      </template>
      <template #extra>
        <a-button :loading="saving" type="primary" @click="saveUser">
          确定
        </a-button>
      </template>

      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="登录账号" required>
              <a-input v-model:value="form.username" maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示名" required>
              <a-input v-model:value="form.displayName" maxlength="100" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="editingId ? '新密码' : '初始密码'" :required="!editingId">
              <a-input-password v-model:value="form.password" maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号">
              <a-input v-model:value="form.phone" maxlength="30" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="角色">
          <a-select v-model:value="form.roleIds" mode="multiple" :options="roleOptions" placeholder="请选择角色" />
        </a-form-item>
        <a-form-item label="可管小区">
          <a-select
            v-model:value="form.communityIds"
            mode="multiple"
            :options="communityOptions"
            placeholder="超级管理员可不选，普通用户请选择可管小区"
          />
        </a-form-item>
      </a-form>
    </a-drawer>

    <a-modal v-model:open="resetOpen" :title="getActionTitle('PlatformUsersResetPassword', '重置密码')" @ok="resetPassword">
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="resetForm.password" maxlength="64" />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>

<style scoped>
.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
