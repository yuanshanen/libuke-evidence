<script lang="ts" setup>
import type { ReportCategoryRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeGridListeners,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createReportCategoryApi,
  deleteReportCategoryApi,
  getReportCategoriesApi,
  updateReportCategoryApi,
} from '#/api';

import {
  categoryStatusOptions,
  useColumns,
  useGridFormSchema,
} from './data';

const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<string>();
const selectedCount = ref(0);
const rootCategories = ref<{ label: string; value: string }[]>([]);

const form = reactive({
  code: '',
  enabled: true,
  name: '',
  parentId: undefined as string | undefined,
  remark: '',
  sortOrder: 0,
});

const hasSelection = computed(() => selectedCount.value > 0);

const gridEvents: VxeGridListeners<ReportCategoryRecord> = {
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
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        query: async (_params, formValues) => {
          const records = await getReportCategoriesApi({
            enabled: formValues.enabled,
            keyword: formValues.keyword,
          });
          syncRootCategories(records);
          return {
            items: records,
            total: records.length,
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
  } as VxeTableGridOptions<ReportCategoryRecord>,
});

function updateSelectedCount() {
  selectedCount.value = gridApi.grid.getCheckboxRecords().length;
}

function onActionClick(e: OnActionClickParams<ReportCategoryRecord>) {
  if (e.code === 'createChild') {
    openCreate(e.row);
  }
  if (e.code === 'edit') {
    openEdit(e.row);
  }
  if (e.code === 'delete') {
    deleteCategory(e.row.id);
  }
}

function syncRootCategories(records: ReportCategoryRecord[]) {
  rootCategories.value = records
    .filter((item) => !item.parentId)
    .map((item) => ({ label: item.name, value: item.id }));
}

function resetForm(parent?: ReportCategoryRecord) {
  editingId.value = undefined;
  form.code = '';
  form.enabled = true;
  form.name = '';
  form.parentId = parent?.id;
  form.remark = '';
  form.sortOrder = 0;
}

function openCreate(parent?: ReportCategoryRecord) {
  if (parent?.parentId) {
    message.warning('小类下不能继续新增子类');
    return;
  }
  resetForm(parent);
  modalOpen.value = true;
}

function openEdit(record: ReportCategoryRecord) {
  editingId.value = record.id;
  form.code = record.code ?? '';
  form.enabled = record.enabled;
  form.name = record.name;
  form.parentId = record.parentId;
  form.remark = record.remark ?? '';
  form.sortOrder = record.sortOrder ?? 0;
  modalOpen.value = true;
}

async function saveCategory() {
  if (!form.name.trim()) {
    message.warning('请填写分类名称');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      code: form.code.trim() || undefined,
      enabled: form.enabled,
      name: form.name.trim(),
      parentId: form.parentId,
      remark: form.remark.trim() || undefined,
      sortOrder: Number(form.sortOrder) || 0,
    };
    if (editingId.value) {
      await updateReportCategoryApi(editingId.value, payload);
    } else {
      await createReportCategoryApi(payload);
    }
    modalOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function deleteCategory(categoryId: string) {
  await deleteReportCategoryApi(categoryId);
  message.success('已删除分类');
  gridApi.query();
}

async function batchDeleteCategories() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要删除的分类');
    return;
  }
  await Promise.all(rows.map((row) => deleteReportCategoryApi(row.id)));
  message.success(`已删除 ${rows.length} 个分类`);
  selectedCount.value = 0;
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <a-space>
          <a-popconfirm
            v-if="hasSelection"
            title="确认批量删除选中的分类？存在子类的分类需要先删除子类。"
            @confirm="batchDeleteCategories"
          >
            <a-button danger>
              <IconifyIcon class="size-4" icon="lucide:trash-2" />
              批量删除
            </a-button>
          </a-popconfirm>
          <a-button type="primary" @click="openCreate()">
            <Plus class="size-5" />
            新增大类
          </a-button>
        </a-space>
      </template>
    </Grid>

    <a-modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="editingId ? '编辑分类' : form.parentId ? '新增小类' : '新增大类'"
      width="560px"
      @ok="saveCategory"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="上级分类">
          <a-select
            v-model:value="form.parentId"
            allow-clear
            :disabled="!!editingId"
            :options="rootCategories"
            placeholder="不选择则为问题大类"
          />
        </a-form-item>
        <a-form-item label="分类名称" required>
          <a-input
            v-model:value="form.name"
            maxlength="100"
            placeholder="例如：公共卫生类 / 楼道卫生"
          />
        </a-form-item>
        <a-form-item label="分类编码">
          <a-input
            v-model:value="form.code"
            maxlength="64"
            placeholder="例如：public_health"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sortOrder" class="w-full" :min="0" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group
            v-model:value="form.enabled"
            :options="categoryStatusOptions"
            option-type="button"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="form.remark"
            :rows="3"
            maxlength="255"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </Page>
</template>
