<script lang="ts" setup>
import type { InvitationCodeRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeGridListeners,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createInvitationCodeApi,
  deleteInvitationCodeApi,
  getCommunitiesApi,
  getInvitationCodesApi,
  updateInvitationCodeApi,
} from '#/api';

import { useColumns, useGridFormSchema } from './data';

const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<string>();
const selectedCount = ref(0);
const communities = ref<{ label: string; value: string }[]>([]);
const form = reactive({
  code: '',
  communityId: undefined as string | undefined,
  enabled: true,
  expiresAt: undefined as string | undefined,
  maxUsageCount: undefined as number | undefined,
});

const gridEvents: VxeGridListeners<InvitationCodeRecord> = {
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
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getInvitationCodesApi({
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
  } as VxeTableGridOptions<InvitationCodeRecord>,
});

function updateSelectedCount() {
  selectedCount.value = gridApi.grid.getCheckboxRecords().length;
}

function onActionClick(e: OnActionClickParams<InvitationCodeRecord>) {
  switch (e.code) {
    case 'delete': {
      deleteInvitationCode(e.row);
      break;
    }
    case 'edit': {
      openEdit(e.row);
      break;
    }
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

function openCreate() {
  editingId.value = undefined;
  form.code = '';
  form.communityId = undefined;
  form.enabled = true;
  form.expiresAt = undefined;
  form.maxUsageCount = undefined;
  modalOpen.value = true;
}

function openEdit(record: InvitationCodeRecord) {
  editingId.value = record.id;
  form.code = record.code;
  form.communityId = record.communityId;
  form.enabled = record.enabled;
  form.expiresAt = record.expiresAt;
  form.maxUsageCount = record.maxUsageCount;
  modalOpen.value = true;
}

async function saveInvitationCode() {
  if (!form.communityId || !form.code.trim()) {
    message.warning('请选择小区并填写邀请码');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      code: form.code,
      communityId: form.communityId,
      enabled: form.enabled,
      expiresAt: form.expiresAt,
      maxUsageCount: form.maxUsageCount,
    };
    if (editingId.value) {
      await updateInvitationCodeApi(editingId.value, payload);
    } else {
      await createInvitationCodeApi(payload);
    }
    modalOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function deleteInvitationCode(record: InvitationCodeRecord) {
  await deleteInvitationCodeApi(record.id);
  message.success(record.usedCount > 0 ? '已停用邀请码' : '已删除邀请码');
  gridApi.query();
}

async function batchDeleteInvitationCodes() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要删除的邀请码');
    return;
  }
  await Promise.all(rows.map((row) => deleteInvitationCodeApi(row.id)));
  message.success(`已处理 ${rows.length} 个邀请码`);
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
        <a-space>
          <a-popconfirm
            v-if="selectedCount > 0"
            title="确认批量删除选中的邀请码？已使用的邀请码会自动停用。"
            @confirm="batchDeleteInvitationCodes"
          >
            <a-button danger>
              <IconifyIcon class="size-4" icon="lucide:trash-2" />
              批量删除
            </a-button>
          </a-popconfirm>
          <a-button type="primary" @click="openCreate">
            <Plus class="size-5" />
            新增邀请码
          </a-button>
        </a-space>
      </template>
    </Grid>

    <a-modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="editingId ? '编辑邀请码' : '新增邀请码'"
      @ok="saveInvitationCode"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="小区" required>
          <a-select v-model:value="form.communityId" :options="communities" placeholder="请选择小区" />
        </a-form-item>
        <a-form-item label="邀请码" required>
          <a-input v-model:value="form.code" maxlength="64" />
        </a-form-item>
        <a-form-item label="最大使用次数">
          <a-input-number v-model:value="form.maxUsageCount" :min="1" class="w-full" />
        </a-form-item>
        <a-form-item label="过期时间">
          <a-date-picker
            v-model:value="form.expiresAt"
            class="w-full"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>
