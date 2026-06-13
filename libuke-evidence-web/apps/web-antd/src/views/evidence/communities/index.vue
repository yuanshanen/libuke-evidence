<script lang="ts" setup>
import type { CommunityRecord } from '#/api';
import type {
  OnActionClickParams,
  VxeGridListeners,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';
import { message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createCommunityApi,
  deleteCommunityApi,
  getCommunitiesApi,
  getRuntimeConfigApi,
  updateCommunityApi,
} from '#/api';

import { useColumns, useGridFormSchema } from './data';

type Coordinate = [number, number];

declare global {
  interface Window {
    AMap?: any;
    _AMapSecurityConfig?: {
      securityJsCode?: string;
    };
    __amapLoader?: Promise<void>;
  }
}

const DEFAULT_CENTER: Coordinate = [108.953, 34.265];
const BOUNDARY_FILL_COLOR = '#1677ff';
const BOUNDARY_FILL_OPACITY = 0.18;
const BOUNDARY_STROKE_COLOR = '#1677ff';
const BOUNDARY_STROKE_OPACITY = 0.9;
const BOUNDARY_STROKE_WEIGHT = 3;

const saving = ref(false);
const drawerOpen = ref(false);
const editingId = ref<string>();
const selectedCount = ref(0);
const mapContainerRef = ref<HTMLDivElement>();
const mapError = ref('');
const mapLoading = ref(false);
const selectingCenter = ref(false);
const drawingBoundary = ref(false);
const mapSearchKeyword = ref('');
const mapSearching = ref(false);
const mapSearchOptions = ref<{ label: string; poi: any; value: string }[]>([]);
const mapSearchOpen = ref(false);
const amapJsApiKey = ref<string>();
const amapJsApiSecurityKey = ref<string>();
const mapDefaultZoom = ref(17);

let mapInstance: any;
let mouseTool: any;
let boundaryPolygon: any;
let buildingLayer: any;
let placeSearch: any;
let mapClickHandler: ((event: any) => void) | undefined;

const form = reactive<{
  boundary: Coordinate[];
  center?: Coordinate;
  communityName: string;
  buildingColor: string;
  mapPitch: number;
  mapRotation: number;
  mapZoom: number;
  principalName: string;
  principalPhone: string;
  status: number;
}>({
  boundary: [],
  center: undefined,
  buildingColor: '#ffcc00',
  mapPitch: 58,
  mapRotation: -18,
  mapZoom: 17,
  communityName: '',
  principalName: '',
  principalPhone: '',
  status: 1,
});

const centerText = computed(() => {
  if (!form.center) {
    return '未选择';
  }
  return `${form.center[0].toFixed(6)}, ${form.center[1].toFixed(6)}`;
});

const boundaryText = computed(() => {
  if (form.boundary.length === 0) {
    return '未绘制';
  }
  return `${form.boundary.length} 个坐标点`;
});

const gridEvents: VxeGridListeners<CommunityRecord> = {
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
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getCommunitiesApi({
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
  } as VxeTableGridOptions<CommunityRecord>,
});

function updateSelectedCount() {
  selectedCount.value = gridApi.grid.getCheckboxRecords().length;
}

function onActionClick(e: OnActionClickParams<CommunityRecord>) {
  if (e.code === 'edit') {
    openEdit(e.row);
  }
  if (e.code === 'delete') {
    deleteCommunity(e.row.id);
  }
}

function resetForm() {
  form.communityName = '';
  form.principalName = '';
  form.principalPhone = '';
  form.status = 1;
  form.center = undefined;
  form.boundary = [];
  form.buildingColor = '#ffcc00';
  form.mapZoom = mapDefaultZoom.value;
  form.mapPitch = 58;
  form.mapRotation = -18;
}

function openCreate() {
  editingId.value = undefined;
  resetForm();
  drawerOpen.value = true;
}

function openEdit(record: CommunityRecord) {
  editingId.value = record.id;
  form.communityName = record.communityName || record.name;
  form.principalName = record.principalName ?? '';
  form.principalPhone = record.principalPhone ?? '';
  form.status = record.status ?? (record.enabled ? 1 : 0);
  form.center = record.center;
  form.boundary = record.boundary ?? [];
  form.buildingColor = record.buildingColor ?? '#ffcc00';
  form.mapZoom = record.mapZoom ?? mapDefaultZoom.value;
  form.mapPitch = record.mapPitch ?? 58;
  form.mapRotation = record.mapRotation ?? -18;
  drawerOpen.value = true;
}

function validatePhone(value: string) {
  if (!value.trim()) {
    return true;
  }
  return /^1[3-9]\d{9}$/.test(value.trim()) || /^(0\d{2,3}-?)?\d{7,8}(-\d{1,6})?$/.test(value.trim());
}

async function saveCommunity() {
  if (!form.communityName.trim()) {
    message.warning('请填写小区名称');
    return;
  }
  if (!form.center) {
    message.warning('请选择小区中心点');
    return;
  }
  if (form.boundary.length < 3) {
    message.warning('请绘制小区边界，且至少需要 3 个坐标点');
    return;
  }
  if (!validatePhone(form.principalPhone)) {
    message.warning('负责人联系方式格式不正确');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      boundary: form.boundary,
      center: form.center,
      communityName: form.communityName.trim(),
      buildingColor: form.buildingColor,
      mapZoom: form.mapZoom,
      mapPitch: form.mapPitch,
      mapRotation: form.mapRotation,
      principalName: form.principalName || undefined,
      principalPhone: form.principalPhone || undefined,
      status: form.status,
    };
    if (editingId.value) {
      await updateCommunityApi(editingId.value, payload);
      message.success('小区信息已保存');
    } else {
      await createCommunityApi(payload);
      message.success('小区已新增');
    }
    drawerOpen.value = false;
    gridApi.query();
  } finally {
    saving.value = false;
  }
}

async function deleteCommunity(communityId: string) {
  await deleteCommunityApi(communityId);
  message.success('已删除小区');
  gridApi.query();
}

async function batchDeleteCommunities() {
  const rows = gridApi.grid.getCheckboxRecords();
  if (rows.length === 0) {
    message.warning('请先选择要删除的小区');
    return;
  }
  await Promise.all(rows.map((row) => deleteCommunityApi(row.id)));
  message.success(`已删除 ${rows.length} 个小区`);
  selectedCount.value = 0;
  gridApi.query();
}

async function loadAmap() {
  if (window.AMap) {
    await loadAmapPlugins();
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
      )}&plugins=AMap.MouseTool,AMap.PlaceSearch`;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('高德地图加载失败'));
      document.head.append(script);
    });
  }
  await window.__amapLoader;
  await loadAmapPlugins();
}

function loadAmapPlugins() {
  return new Promise<void>((resolve) => {
    if (!window.AMap?.plugin) {
      resolve();
      return;
    }
    window.AMap.plugin(['AMap.MouseTool', 'AMap.PlaceSearch'], () => resolve());
  });
}

async function initMap() {
  mapError.value = '';
  await nextTick();
  if (!drawerOpen.value || !mapContainerRef.value || mapInstance) {
    return;
  }
  mapLoading.value = true;
  try {
    await loadAmap();
    const AMap = window.AMap;
    if (!AMap) {
      throw new Error('高德地图加载失败');
    }
    const center = form.center ?? DEFAULT_CENTER;
    buildingLayer = new AMap.Buildings({
      heightFactor: 2,
      zooms: [2, 20],
      zIndex: 10,
    });
    mapInstance = new AMap.Map(mapContainerRef.value, {
      center,
      features: ['bg', 'road', 'point', 'building'],
      pitch: form.mapPitch,
      rotation: form.mapRotation,
      showLabel: true,
      viewMode: '3D',
      zoom: form.mapZoom,
    });
    mapInstance.add(buildingLayer);
    mouseTool = new AMap.MouseTool(mapInstance);
    placeSearch = new AMap.PlaceSearch({
      city: '全国',
      pageSize: 10,
    });
    renderBoundary();
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '地图加载失败';
  } finally {
    mapLoading.value = false;
  }
}

function normalizeLngLat(lnglat: any): Coordinate {
  if (Array.isArray(lnglat)) {
    return [Number(lnglat[0]), Number(lnglat[1])];
  }
  return [Number(lnglat.lng), Number(lnglat.lat)];
}

function renderBoundary() {
  if (!mapInstance || !window.AMap) {
    return;
  }
  if (boundaryPolygon) {
    mapInstance.remove(boundaryPolygon);
    boundaryPolygon = undefined;
  }
  if (form.boundary.length < 3) {
    updateBuildingHighlight();
    return;
  }
  updateBuildingHighlight();
  if (form.center) {
    mapInstance.setCenter(form.center);
  }
  applyMapView();
}

function updateBuildingHighlight() {
  if (!buildingLayer?.setStyle) {
    return;
  }
  if (form.boundary.length < 3) {
    buildingLayer.setStyle({ areas: [], hideWithoutStyle: false });
    return;
  }
  buildingLayer.setStyle({
    areas: [
      {
        color1: form.buildingColor,
        color2: form.buildingColor,
        path: form.boundary,
        rejectTexture: true,
      },
    ],
    hideWithoutStyle: false,
  });
}

function onBuildingColorChange(event: Event) {
  const target = event.target as HTMLInputElement;
  if (!target.value) {
    return;
  }
  form.buildingColor = target.value;
  updateBuildingHighlight();
}

function mapPoiToOption(poi: any) {
  const district = poi.adname || poi.district || '';
  const address = typeof poi.address === 'string' ? poi.address : '';
  const detail = [district, address].filter(Boolean).join(' ');
  return {
    label: detail ? `${poi.name} · ${detail}` : poi.name,
    poi,
    value: `${poi.id || poi.name}-${poi.location?.lng}-${poi.location?.lat}`,
  };
}

function locateMapPoi(poi: any) {
  if (!mapInstance || !poi?.location) {
    return;
  }
  const center = normalizeLngLat(poi.location);
  form.center = center;
  mapSearchKeyword.value = poi.name;
  mapSearchOptions.value = [];
  mapInstance.setCenter(center);
  mapInstance.setZoom(Math.max(form.mapZoom, 17));
  message.success(`已定位到：${poi.name}`);
}

function searchMapPlace() {
  const keyword = mapSearchKeyword.value.trim();
  if (!keyword) {
    mapSearchOptions.value = [];
    mapSearchOpen.value = false;
    message.warning('请输入地点名称');
    return;
  }
  if (!mapInstance || !placeSearch || !window.AMap) {
    message.warning('地图搜索仍在加载，请稍后再试');
    return;
  }
  mapSearching.value = true;
  placeSearch.search(keyword, (status: string, result: any) => {
    mapSearching.value = false;
    if (status !== 'complete' || !result?.poiList?.pois?.length) {
      mapSearchOptions.value = [];
      mapSearchOpen.value = false;
      message.warning('没有找到匹配地点');
      return;
    }
    mapSearchOptions.value = result.poiList.pois.slice(0, 8).map(mapPoiToOption);
    mapSearchOpen.value = true;
  });
}

function onMapSearchSelect(value: string) {
  const option = mapSearchOptions.value.find((item) => item.value === value);
  if (option) {
    locateMapPoi(option.poi);
  }
}

function applyMapView() {
  if (!mapInstance) {
    return;
  }
  if (form.center) {
    mapInstance.setCenter(form.center);
  }
  mapInstance.setZoom(Number(form.mapZoom));
  mapInstance.setPitch?.(Number(form.mapPitch));
  mapInstance.setRotation?.(Number(form.mapRotation));
}

function saveCurrentMapView() {
  if (!mapInstance) {
    message.warning('地图仍在加载，请稍后再试');
    return;
  }
  const center = mapInstance.getCenter?.();
  if (center) {
    form.center = normalizeLngLat(center);
  }
  form.mapZoom = Math.round(Number(mapInstance.getZoom?.() ?? form.mapZoom));
  form.mapPitch = Math.round(Number(mapInstance.getPitch?.() ?? form.mapPitch));
  form.mapRotation = Math.round(Number(mapInstance.getRotation?.() ?? form.mapRotation));
  message.success('已锁定当前地图视角');
}

function startSelectCenter() {
  if (!mapInstance) {
    message.warning('地图仍在加载，请稍后再试');
    return;
  }
  selectingCenter.value = true;
  if (mapClickHandler) {
    mapInstance.off('click', mapClickHandler);
  }
  mapClickHandler = (event: any) => {
    form.center = normalizeLngLat(event.lnglat);
    selectingCenter.value = false;
    mapInstance.off('click', mapClickHandler);
    mapClickHandler = undefined;
    message.success('中心点已选择');
  };
  mapInstance.on('click', mapClickHandler);
}

function startDrawBoundary() {
  if (!mouseTool || !window.AMap) {
    message.warning('地图绘制工具仍在加载，请稍后再试');
    return;
  }
  drawingBoundary.value = true;
  mouseTool.close(true);
  mouseTool.off('draw', handleBoundaryDraw);
  mouseTool.polygon({
    fillColor: BOUNDARY_FILL_COLOR,
    fillOpacity: BOUNDARY_FILL_OPACITY,
    strokeColor: BOUNDARY_STROKE_COLOR,
    strokeOpacity: BOUNDARY_STROKE_OPACITY,
    strokeWeight: BOUNDARY_STROKE_WEIGHT,
  });
  mouseTool.on('draw', handleBoundaryDraw);
}

function handleBoundaryDraw(event: any) {
  drawingBoundary.value = false;
  mouseTool?.off('draw', handleBoundaryDraw);
  mouseTool?.close(false);
  const path = event.obj.getPath().map(normalizeLngLat);
  form.boundary = path;
  if (boundaryPolygon && boundaryPolygon !== event.obj) {
    mapInstance?.remove(boundaryPolygon);
  }
  boundaryPolygon = event.obj;
  if (boundaryPolygon && mapInstance) {
    mapInstance.remove(boundaryPolygon);
    boundaryPolygon = undefined;
  }
  renderBoundary();
  message.success('小区边界已绘制');
}

function clearBoundary() {
  form.boundary = [];
  drawingBoundary.value = false;
  mouseTool?.close(true);
  if (boundaryPolygon && mapInstance) {
    mapInstance.remove(boundaryPolygon);
    boundaryPolygon = undefined;
  }
  updateBuildingHighlight();
}

function cleanupMap() {
  selectingCenter.value = false;
  drawingBoundary.value = false;
  if (mapClickHandler && mapInstance) {
    mapInstance.off('click', mapClickHandler);
  }
  mapClickHandler = undefined;
  mouseTool?.off('draw', handleBoundaryDraw);
  mouseTool?.close(true);
  mouseTool = undefined;
  placeSearch = undefined;
  boundaryPolygon = undefined;
  buildingLayer = undefined;
  mapInstance?.destroy();
  mapInstance = undefined;
  if (mapContainerRef.value) {
    mapContainerRef.value.innerHTML = '';
  }
}

watch(drawerOpen, (open) => {
  if (open) {
    initMap();
  } else {
    cleanupMap();
  }
});

onMounted(() => {
  getRuntimeConfigApi().then((config) => {
    amapJsApiKey.value = config.map.jsApiKey;
    amapJsApiSecurityKey.value = config.map.jsApiSecurityKey;
    mapDefaultZoom.value = config.map.defaultZoom || 17;
  });
});

onBeforeUnmount(() => {
  cleanupMap();
});
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <a-space>
          <a-popconfirm
            v-if="selectedCount > 0"
            title="确认批量删除选中的小区？"
            @confirm="batchDeleteCommunities"
          >
            <a-button danger>
              <IconifyIcon class="size-4" icon="lucide:trash-2" />
              批量删除
            </a-button>
          </a-popconfirm>
          <a-button type="primary" @click="openCreate">
            <Plus class="size-5" />
            新增小区
          </a-button>
        </a-space>
      </template>
    </Grid>

    <a-drawer
      v-model:open="drawerOpen"
      :body-style="{ padding: 0 }"
      :closable="false"
      :title="null"
      width="calc(100vw - 240px)"
    >
      <div class="community-drawer">
        <div class="drawer-header">
          <div class="drawer-title-row">
            <a-button class="drawer-close-button" type="text" @click="drawerOpen = false">
              <IconifyIcon class="size-4" icon="lucide:x" />
            </a-button>
            <div class="drawer-title">{{ editingId ? '编辑小区' : '新增小区' }}</div>
            <a-switch
              v-model:checked="form.status"
              checked-children="启用"
              :checked-value="1"
              un-checked-children="禁用"
              :un-checked-value="0"
            />
          </div>
          <a-space class="drawer-actions">
            <a-button :loading="saving" type="primary" @click="saveCommunity">
              确定
            </a-button>
          </a-space>
        </div>

        <div class="drawer-body">
          <a-form :model="form" layout="vertical">
            <section class="form-section">
              <div class="section-title">基础信息</div>
              <a-form-item label="小区名称" required>
                <a-input v-model:value="form.communityName" maxlength="100" placeholder="请输入小区名称" />
              </a-form-item>
              <div class="form-grid two-columns">
                <a-form-item label="负责人姓名">
                  <a-input v-model:value="form.principalName" maxlength="100" placeholder="请输入负责人姓名" />
                </a-form-item>
                <a-form-item label="负责人联系方式">
                  <a-input v-model:value="form.principalPhone" maxlength="50" placeholder="手机号或固定电话" />
                </a-form-item>
              </div>
            </section>

            <section class="form-section">
              <div class="section-title">地图定位</div>
              <div class="location-row">
                <a-form-item class="location-field" label="小区中心点" required>
                  <div class="coordinate-display">{{ centerText }}</div>
                </a-form-item>
                <a-button type="primary" ghost @click="startSelectCenter">
                  <IconifyIcon class="size-4" icon="lucide:map-pin" />
                  {{ selectingCenter ? '在地图上点击位置' : '选择中心点' }}
                </a-button>
              </div>
            </section>

            <section class="form-section map-section">
              <div class="map-section-head">
                <div class="map-title-line">
                  <span class="section-title map-section-title">小区范围</span>
                  <span class="map-meta">
                    边界：{{ boundaryText }} · 3D 楼栋高亮预览
                  </span>
                </div>
                <a-space class="map-actions">
                  <a-auto-complete
                    v-model:value="mapSearchKeyword"
                    class="map-search-input"
                    v-model:open="mapSearchOpen"
                    :options="mapSearchOptions"
                    placeholder="搜索小区、道路或地标"
                    @select="onMapSearchSelect"
                  >
                    <template #default>
                      <a-input
                        v-model:value="mapSearchKeyword"
                        placeholder="搜索小区、道路或地标"
                        @press-enter="searchMapPlace()"
                      >
                        <template #suffix>
                          <IconifyIcon
                            class="map-search-icon"
                            :class="{ 'is-loading': mapSearching }"
                            icon="lucide:search"
                            @click.stop="searchMapPlace()"
                          />
                        </template>
                      </a-input>
                    </template>
                    <template #option="{ label }">
                      <div class="map-search-option">{{ label }}</div>
                    </template>
                  </a-auto-complete>
                  <a-button class="map-view-save-button" @click="saveCurrentMapView">
                    <IconifyIcon class="size-4" icon="lucide:scan-eye" />
                    锁定当前视角
                  </a-button>
                  <a-button class="map-tool-button color-tool-button">
                    <span
                      class="building-color-icon"
                      :style="{ backgroundColor: form.buildingColor }"
                    ></span>
                    楼栋颜色
                    <input
                      class="color-input-overlay"
                      type="color"
                      :value="form.buildingColor"
                      @change="onBuildingColorChange"
                    />
                  </a-button>
                  <a-button class="map-tool-button" @click="startDrawBoundary">
                    <IconifyIcon class="size-4" icon="lucide:pen-line" />
                    {{ drawingBoundary ? '正在绘制' : '绘制边界' }}
                  </a-button>
                  <a-button class="map-tool-button" danger ghost @click="clearBoundary">
                    <IconifyIcon class="size-4" icon="lucide:eraser" />
                    清除边界
                  </a-button>
                </a-space>
              </div>
              <a-alert
                v-if="selectingCenter"
                class="map-tip"
                message="请在地图上点击小区中心位置，可重复选择。"
                show-icon
                type="info"
              />
              <a-spin :spinning="mapLoading">
                <a-alert v-if="mapError" :message="mapError" show-icon type="warning" />
                <div v-show="!mapError" ref="mapContainerRef" class="community-map"></div>
              </a-spin>
            </section>
          </a-form>
        </div>

      </div>
    </a-drawer>
  </Page>
</template>

<style scoped>
.community-drawer {
  display: flex;
  height: 100%;
  min-height: 100vh;
  flex-direction: column;
  background: #f8fafc;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.drawer-close-button {
  width: 28px;
  height: 28px;
  padding: 0;
}

.drawer-title {
  color: #111827;
  font-size: 18px;
  font-weight: 600;
}

.drawer-actions {
  margin-left: auto;
}

.drawer-body {
  flex: 1;
  padding: 16px 24px 24px;
  overflow: auto;
}

.form-section {
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.section-title {
  margin-bottom: 16px;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.location-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.location-field {
  flex: 1;
  margin-bottom: 0;
}

.coordinate-display {
  min-height: 32px;
  padding: 5px 11px;
  color: #374151;
  background: #f9fafb;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.map-section {
  border-color: #bfdbfe;
}

.map-section-head {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 12px;
}

.map-title-line {
  display: flex;
  align-items: baseline;
  flex: none;
  gap: 10px;
  min-width: 0;
}

.map-section-title {
  margin-bottom: 0;
}

.map-meta {
  color: #6b7280;
  font-size: 13px;
  white-space: nowrap;
}

.map-tip {
  margin-bottom: 12px;
}

.map-actions {
  flex: 1;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.map-view-save-button {
  width: 134px;
  justify-content: center;
}

.map-search-input {
  width: 320px;
}

.map-search-icon {
  color: #1677ff;
  cursor: pointer;
  transition: color 0.2s ease, transform 0.2s ease;
}

.map-search-icon:hover {
  color: #0958d9;
  transform: scale(1.08);
}

.map-search-icon.is-loading {
  color: #94a3b8;
  pointer-events: none;
}

.map-search-option {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-tool-button {
  width: 126px;
  justify-content: center;
}

.color-tool-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
}

.color-input-overlay {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.building-color-icon {
  flex: none;
  width: 16px;
  height: 16px;
  border: 1px solid rgb(0 0 0 / 18%);
  border-radius: 3px;
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 45%);
}

.community-map {
  width: 100%;
  height: 560px;
  overflow: hidden;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  box-shadow: inset 0 0 0 1px rgb(22 119 255 / 8%);
}

@media (max-width: 768px) {
  .form-grid,
  .map-section-head {
    grid-template-columns: 1fr;
  }

  .form-grid {
    display: block;
  }

  .location-row,
  .map-section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .map-title-line {
    flex-wrap: wrap;
  }

  .map-meta {
    white-space: normal;
  }

  .map-actions {
    flex-wrap: wrap;
    justify-content: stretch;
  }

  .map-view-save-button {
    width: 100%;
  }

  .map-search-input {
    width: 100%;
  }

  .map-tool-button {
    width: 100%;
  }

  .community-map {
    height: 440px;
  }
}
</style>
