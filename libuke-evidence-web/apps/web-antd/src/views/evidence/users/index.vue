<script lang="ts" setup>
import type { UserRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeGridListeners,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteUserApi,
  getCommunitiesApi,
  getUsersApi,
  updateUserApi,
  uploadAvatarApi,
} from '#/api';

import { useColumns, useGridFormSchema, userStatusOptions } from './data';
import { loadMenuActionTree, useMenuActionTitles } from '../menu-actions';

const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<string>();
const selectedCount = ref(0);
const communities = ref<{ label: string; value: string }[]>([]);
const { getActionTitle } = useMenuActionTitles('/evidence/users');
void loadMenuActionTree();

const form = reactive({
  authStatus: 'pending',
  avatarObjectKey: undefined as string | undefined,
  avatarUrl: undefined as string | undefined,
  communityId: undefined as string | undefined,
  nickname: '',
  witnessInfo: '',
});

const statusOptions = userStatusOptions.map(({ label, value }) => ({
  label,
  value,
}));

const gridEvents: VxeGridListeners<UserRecord> = {
  checkboxAll: updateSelectedCount,
  checkboxChange: updateSelectedCount,
};

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(communities.value),
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
          const result = await getUsersApi({
            communityId: formValues.communityId,
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
  } as VxeTableGridOptions<UserRecord>,
});

function updateSelectedCount() {
  selectedCount.value = gridApi.grid.getCheckboxRecords().length;
}

function onActionClick(e: OnActionClickParams<UserRecord>) {
  if (e.code === 'edit') {
    openEdit(e.row);
  }
}

async function loadCommunities() {
  const result = await getCommunitiesApi({ pageNo: 1, pageSize: 200 });
  communities.value.splice(
    0,
    communities.value.length,
    ...result.records.map((item) => ({
      label: item.name,
      value: item.id,
    })),
  );
}

function openEdit(record: UserRecord) {
  editingId.value = record.id;
  form.authStatus = record.authStatus || 'pending';
  form.avatarObjectKey = record.avatarObjectKey;
  form.avatarUrl = record.avatarUrl;
  form.communityId = record.communityId;
  form.nickname = record.nickname ?? '';
  form.witnessInfo = record.witnessInfo ?? '';
  modalOpen.value = true;
}

async function uploadAvatar(options: any) {
  const file = options.file as File;
  try {
    const result = await uploadAvatarApi(file);
    form.avatarObjectKey = result.objectKey;
    form.avatarUrl = result.url;
    options.onSuccess?.(result);
  } catch (error) {
    options.onError?.(error);
  }
}

async function saveUser() {
  if (!editingId.value) return;
  if (
    (form.authStatus === 'verified' || form.authStatus === 'admin') &&
    !form.communityId
  ) {
    message.warning('已认证或管理员用户需要绑定小区');
    return;
  }
  saving.value = true;
  try {
    await updateUserApi(editingId.value, {
      authStatus: form.authStatus,
      avatarObjectKey: form.avatarObjectKey,
      communityId: form.communityId,
      nickname: form.nickname || undefined,
      witnessInfo: form.witnessInfo || undefined,
    });
    modalOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function batchDeleteUsers() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要删除的用户');
    return;
  }
  await Promise.all(rows.map((row) => deleteUserApi(row.id)));
  message.success(`已删除 ${rows.length} 个用户`);
  selectedCount.value = 0;
  gridApi.query();
}

onMounted(() => {
  loadCommunities();
});
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <a-popconfirm
          v-if="selectedCount > 0"
          title="确认批量删除选中的用户？"
          @confirm="batchDeleteUsers"
        >
          <a-button danger>
            <IconifyIcon class="size-4" icon="lucide:trash-2" />
            {{ getActionTitle('WxUsersDelete', '批量删除') }}
          </a-button>
        </a-popconfirm>
      </template>
    </Grid>

    <a-modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="getActionTitle('WxUsersUpdate', '编辑用户')"
      @ok="saveUser"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="用户头像">
          <div class="avatar-field">
            <a-avatar :size="56" :src="form.avatarUrl">
              {{ form.nickname?.slice(0, 1) || '用' }}
            </a-avatar>
            <a-upload
              accept="image/jpeg,image/png"
              :custom-request="uploadAvatar"
              :max-count="1"
              :show-upload-list="false"
            >
              <a-button>上传头像</a-button>
            </a-upload>
          </div>
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="form.nickname" maxlength="100" />
        </a-form-item>
        <a-form-item label="绑定小区">
          <a-select
            v-model:value="form.communityId"
            allow-clear
            :options="communities"
            placeholder="请选择小区"
          />
        </a-form-item>
        <a-form-item label="认证状态" required>
          <a-select v-model:value="form.authStatus" :options="statusOptions" />
        </a-form-item>
        <a-form-item label="取证人信息">
          <a-input v-model:value="form.witnessInfo" maxlength="200" />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>

<style scoped>
.avatar-field {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
