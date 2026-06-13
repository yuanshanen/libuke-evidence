<script lang="ts" setup>
import type { ReportEventRecord, ReportRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeGridListeners,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteReportApi,
  exportReportsApi,
  getCommunitiesApi,
  getReportEventsApi,
  getReportCategoriesApi,
  getReportDetailApi,
  getReportsApi,
  getRuntimeConfigApi,
  updateReportStatusApi,
} from '#/api';
import { categoryOptions, statusMeta, statusOptions } from '#/views/evidence/constants';

import { useColumns, useGridFormSchema } from './data';

declare global {
  interface Window {
    AMap?: any;
    _AMapSecurityConfig?: {
      securityJsCode?: string;
    };
    __amapLoader?: Promise<void>;
  }
}

const amapJsApiKey = ref<string>();
const amapJsApiSecurityKey = ref<string>();
const mapDefaultZoom = ref(17);

interface CommunityOption {
  center?: [number, number];
  label: string;
  mapPitch?: number;
  mapRotation?: number;
  mapZoom?: number;
  value: string;
}

const detailLoading = ref(false);
const exporting = ref(false);
const processLoading = ref(false);
const saving = ref(false);
const detail = ref<ReportRecord>();
const reportEvents = ref<ReportEventRecord[]>([]);
const processing = ref<ReportRecord>();
const drawerOpen = ref(false);
const processOpen = ref(false);
const batchProcessMode = ref(false);
const selectedCount = ref(0);
const mapContainerRef = ref<HTMLDivElement>();
const mapError = ref('');
const mapLoading = ref(false);
const communities = ref<CommunityOption[]>([]);
const categories = ref<{ label: string; value: string }[]>([
  ...categoryOptions,
]);
const route = useRoute();
const openedRouteReportId = ref<string>();

const hasSelection = computed(() => selectedCount.value > 0);

const processForm = reactive({
  adminNote: '',
  status: 'pending',
});

const attachmentList = computed(() => detail.value?.attachments ?? []);
const hasCoordinate = computed(
  () => detail.value?.longitude != null && detail.value?.latitude != null,
);
const coordinateText = computed(() => {
  if (!hasCoordinate.value) {
    return '-';
  }
  const longitude = Number(detail.value?.longitude);
  const latitude = Number(detail.value?.latitude);
  const longitudeDirection = longitude >= 0 ? '东经' : '西经';
  const latitudeDirection = latitude >= 0 ? '北纬' : '南纬';
  return `${longitudeDirection} ${Math.abs(longitude)}, ${latitudeDirection} ${Math.abs(latitude)}`;
});

const gridEvents: VxeGridListeners<ReportRecord> = {
  checkboxAll: updateSelectedCount,
  checkboxChange: updateSelectedCount,
};

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    fieldMappingTime: [['submittedDate', ['startDate', 'endDate']]],
    schema: useGridFormSchema(communities.value, categories.value),
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
          const result = await getReportsApi({
            category: formValues.category,
            communityId: formValues.communityId,
            endDate: formValues.endDate,
            keyword: formValues.keyword,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            startDate: formValues.startDate,
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
  } as VxeTableGridOptions<ReportRecord>,
});

function updateSelectedCount() {
  selectedCount.value = gridApi.grid.getCheckboxRecords().length;
}

function onActionClick(e: OnActionClickParams<ReportRecord>) {
  if (e.code === 'detail') {
    openDetail(e.row);
  }
  if (e.code === 'process') {
    openProcess(e.row);
  }
  if (e.code === 'delete') {
    deleteReport(e.row.id);
  }
}

async function loadCommunities() {
  const result = await getCommunitiesApi({ pageNo: 1, pageSize: 200 });
  communities.value.splice(
    0,
    communities.value.length,
    ...result.records.map((item) => ({
      center: item.center,
      label: item.name,
      mapPitch: item.mapPitch,
      mapRotation: item.mapRotation,
      mapZoom: item.mapZoom,
      value: item.id,
    })),
  );
}

function findCommunityView(record: ReportRecord) {
  return communities.value.find((item) => item.value === record.communityId);
}

async function loadCategories() {
  const result = await getReportCategoriesApi({ enabled: true });
  categories.value.splice(
    0,
    categories.value.length,
    ...result.map((item) => ({
      label: item.name,
      value: item.name,
    })),
  );
}

async function openDetail(record: ReportRecord) {
  drawerOpen.value = true;
  detailLoading.value = true;
  resetMap();
  try {
    detail.value = await getReportDetailApi(record.id);
    reportEvents.value = await getReportEventsApi(record.id);
    await renderMap();
  } finally {
    detailLoading.value = false;
  }
}

async function openDetailById(reportId: string) {
  if (!reportId || openedRouteReportId.value === reportId) {
    return;
  }
  openedRouteReportId.value = reportId;
  drawerOpen.value = true;
  detailLoading.value = true;
  resetMap();
  try {
    detail.value = await getReportDetailApi(reportId);
    reportEvents.value = await getReportEventsApi(reportId);
    await renderMap();
  } finally {
    detailLoading.value = false;
  }
}

function normalizeRouteReportId(value: unknown) {
  if (Array.isArray(value)) {
    return value[0];
  }
  return typeof value === 'string' ? value : undefined;
}

function eventTitle(event: ReportEventRecord) {
  if (event.eventType === 'created') {
    return '业主提交问题上报';
  }
  if (event.eventType === 'note_updated') {
    return '补充处置说明';
  }
  if (event.fromStatus && event.toStatus) {
    return `${statusMeta(event.fromStatus).label} → ${statusMeta(event.toStatus).label}`;
  }
  return event.content || '状态更新';
}

function eventDescription(event: ReportEventRecord) {
  const operator = event.operatorName || (event.operatorType === 'admin' ? '后台管理员' : '业主用户');
  return [operator, event.content].filter(Boolean).join('：');
}

function eventColor(event: ReportEventRecord) {
  if (event.eventType === 'created') {
    return 'blue';
  }
  if (event.toStatus === 'resolved' || event.toStatus === 'closed') {
    return 'green';
  }
  if (event.toStatus === 'invalid' || event.toStatus === 'duplicate') {
    return 'red';
  }
  return 'blue';
}

async function openProcess(record: ReportRecord) {
  batchProcessMode.value = false;
  processOpen.value = true;
  processLoading.value = true;
  try {
    processing.value = await getReportDetailApi(record.id);
    processForm.status = processing.value.status;
    processForm.adminNote = processing.value.adminNote ?? '';
  } finally {
    processLoading.value = false;
  }
}

function openBatchProcess() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要处理的记录');
    return;
  }
  processing.value = undefined;
  batchProcessMode.value = true;
  processForm.status = 'processing';
  processForm.adminNote = '';
  processOpen.value = true;
}

async function saveProcess() {
  saving.value = true;
  try {
    if (batchProcessMode.value) {
      const rows = gridApi.grid.getCheckboxRecords();
      if (rows.length === 0) {
        message.warning('请先选择要处理的记录');
        return;
      }
      await Promise.all(
        rows.map((row) =>
          updateReportStatusApi(row.id, {
            adminNote: processForm.adminNote || undefined,
            status: processForm.status,
          }),
        ),
      );
      message.success(`已处理 ${rows.length} 条记录`);
      selectedCount.value = 0;
    } else {
      if (!processing.value) return;
      processing.value = await updateReportStatusApi(processing.value.id, {
        adminNote: processForm.adminNote || undefined,
        status: processForm.status,
      });
      if (detail.value?.id === processing.value.id) {
        detail.value = processing.value;
        reportEvents.value = await getReportEventsApi(processing.value.id);
        await renderMap();
      }
      message.success('处理信息已保存');
    }
    processOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function deleteReport(reportId: string) {
  await deleteReportApi(reportId);
  message.success('已删除记录');
  gridApi.query();
}

async function batchDeleteReports() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要删除的记录');
    return;
  }
  await Promise.all(rows.map((row) => deleteReportApi(row.id)));
  message.success(`已删除 ${rows.length} 条记录`);
  selectedCount.value = 0;
  gridApi.query();
}

function currentExportFileName() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  const timestamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  return `问题记录_${timestamp}.xlsx`;
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

async function exportReports() {
  exporting.value = true;
  try {
    const rows = gridApi.grid.getCheckboxRecords();
    const formValues = await gridApi.formApi.getValues();
    const params = rows.length > 0
      ? { ids: rows.map((row) => row.id).join(',') }
      : {
          category: formValues.category,
          communityId: formValues.communityId,
          endDate: formValues.endDate,
          keyword: formValues.keyword,
          startDate: formValues.startDate,
          status: formValues.status,
    };
    const blob = await exportReportsApi(params);
    downloadBlob(blob, currentExportFileName());
    message.success(rows.length > 0 ? `已导出 ${rows.length} 条记录` : '已导出当前筛选结果');
  } finally {
    exporting.value = false;
  }
}
async function loadAmap() {
  if (window.AMap) {
    return;
  }
  if (!amapJsApiKey.value) {
    const config = await getRuntimeConfigApi();
    amapJsApiKey.value = config.map.jsApiKey;
    amapJsApiSecurityKey.value = config.map.jsApiSecurityKey;
    mapDefaultZoom.value = config.map.defaultZoom || 17;
  }
  if (!amapJsApiKey.value) {
    throw new Error('请先配置高德 JS API Key');
  }
  const mapKey = amapJsApiKey.value;
  if (!window.__amapLoader) {
    window.__amapLoader = new Promise((resolve, reject) => {
      if (amapJsApiSecurityKey.value) {
        window._AMapSecurityConfig = {
          securityJsCode: amapJsApiSecurityKey.value,
        };
      }
      const script = document.createElement('script');
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(
        mapKey,
      )}`;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('高德地图加载失败'));
      document.head.append(script);
    });
  }
  await window.__amapLoader;
}

function resetMap() {
  mapError.value = '';
  mapLoading.value = false;
  if (mapContainerRef.value) {
    mapContainerRef.value.innerHTML = '';
  }
}

async function renderMap() {
  mapError.value = '';
  if (!detail.value || !hasCoordinate.value) {
    resetMap();
    return;
  }
  await nextTick();
  if (!mapContainerRef.value) {
    return;
  }
  mapLoading.value = true;
  try {
    await loadAmap();
    const AMap = window.AMap;
    if (!AMap) {
      throw new Error('高德地图加载失败');
    }
    const position = [
      Number(detail.value.longitude),
      Number(detail.value.latitude),
    ] as [number, number];
    const communityView = findCommunityView(detail.value);
    const center = communityView?.center ?? position;
    mapContainerRef.value.innerHTML = '';
    const map = new AMap.Map(mapContainerRef.value, {
      center,
      features: ['bg', 'road', 'point', 'building'],
      pitch: communityView?.mapPitch ?? 58,
      rotation: communityView?.mapRotation ?? -18,
      viewMode: '3D',
      zoom: communityView?.mapZoom ?? mapDefaultZoom.value,
    });
    new AMap.Marker({
      map,
      position,
    });
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '地图加载失败';
  } finally {
    mapLoading.value = false;
  }
}

onMounted(async () => {
  getRuntimeConfigApi().then((config) => {
    amapJsApiKey.value = config.map.jsApiKey;
    amapJsApiSecurityKey.value = config.map.jsApiSecurityKey;
    mapDefaultZoom.value = config.map.defaultZoom || 17;
  });
  await Promise.all([loadCommunities(), loadCategories()]);
  await openDetailById(normalizeRouteReportId(route.query.reportId) ?? '');
});

watch(
  () => route.query.reportId,
  (reportId) => {
    openDetailById(normalizeRouteReportId(reportId) ?? '');
  },
);
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-actions>
        <a-button :loading="exporting" @click="exportReports">
          <IconifyIcon class="size-4" icon="lucide:download" />
          导出
        </a-button>
      </template>
      <template #toolbar-tools>
        <div class="toolbar-batch-actions" :class="{ visible: hasSelection }">
          <a-space>
            <a-button
              :disabled="!hasSelection"
              type="primary"
              @click="openBatchProcess"
            >
              <IconifyIcon class="size-4" icon="lucide:check-check" />
              批量处理
            </a-button>
            <a-popconfirm
              title="确认批量删除选中的记录？"
              @confirm="batchDeleteReports"
            >
              <a-button :disabled="!hasSelection" danger>
                <IconifyIcon class="size-4" icon="lucide:trash-2" />
                批量删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </template>
    </Grid>

    <a-drawer v-model:open="drawerOpen" title="记录详情" width="960">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div class="report-detail">
            <a-alert
              message="业主上报内容用于问题定位和处理，后台仅维护处理状态与备注。"
              show-icon
              type="info"
            />

            <a-descriptions bordered :column="2" size="small">
              <a-descriptions-item label="记录编号">
                {{ detail.reportNo }}
              </a-descriptions-item>
              <a-descriptions-item label="当前状态">
                {{ statusMeta(detail.status).label }}
              </a-descriptions-item>
              <a-descriptions-item label="提交时间">
                {{ detail.submittedAt }}
              </a-descriptions-item>
              <a-descriptions-item label="小区">
                {{ detail.communityName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="问题大类" :span="2">
                {{ detail.category }}
              </a-descriptions-item>
              <a-descriptions-item label="常见问题" :span="2">
                {{ detail.subCategory }}
              </a-descriptions-item>
              <a-descriptions-item label="用户 openid" :span="2">
                {{ detail.openid || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="取证人信息">
                {{ detail.witnessInfo || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="附件数量">
                {{ detail.imageCount || 0 }}
              </a-descriptions-item>
              <a-descriptions-item label="当前位置" :span="2">
                {{ detail.locationAddress || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="经纬度" :span="2">
                {{ coordinateText }}
              </a-descriptions-item>
              <a-descriptions-item label="用户备注" :span="2">
                {{ detail.remark || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="后台备注" :span="2">
                {{ detail.adminNote || '-' }}
              </a-descriptions-item>
            </a-descriptions>

            <a-card title="处理时间线" size="small">
              <a-empty v-if="reportEvents.length === 0" description="暂无处置记录" />
              <a-timeline v-else>
                <a-timeline-item
                  v-for="event in reportEvents"
                  :key="event.id"
                  :color="eventColor(event)"
                >
                  <div class="timeline-title">{{ eventTitle(event) }}</div>
                  <div class="timeline-meta">{{ event.createdAt }}</div>
                  <div v-if="eventDescription(event)" class="timeline-content">
                    {{ eventDescription(event) }}
                  </div>
                </a-timeline-item>
              </a-timeline>
            </a-card>

            <a-card title="地理位置" size="small">
              <a-spin :spinning="mapLoading">
                <a-empty v-if="!hasCoordinate" description="暂无经纬度信息" />
                <a-alert
                  v-else-if="mapError"
                  type="warning"
                  show-icon
                  :message="mapError"
                />
                <div
                  v-show="hasCoordinate && !mapError"
                  ref="mapContainerRef"
                  class="map-view"
                ></div>
              </a-spin>
            </a-card>

            <a-card title="附件" size="small">
              <a-empty v-if="attachmentList.length === 0" />
              <a-image-preview-group v-else>
                <div class="image-grid">
                  <a-image
                    v-for="item in attachmentList"
                    :key="item.id"
                    :src="item.url"
                    :height="150"
                    class="w-full object-cover"
                  />
                </div>
              </a-image-preview-group>
            </a-card>
          </div>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="processOpen"
      :confirm-loading="saving"
      :title="batchProcessMode ? '批量处理记录' : '处理记录'"
      width="560px"
      @ok="saveProcess"
    >
      <a-spin :spinning="processLoading">
        <a-alert
          v-if="batchProcessMode"
          class="mb-4"
          :message="`将对已选中的 ${selectedCount} 条记录应用相同的处理状态和后台备注。`"
          show-icon
          type="info"
        />
        <a-form :model="processForm" layout="vertical">
          <a-form-item label="处理状态" required>
            <a-select v-model:value="processForm.status" :options="statusOptions" />
          </a-form-item>
          <a-form-item label="后台备注">
            <a-textarea
              v-model:value="processForm.adminNote"
              :rows="4"
              maxlength="500"
              show-count
            />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </Page>
</template>

<style scoped>
.toolbar-batch-actions {
  min-width: 196px;
  opacity: 0;
  pointer-events: none;
}

.toolbar-batch-actions.visible {
  opacity: 1;
  pointer-events: auto;
}

.report-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-title {
  color: #111827;
  font-size: 14px;
  font-weight: 650;
}

.timeline-meta {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.timeline-content {
  margin-top: 4px;
  color: #334155;
  font-size: 13px;
}

.map-view {
  width: 100%;
  height: 320px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 768px) {
  .image-grid {
    grid-template-columns: 1fr;
  }
}
</style>
