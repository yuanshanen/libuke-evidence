<script lang="ts" setup>
import type { UploadProps } from 'ant-design-vue';
import type { WatermarkTaskRecord, WatermarkTemplateRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createWatermarkTemplateApi,
  deleteWatermarkTemplateApi,
  getWatermarkTasksApi,
  getWatermarkTemplatesApi,
  retryWatermarkTaskApi,
  updateWatermarkTemplateApi,
} from '#/api';

import {
  positionOptions,
  taskStatusOptions,
  useColumns,
  useGridFormSchema,
} from './data';

const defaultTemplate =
  '{{communityName}}｜{{category}} / {{subCategory}}\n位置：{{locationAddress}}\n上报时间：{{submittedAt}}\n编号：{{reportNo}}';
const watermarkReferenceWidth = 720;
const sampleValues: Record<string, string> = {
  category: '公共卫生类',
  communityName: '御锦城阳光里',
  latitude: '34.221241',
  locationAddress: '雁塔区甘家寨小区西区(高新路东)',
  longitude: '108.900066',
  reportNo: 'R20260522046545354717321600',
  subCategory: '楼道卫生',
  submittedAt: '2026-05-22 20:46',
};

const templates = ref<WatermarkTemplateRecord[]>([]);
const saving = ref(false);
const drawerOpen = ref(false);
const editingId = ref<string>();
const previewImageUrl = ref('');
const previewCanvasRef = ref<HTMLCanvasElement>();

const form = reactive({
  backgroundColor: '#000000',
  backgroundOpacity: 0.58,
  contentTemplate: defaultTemplate,
  enabled: true,
  fontSize: undefined as number | undefined,
  name: '',
  opacity: 1,
  position: 'bottom',
  textColor: '#FFFFFF',
});

const enabledTemplate = computed(() =>
  templates.value.find((item) => item.enabled),
);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getWatermarkTasksApi({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            status: formValues.status,
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
  } as VxeTableGridOptions<WatermarkTaskRecord>,
});

function onActionClick(e: OnActionClickParams<WatermarkTaskRecord>) {
  if (e.code === 'retry') {
    retryTask(e.row.id);
  }
}

async function loadTemplates() {
  templates.value = await getWatermarkTemplatesApi();
}

function resetForm() {
  editingId.value = undefined;
  form.backgroundColor = '#000000';
  form.backgroundOpacity = 0.58;
  form.contentTemplate = defaultTemplate;
  form.enabled = true;
  form.fontSize = undefined;
  form.name = '';
  form.opacity = 1;
  form.position = 'bottom';
  form.textColor = '#FFFFFF';
  previewImageUrl.value = '';
}

function openCreate() {
  resetForm();
  drawerOpen.value = true;
}

function openEdit(record: WatermarkTemplateRecord) {
  editingId.value = record.id;
  form.backgroundColor = record.backgroundColor;
  form.backgroundOpacity = Number(record.backgroundOpacity);
  form.contentTemplate = record.contentTemplate;
  form.enabled = record.enabled;
  form.fontSize = record.fontSize;
  form.name = record.name;
  form.opacity = Number(record.opacity);
  form.position = record.position;
  form.textColor = record.textColor;
  previewImageUrl.value = '';
  drawerOpen.value = true;
}

async function saveTemplate() {
  if (!form.name.trim() || !form.contentTemplate.trim()) {
    message.warning('请填写模板名称和水印内容');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      backgroundColor: form.backgroundColor,
      backgroundOpacity: form.backgroundOpacity,
      contentTemplate: form.contentTemplate.trim(),
      enabled: form.enabled,
      fontSize: form.fontSize,
      name: form.name.trim(),
      opacity: form.opacity,
      position: form.position,
      textColor: form.textColor,
    };
    if (editingId.value) {
      await updateWatermarkTemplateApi(editingId.value, payload);
    } else {
      await createWatermarkTemplateApi(payload);
    }
    drawerOpen.value = false;
    await loadTemplates();
  } finally {
    saving.value = false;
  }
}

async function deleteTemplate(record: WatermarkTemplateRecord) {
  if (record.systemTemplate) {
    message.warning('系统模板不允许删除');
    return;
  }
  await deleteWatermarkTemplateApi(record.id);
  message.success('已删除模板');
  await loadTemplates();
}

async function retryTask(taskId: string) {
  await retryWatermarkTaskApi(taskId);
  message.success('已重新提交水印任务');
  gridApi.query();
}

const handlePreviewUpload: UploadProps['beforeUpload'] = (file) => {
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件');
    return false;
  }
  previewImageUrl.value = URL.createObjectURL(file);
  nextTick(drawPreview);
  return false;
};

function buildPreviewLines() {
  let content = form.contentTemplate;
  Object.entries(sampleValues).forEach(([key, value]) => {
    content = content.replaceAll(`{{${key}}}`, value);
  });
  return content.split('\n').filter(Boolean);
}

function drawPreview() {
  if (!previewImageUrl.value || !previewCanvasRef.value) {
    return;
  }
  const image = new Image();
  image.onload = () => {
    const canvas = previewCanvasRef.value;
    if (!canvas) return;
    const maxWidth = 720;
    const scale = Math.min(1, maxWidth / image.width);
    canvas.width = Math.round(image.width * scale);
    canvas.height = Math.round(image.height * scale);
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
    const lines = buildPreviewLines();
    const fontSize =
      form.fontSize && form.fontSize > 0
        ? Math.max(
            12,
            Math.round((form.fontSize * canvas.width) / watermarkReferenceWidth),
          )
        : Math.max(14, Math.round(canvas.width / 42));
    const padding = Math.max(12, Math.round(canvas.width / 42));
    const lineHeight = Math.round(fontSize * 1.45);
    const panelHeight = padding * 2 + lineHeight * lines.length;
    const panelY = form.position === 'top' ? 0 : canvas.height - panelHeight;
    ctx.globalAlpha = form.backgroundOpacity;
    ctx.fillStyle = form.backgroundColor;
    ctx.fillRect(0, panelY, canvas.width, panelHeight);
    ctx.globalAlpha = form.opacity;
    ctx.fillStyle = form.textColor;
    ctx.font = `bold ${fontSize}px sans-serif`;
    lines.forEach((line, index) => {
      ctx.fillText(line, padding, panelY + padding + fontSize + index * lineHeight);
    });
    ctx.globalAlpha = 1;
  };
  image.src = previewImageUrl.value;
}

onMounted(() => {
  loadTemplates();
});
</script>

<template>
  <Page auto-content-height class="watermark-page">
    <a-tabs class="watermark-tabs">
      <a-tab-pane key="templates" tab="水印模板">
        <div class="template-layout">
          <a-card title="当前启用模板" size="small">
            <template #extra>
              <a-button type="primary" @click="openCreate">
                <Plus class="size-5" />
                新增模板
              </a-button>
            </template>
            <a-empty v-if="!enabledTemplate" description="暂无启用模板" />
            <a-descriptions v-else bordered :column="2" size="small">
              <a-descriptions-item label="模板名称">
                {{ enabledTemplate.name }}
                <a-tag v-if="enabledTemplate.systemTemplate" class="ml-2">系统模板</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="位置">
                {{ enabledTemplate.position === 'top' ? '顶部' : '底部' }}
              </a-descriptions-item>
              <a-descriptions-item label="文字透明度">
                {{ enabledTemplate.opacity }}
              </a-descriptions-item>
              <a-descriptions-item label="背景透明度">
                {{ enabledTemplate.backgroundOpacity }}
              </a-descriptions-item>
              <a-descriptions-item label="水印内容" :span="2">
                <pre class="template-preview">{{ enabledTemplate.contentTemplate }}</pre>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <a-card title="模板列表" size="small">
            <a-table
              :columns="[
                { dataIndex: 'name', title: '模板名称' },
                { dataIndex: 'position', title: '位置', width: 100 },
                { dataIndex: 'enabled', title: '状态', width: 100 },
                { dataIndex: 'systemTemplate', title: '类型', width: 110 },
                { dataIndex: 'updatedAt', title: '更新时间', width: 180 },
                { dataIndex: 'operation', title: '操作', width: 150 },
              ]"
              :data-source="templates"
              :pagination="false"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'enabled'">
                  <a-tag :color="record.enabled ? 'green' : 'default'">
                    {{ record.enabled ? '启用' : '停用' }}
                  </a-tag>
                </template>
                <template v-if="column.dataIndex === 'systemTemplate'">
                  <a-tag :color="record.systemTemplate ? 'blue' : 'default'">
                    {{ record.systemTemplate ? '系统模板' : '自定义' }}
                  </a-tag>
                </template>
                <template v-if="column.dataIndex === 'position'">
                  {{ record.position === 'top' ? '顶部' : '底部' }}
                </template>
                <template v-if="column.dataIndex === 'operation'">
                  <a-space :size="8">
                    <a-button type="link" size="small" @click="openEdit(record)">
                      编辑
                    </a-button>
                    <a-popconfirm
                      v-if="!record.systemTemplate"
                      title="确认删除该水印模板？"
                      @confirm="deleteTemplate(record)"
                    >
                      <a-button danger type="link" size="small">删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </div>
      </a-tab-pane>

      <a-tab-pane key="tasks" tab="水印任务" class="task-pane">
        <Grid>
          <template #toolbar-tools>
            <a-space>
              <a-tag v-for="item in taskStatusOptions" :key="item.value">
                {{ item.label }}
              </a-tag>
            </a-space>
          </template>
        </Grid>
      </a-tab-pane>
    </a-tabs>

    <a-drawer
      v-model:open="drawerOpen"
      :title="null"
      width="860"
    >
      <template #title>
        <div class="drawer-title-row">
          <span>{{ editingId ? '编辑水印模板' : '新增水印模板' }}</span>
          <a-switch
            v-model:checked="form.enabled"
            checked-children="启用"
            un-checked-children="停用"
          />
        </div>
      </template>
      <template #extra>
        <a-button type="primary" :loading="saving" @click="saveTemplate">
          保存
        </a-button>
      </template>

      <a-form :model="form" layout="vertical">
        <a-form-item label="模板名称" required>
          <a-input v-model:value="form.name" maxlength="100" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="水印位置">
              <a-select
                v-model:value="form.position"
                class="field-control"
                :options="positionOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="字体大小">
              <a-input-number
                v-model:value="form.fontSize"
                class="field-control"
                :min="12"
                :max="96"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="文字透明度">
              <a-input-number
                v-model:value="form.opacity"
                class="field-control"
                :min="0"
                :max="1"
                :step="0.05"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="背景透明度">
              <a-input-number
                v-model:value="form.backgroundOpacity"
                class="field-control"
                :min="0"
                :max="1"
                :step="0.05"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="文字颜色">
              <div class="color-control">
                <input v-model="form.textColor" type="color" />
                <a-input v-model:value="form.textColor" />
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="背景颜色">
              <div class="color-control">
                <input v-model="form.backgroundColor" type="color" />
                <a-input v-model:value="form.backgroundColor" />
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="水印内容模板" required>
          <a-textarea v-model:value="form.contentTemplate" :rows="6" />
        </a-form-item>

        <a-card title="测试预览" size="small">
          <template #extra>
            <a-space>
              <a-upload
                accept="image/*"
                :before-upload="handlePreviewUpload"
                :show-upload-list="false"
              >
                <a-button>上传测试图片</a-button>
              </a-upload>
              <a-button :disabled="!previewImageUrl" @click="drawPreview">
                生成预览
              </a-button>
            </a-space>
          </template>
          <a-empty v-if="!previewImageUrl" description="上传一张图片查看当前模板效果" />
          <canvas v-show="previewImageUrl" ref="previewCanvasRef" class="preview-canvas"></canvas>
        </a-card>
      </a-form>
    </a-drawer>
  </Page>
</template>

<style scoped>
.watermark-page {
  height: 100%;
}

.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.watermark-tabs {
  height: 100%;
}

.watermark-tabs :deep(.ant-tabs-content-holder),
.watermark-tabs :deep(.ant-tabs-content),
.watermark-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

.template-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-pane {
  height: 100%;
}

.template-preview {
  margin: 0;
  white-space: pre-wrap;
}

.drawer-enabled-label {
  color: rgb(0 0 0 / 65%);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
}

.field-control {
  width: 100%;
}

.field-control :deep(.ant-input-number) {
  width: 100%;
}

.color-control {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.color-control input[type='color'] {
  width: 42px;
  height: 32px;
  padding: 0;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: transparent;
}

.preview-canvas {
  display: block;
  max-width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
</style>
