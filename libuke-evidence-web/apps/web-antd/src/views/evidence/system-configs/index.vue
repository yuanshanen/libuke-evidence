<script lang="ts" setup>
import type {
  ExportTemplateRecord,
  MapConfigRecord,
  StorageConfigRecord,
  SystemConfigRecord,
  UploadPolicyRecord,
} from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { message } from 'ant-design-vue';

import {
  getExportTemplatesApi,
  getMapConfigsApi,
  getStorageConfigsApi,
  getSystemConfigsApi,
  getUploadPoliciesApi,
  updateExportTemplateApi,
  updateMapConfigApi,
  updateStorageConfigApi,
  updateSystemConfigApi,
  updateUploadPolicyApi,
} from '#/api';

type ConfigDomain = 'export' | 'map' | 'storage' | 'system' | 'upload';

const activeTab = ref<ConfigDomain>('system');
const loading = ref(false);
const savingKey = ref('');

const systemConfigs = ref<SystemConfigRecord[]>([]);
const storageConfigs = ref<StorageConfigRecord[]>([]);
const mapConfigs = ref<MapConfigRecord[]>([]);
const uploadPolicies = ref<UploadPolicyRecord[]>([]);
const exportTemplates = ref<ExportTemplateRecord[]>([]);

const systemForm = reactive<Record<string, boolean | string>>({});
const storageForm = reactive<Record<string, any>>({});
const mapForm = reactive<Record<string, any>>({});
const uploadForm = reactive<Record<string, any>>({});
const exportForm = reactive<Record<string, any>>({});

const basicConfigs = computed(() =>
  systemConfigs.value.filter(
    (item) =>
      item.configGroup === 'basic' && item.configKey !== 'platform_name',
  ),
);

const securityConfigs = computed(() =>
  systemConfigs.value.filter((item) => item.configGroup === 'security'),
);
const activeMapConfig = computed(() => mapConfigs.value[0]);

function toSystemFormValue(item: SystemConfigRecord) {
  if (item.valueType === 'boolean') {
    return item.configValue === 'true';
  }
  return item.configValue ?? '';
}

function assignForm(target: Record<string, any>, id: string, value: Record<string, any>) {
  target[id] = { ...value };
}

function syncSystemForm(items = systemConfigs.value) {
  items.forEach((item) => {
    systemForm[item.id] = toSystemFormValue(item);
  });
}

function syncStorageForm(items = storageConfigs.value) {
  items.forEach((item) => {
    assignForm(storageForm, item.id, {
      ...item,
      accessKeyId: '',
      accessKeySecret: '',
    });
  });
}

function syncMapForm(items = mapConfigs.value) {
  items.forEach((item) => {
    assignForm(mapForm, item.id, {
      ...item,
      jsApiKey: '',
      jsApiSecurityKey: '',
      reverseGeocodeKey: '',
    });
  });
}

function syncUploadForm(items = uploadPolicies.value) {
  items.forEach((item) => assignForm(uploadForm, item.id, item));
}

function syncExportForm(items = exportTemplates.value) {
  items.forEach((item) => assignForm(exportForm, item.id, item));
}

async function loadSystemConfigs() {
  systemConfigs.value = await getSystemConfigsApi();
  syncSystemForm();
}

async function loadStorageConfigs() {
  storageConfigs.value = await getStorageConfigsApi();
  syncStorageForm();
}

async function loadMapConfigs() {
  mapConfigs.value = await getMapConfigsApi();
  syncMapForm();
}

async function loadUploadPolicies() {
  uploadPolicies.value = await getUploadPoliciesApi();
  syncUploadForm();
}

async function loadExportTemplates() {
  exportTemplates.value = await getExportTemplatesApi();
  syncExportForm();
}

async function loadAllConfigs() {
  loading.value = true;
  try {
    await Promise.all([
      loadSystemConfigs(),
      loadStorageConfigs(),
      loadMapConfigs(),
      loadUploadPolicies(),
      loadExportTemplates(),
    ]);
  } finally {
    loading.value = false;
  }
}

function resetSystemCard(items: SystemConfigRecord[]) {
  syncSystemForm(items);
}

function resetStorageCard(item: StorageConfigRecord) {
  syncStorageForm([item]);
}

function resetMapCard(item: MapConfigRecord) {
  syncMapForm([item]);
}

function resetUploadCard(item: UploadPolicyRecord) {
  syncUploadForm([item]);
}

function resetExportCard(item: ExportTemplateRecord) {
  syncExportForm([item]);
}

async function saveSystemCard(items: SystemConfigRecord[], key: string) {
  savingKey.value = key;
  try {
    await Promise.all(
      items.map((item) =>
        updateSystemConfigApi(item.id, {
          configValue:
            item.valueType === 'boolean'
              ? String(Boolean(systemForm[item.id]))
              : String(systemForm[item.id] ?? ''),
          editable: item.editable,
          remark: item.remark || undefined,
        }),
      ),
    );
    message.success('配置已保存');
    await loadSystemConfigs();
  } finally {
    savingKey.value = '';
  }
}

async function saveStorageCard(item: StorageConfigRecord) {
  savingKey.value = item.id;
  try {
    await updateStorageConfigApi(item.id, storageForm[item.id]);
    message.success('配置已保存');
    await loadStorageConfigs();
  } finally {
    savingKey.value = '';
  }
}

async function saveMapCard(item: MapConfigRecord) {
  savingKey.value = item.id;
  try {
    await updateMapConfigApi(item.id, mapForm[item.id]);
    message.success('配置已保存');
    await loadMapConfigs();
  } finally {
    savingKey.value = '';
  }
}

async function saveUploadCard(item: UploadPolicyRecord) {
  savingKey.value = item.id;
  try {
    await updateUploadPolicyApi(item.id, uploadForm[item.id]);
    message.success('配置已保存');
    await loadUploadPolicies();
  } finally {
    savingKey.value = '';
  }
}

async function saveExportCard(item: ExportTemplateRecord) {
  savingKey.value = item.id;
  try {
    await updateExportTemplateApi(item.id, exportForm[item.id]);
    message.success('配置已保存');
    await loadExportTemplates();
  } finally {
    savingKey.value = '';
  }
}

onMounted(loadAllConfigs);
</script>

<template>
  <Page auto-content-height class="system-config-page">
    <a-spin :spinning="loading">
      <a-tabs v-model:active-key="activeTab" class="system-config-tabs">
        <a-tab-pane key="system" tab="基础配置">
          <div class="config-grid">
            <a-card size="small" title="平台信息">
              <template #extra>
                <a-space>
                  <a-button size="small" @click="resetSystemCard(basicConfigs)">
                    重置
                  </a-button>
                  <a-button
                    size="small"
                    type="primary"
                    :loading="savingKey === 'system-basic'"
                    @click="saveSystemCard(basicConfigs, 'system-basic')"
                  >
                    保存
                  </a-button>
                </a-space>
              </template>
              <a-empty v-if="basicConfigs.length === 0" description="暂无基础配置" />
              <a-form v-else layout="vertical">
                <a-form-item v-for="item in basicConfigs" :key="item.id" :label="item.configName">
                  <a-textarea
                    v-if="item.valueType === 'TEXT'"
                    v-model:value="systemForm[item.id]"
                    :disabled="!item.editable"
                    :rows="3"
                  />
                  <a-input v-else v-model:value="systemForm[item.id]" :disabled="!item.editable" />
                  <a-typography-text v-if="item.remark" class="config-remark">
                    {{ item.remark }}
                  </a-typography-text>
                </a-form-item>
              </a-form>
            </a-card>

            <a-card size="small" title="安全开关">
              <template #extra>
                <a-space>
                  <a-button size="small" @click="resetSystemCard(securityConfigs)">
                    重置
                  </a-button>
                  <a-button
                    size="small"
                    type="primary"
                    :loading="savingKey === 'system-security'"
                    @click="saveSystemCard(securityConfigs, 'system-security')"
                  >
                    保存
                  </a-button>
                </a-space>
              </template>
              <a-empty v-if="securityConfigs.length === 0" description="暂无安全配置" />
              <div v-else class="switch-list">
                <div v-for="item in securityConfigs" :key="item.id" class="switch-row">
                  <div>
                    <div class="config-label">{{ item.configName }}</div>
                    <div class="config-desc">{{ item.remark || item.configKey }}</div>
                  </div>
                  <a-switch
                    v-model:checked="systemForm[item.id]"
                    :disabled="!item.editable"
                    checked-children="启用"
                    un-checked-children="停用"
                  />
                </div>
              </div>
            </a-card>
          </div>
        </a-tab-pane>

        <a-tab-pane key="storage" tab="存储配置">
          <div class="config-grid">
            <a-card v-for="item in storageConfigs" :key="item.id" size="small">
              <template #title>
                <div class="card-title">
                  <IconifyIcon class="size-5" icon="lucide:hard-drive-upload" />
                  <span>{{ item.name }}</span>
                  <a-switch
                    v-if="storageForm[item.id]"
                    v-model:checked="storageForm[item.id].enabled"
                    checked-children="启用"
                    un-checked-children="停用"
                  />
                </div>
              </template>
              <template #extra>
                <a-space>
                  <a-button size="small" @click="resetStorageCard(item)">重置</a-button>
                  <a-button
                    size="small"
                    type="primary"
                    :loading="savingKey === item.id"
                    @click="saveStorageCard(item)"
                  >
                    保存
                  </a-button>
                </a-space>
              </template>
              <a-form v-if="storageForm[item.id]" :model="storageForm[item.id]" layout="vertical">
                <a-row :gutter="12">
                  <a-col :span="12"><a-form-item label="配置名称"><a-input v-model:value="storageForm[item.id].name" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="供应商"><a-input v-model:value="storageForm[item.id].provider" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="Endpoint"><a-input v-model:value="storageForm[item.id].endpoint" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="Region"><a-input v-model:value="storageForm[item.id].region" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="Bucket"><a-input v-model:value="storageForm[item.id].bucketName" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="通用上传目录"><a-input v-model:value="storageForm[item.id].uploadDir" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="AccessKey ID"><a-input v-model:value="storageForm[item.id].accessKeyId" placeholder="留空表示不修改" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="AccessKey Secret"><a-input-password v-model:value="storageForm[item.id].accessKeySecret" placeholder="留空表示不修改" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="原图目录"><a-input v-model:value="storageForm[item.id].originalDir" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="水印目录"><a-input v-model:value="storageForm[item.id].watermarkedDir" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="头像目录"><a-input v-model:value="storageForm[item.id].avatarDir" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="临时链接有效分钟"><a-input-number v-model:value="storageForm[item.id].presignedUrlMinutes" class="full-control" :min="1" /></a-form-item></a-col>
                </a-row>
              </a-form>
            </a-card>
          </div>
        </a-tab-pane>
        <a-tab-pane key="map" tab="地图配置">
          <div class="config-grid">
            <a-card v-if="activeMapConfig" :key="activeMapConfig.id" size="small">
              <template #title>
                <div class="card-title">
                  <IconifyIcon class="size-5" icon="lucide:map" />
                  <span>地图配置</span>
                  <a-switch
                    v-if="mapForm[activeMapConfig.id]"
                    v-model:checked="mapForm[activeMapConfig.id].enabled"
                    checked-children="启用"
                    un-checked-children="停用"
                  />
                </div>
              </template>
              <template #extra>
                <a-space>
                  <a-button size="small" @click="resetMapCard(activeMapConfig)">重置</a-button>
                  <a-button size="small" type="primary" :loading="savingKey === activeMapConfig.id" @click="saveMapCard(activeMapConfig)">保存</a-button>
                </a-space>
              </template>
              <a-form v-if="mapForm[activeMapConfig.id]" :model="mapForm[activeMapConfig.id]" layout="vertical">
                <a-row :gutter="12">
                  <a-col :span="12"><a-form-item label="逆地址解析 Key"><a-input v-model:value="mapForm[activeMapConfig.id].reverseGeocodeKey" placeholder="高德 WebService Key，留空表示不修改" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="JS API Key"><a-input v-model:value="mapForm[activeMapConfig.id].jsApiKey" placeholder="高德 JS API Key，留空表示不修改" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="JS API 安全密钥"><a-input-password v-model:value="mapForm[activeMapConfig.id].jsApiSecurityKey" placeholder="Security JS Code，留空表示不修改" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="默认缩放级别"><a-input-number v-model:value="mapForm[activeMapConfig.id].defaultZoom" class="full-control" :min="3" :max="20" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="默认经度"><a-input-number v-model:value="mapForm[activeMapConfig.id].defaultLongitude" class="full-control" :precision="6" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="默认纬度"><a-input-number v-model:value="mapForm[activeMapConfig.id].defaultLatitude" class="full-control" :precision="6" /></a-form-item></a-col>
                </a-row>
                <a-form-item label="备注">
                  <a-textarea v-model:value="mapForm[activeMapConfig.id].remark" :rows="3" />
                </a-form-item>
              </a-form>
            </a-card>
            <a-empty v-else description="暂无地图配置" />
          </div>
        </a-tab-pane>

        <a-tab-pane key="upload" tab="上传策略">
          <div class="config-grid">
            <a-card v-for="item in uploadPolicies" :key="item.id" size="small">
              <template #title>
                <div class="card-title">
                  <IconifyIcon class="size-5" icon="lucide:upload-cloud" />
                  <span>{{ item.name }}</span>
                  <a-switch
                    v-if="uploadForm[item.id]"
                    v-model:checked="uploadForm[item.id].enabled"
                    checked-children="启用"
                    un-checked-children="停用"
                  />
                </div>
              </template>
              <template #extra>
                <a-space>
                  <a-button size="small" @click="resetUploadCard(item)">重置</a-button>
                  <a-button size="small" type="primary" :loading="savingKey === item.id" @click="saveUploadCard(item)">保存</a-button>
                </a-space>
              </template>
              <a-form v-if="uploadForm[item.id]" :model="uploadForm[item.id]" layout="vertical">
                <a-row :gutter="12">
                  <a-col :span="12"><a-form-item label="策略名称"><a-input v-model:value="uploadForm[item.id].name" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="场景"><a-input v-model:value="uploadForm[item.id].scene" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="最多文件数量"><a-input-number v-model:value="uploadForm[item.id].maxFileCount" class="full-control" :min="1" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="单文件大小 MB"><a-input-number v-model:value="uploadForm[item.id].maxFileSizeMb" class="full-control" :min="1" /></a-form-item></a-col>
                </a-row>
                <a-form-item label="允许类型"><a-input v-model:value="uploadForm[item.id].allowedMimeTypes" /></a-form-item>
                <a-space>
                  <a-checkbox v-model:checked="uploadForm[item.id].compressEnabled">图片压缩</a-checkbox>
                </a-space>
              </a-form>
            </a-card>
          </div>
        </a-tab-pane>

        <a-tab-pane key="export" tab="导出配置">
          <div class="config-grid">
            <a-card v-for="item in exportTemplates" :key="item.id" size="small">
              <template #title>
                <div class="card-title">
                  <IconifyIcon class="size-5" icon="lucide:file-down" />
                  <span>{{ item.name }}</span>
                  <a-switch
                    v-if="exportForm[item.id]"
                    v-model:checked="exportForm[item.id].enabled"
                    checked-children="启用"
                    un-checked-children="停用"
                  />
                </div>
              </template>
              <template #extra>
                <a-space>
                  <a-button size="small" @click="resetExportCard(item)">重置</a-button>
                  <a-button size="small" type="primary" :loading="savingKey === item.id" @click="saveExportCard(item)">保存</a-button>
                </a-space>
              </template>
              <a-form v-if="exportForm[item.id]" :model="exportForm[item.id]" layout="vertical">
                <a-row :gutter="12">
                  <a-col :span="12"><a-form-item label="模板名称"><a-input v-model:value="exportForm[item.id].name" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="场景"><a-input v-model:value="exportForm[item.id].scene" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="文件保留天数"><a-input-number v-model:value="exportForm[item.id].fileRetentionDays" class="full-control" :min="1" /></a-form-item></a-col>
                </a-row>
                <a-form-item label="导出字段 JSON"><a-textarea v-model:value="exportForm[item.id].fieldsJson" :rows="6" /></a-form-item>
                <a-space>
                  <a-checkbox v-model:checked="exportForm[item.id].includeOriginalLinks">包含原图链接</a-checkbox>
                  <a-checkbox v-model:checked="exportForm[item.id].includeWatermarkedLinks">包含水印图链接</a-checkbox>
                </a-space>
              </a-form>
            </a-card>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </Page>
</template>

<style scoped>
.system-config-page {
  height: 100%;
}

.system-config-tabs {
  height: 100%;
}

.system-config-tabs :deep(.ant-tabs-content-holder),
.system-config-tabs :deep(.ant-tabs-content),
.system-config-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

.config-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  padding: 4px 0 16px;
}

.card-title {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.card-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-remark {
  display: block;
  margin-top: 4px;
  color: rgb(0 0 0 / 45%);
}

.switch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.switch-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}

.config-label {
  font-weight: 500;
}

.config-desc {
  margin-top: 4px;
  color: rgb(0 0 0 / 45%);
}

.full-control {
  width: 100%;
}
</style>
