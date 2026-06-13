<script lang="ts" setup>
import type {
  CommunityRecord,
  DashboardCommunityMapData,
  ReportMapPointRecord,
} from '#/api';

import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { IconifyIcon } from '@vben/icons';

import { message } from 'ant-design-vue';

import {
  createReportStreamSource,
  getDashboardCommunityMapApi,
  getDashboardMapCommunitiesApi,
  getReportMapPointsApi,
  getRuntimeConfigApi,
  updateReportStatusApi,
} from '#/api';

declare global {
  interface Window {
    AMap?: any;
    _AMapSecurityConfig?: { securityJsCode?: string };
    __amapLoader?: Promise<void>;
  }
}

const DEFAULT_CENTER: [number, number] = [108.953, 34.265];
const REPORT_CLUSTER_THRESHOLD = 12;
const NEW_REPORT_HIGHLIGHT_DURATION = 5000;
const REPORT_PANEL_COLLAPSED_KEY = 'evidence.dashboard.reportPanelCollapsed';
const REPORT_CARDS_VISIBLE_KEY = 'evidence.dashboard.reportCardsVisible';

const loading = ref(false);
const mapLoading = ref(false);
const mapError = ref('');
const mapContainerRef = ref<HTMLDivElement>();
const communities = ref<CommunityRecord[]>([]);
const selectedCommunityId = ref<string>();
const mapData = ref<DashboardCommunityMapData>();
const reportPanelCollapsed = ref(readReportPanelCollapsed());
const reportCardsVisible = ref(readReportCardsVisible());
const mapFullscreen = ref(false);
const reportMapPoints = ref<ReportMapPointRecord[]>([]);
const reportFilterMode = ref<'all' | 'overdue' | 'today'>('all');
const reportCategoryFilter = ref('all');
const newReportIds = ref<Set<string>>(new Set());
const quickProcessingIds = ref<Set<string>>(new Set());
const amapJsApiKey = ref<string>();
const amapJsApiSecurityKey = ref<string>();
const router = useRouter();

let mapInstance: any;
let buildingLayer: any;
let mapContextMenu: any;
let reportCluster: any;
let reportCardMarkers: any[] = [];
let reportCardMarkerMap = new Map<string, { marker: any; point: ReportMapPointRecord }>();
let reportMarkers: any[] = [];
let mapPointRefreshTimer: number | undefined;
let reportEventSource: EventSource | undefined;

const communityOptions = computed(() =>
  communities.value.map((item) => ({ label: item.name, value: item.id })),
);

const allFilterReportCount = computed(() => filterReportPointsByCategory(reportMapPoints.value).length);

const todayFilterReportCount = computed(() =>
  filterReportPointsByCategory(reportMapPoints.value).filter((point) => reportPointLevel(point) === 'today').length,
);

const overdueFilterReportCount = computed(() =>
  filterReportPointsByCategory(reportMapPoints.value).filter((point) => reportPointLevel(point) === 'overdue').length,
);

const reportCategoryOptions = computed(() => {
  const categories = [...new Set(reportMapPoints.value.map((point) => point.category).filter(Boolean))];
  return [
    { label: '全部分类', value: 'all' },
    ...categories.map((category) => ({ label: category, value: category })),
  ];
});

const filteredReportMapPoints = computed(() =>
  filterReportPointsByCategory(reportMapPoints.value).filter((point) => {
    const level = reportPointLevel(point);
    return reportFilterMode.value === 'all' || level === reportFilterMode.value;
  }),
);

function filterReportPointsByCategory(points: ReportMapPointRecord[]) {
  return points.filter((point) => reportCategoryFilter.value === 'all' || point.category === reportCategoryFilter.value);
}

function readReportPanelCollapsed() {
  if (typeof window === 'undefined') {
    return true;
  }
  const storedValue = window.localStorage.getItem(REPORT_PANEL_COLLAPSED_KEY);
  return storedValue == null ? true : storedValue === 'true';
}

function setReportPanelCollapsed(collapsed: boolean) {
  reportPanelCollapsed.value = collapsed;
  window.localStorage.setItem(REPORT_PANEL_COLLAPSED_KEY, String(collapsed));
}

function readReportCardsVisible() {
  if (typeof window === 'undefined') {
    return true;
  }
  const storedValue = window.localStorage.getItem(REPORT_CARDS_VISIBLE_KEY);
  return storedValue == null ? true : storedValue === 'true';
}

function setReportCardsVisible(visible: boolean) {
  reportCardsVisible.value = visible;
  window.localStorage.setItem(REPORT_CARDS_VISIBLE_KEY, String(visible));
}

async function loadAmap() {
  if (window.AMap) {
    return;
  }
  if (!amapJsApiKey.value) {
    const config = await getRuntimeConfigApi();
    amapJsApiKey.value = config.map.jsApiKey;
    amapJsApiSecurityKey.value = config.map.jsApiSecurityKey;
  }
  if (!amapJsApiKey.value) {
    throw new Error('请先在系统配置中配置高德 JS API Key');
  }
  if (!window.__amapLoader) {
    window.__amapLoader = new Promise((resolve, reject) => {
      if (amapJsApiSecurityKey.value) {
        window._AMapSecurityConfig = {
          securityJsCode: amapJsApiSecurityKey.value,
        };
      }
      const script = document.createElement('script');
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(
        amapJsApiKey.value!,
      )}&plugin=AMap.MarkerCluster`;
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
    window.AMap.plugin(['AMap.MarkerCluster'], () => resolve());
  });
}

function cleanupMap() {
  stopMapPointPolling();
  stopReportStream();
  clearNewReportHints();
  clearReportMarkers();
  clearReportCardMarkers();
  mapContextMenu?.close?.();
  mapContextMenu = undefined;
  buildingLayer = undefined;
  if (mapInstance) {
    mapInstance.destroy();
  }
  mapInstance = undefined;
}

function clearReportMarkers() {
  reportCluster?.clearMarkers?.();
  reportCluster?.setMap?.(null);
  reportCluster = undefined;
  if (mapInstance && reportMarkers.length > 0) {
    mapInstance.remove(reportMarkers);
  }
  reportMarkers = [];
}

function clearReportCardMarkers() {
  if (mapInstance && reportCardMarkers.length > 0) {
    mapInstance.remove(reportCardMarkers);
  }
  reportCardMarkers = [];
  reportCardMarkerMap = new Map<string, { marker: any; point: ReportMapPointRecord }>();
}

function stopMapPointPolling() {
  if (mapPointRefreshTimer) {
    window.clearInterval(mapPointRefreshTimer);
    mapPointRefreshTimer = undefined;
  }
}

function startMapPointPolling() {
  stopMapPointPolling();
  mapPointRefreshTimer = window.setInterval(() => {
    loadReportMapPoints();
  }, 30_000);
}

function stopReportStream() {
  reportEventSource?.close();
  reportEventSource = undefined;
}

function startReportStream() {
  stopReportStream();
  if (!selectedCommunityId.value) {
    return;
  }
  reportEventSource = createReportStreamSource(selectedCommunityId.value);
  reportEventSource.addEventListener('report-created', (event) => {
    void handleReportCreatedEvent(event);
  });
}

function clearNewReportHints() {
  newReportIds.value = new Set();
}

async function handleReportCreatedEvent(event: Event) {
  const reportId = parseReportCreatedId(event);
  if (reportId) {
    markNewReport(reportId);
  }
  await Promise.all([loadReportMapPoints(), refreshCommunityMapStats()]);
}

function parseReportCreatedId(event: Event) {
  const messageEvent = event as MessageEvent<string>;
  if (!messageEvent.data) {
    return undefined;
  }
  try {
    const data = JSON.parse(messageEvent.data) as { reportId?: number | string };
    return data.reportId == null ? undefined : String(data.reportId);
  } catch {
    return undefined;
  }
}

function markNewReport(reportId: string) {
  newReportIds.value = new Set([...newReportIds.value, reportId]);
  window.setTimeout(() => {
    const nextIds = new Set(newReportIds.value);
    nextIds.delete(reportId);
    newReportIds.value = nextIds;
  }, NEW_REPORT_HIGHLIGHT_DURATION);
}

function isNewReport(point: ReportMapPointRecord) {
  return newReportIds.value.has(point.reportId);
}

function isQuickProcessing(point: ReportMapPointRecord) {
  return quickProcessingIds.value.has(point.reportId);
}

function setReportFilterMode(mode: 'all' | 'overdue' | 'today') {
  reportFilterMode.value = mode;
  clearReportCardMarkers();
  renderReportMarkers();
}

function onReportCategoryChange() {
  clearReportCardMarkers();
  renderReportMarkers();
}

async function quickUpdateReportStatus(point: ReportMapPointRecord) {
  if (quickProcessingIds.value.has(point.reportId)) {
    return;
  }
  quickProcessingIds.value = new Set([...quickProcessingIds.value, point.reportId]);
  try {
    await updateReportStatusApi(point.reportId, {
      adminNote: '工作台快捷标记处理中',
      status: 'processing',
    });
    message.success('已标记为处理中');
    await Promise.all([loadReportMapPoints(), refreshCommunityMapStats()]);
  } finally {
    const nextIds = new Set(quickProcessingIds.value);
    nextIds.delete(point.reportId);
    quickProcessingIds.value = nextIds;
  }
}

async function initMap() {
  await nextTick();
  if (!mapContainerRef.value || !mapData.value?.center) {
    return;
  }
  cleanupMap();
  mapLoading.value = true;
  mapError.value = '';
  try {
    await loadAmap();
    const AMap = window.AMap;
    if (!AMap) {
      throw new Error('高德地图加载失败');
    }
    const center = mapData.value.center ?? DEFAULT_CENTER;
    buildingLayer = new AMap.Buildings({
      heightFactor: 2,
      zooms: [2, 20],
      zIndex: 10,
    });
    mapInstance = new AMap.Map(mapContainerRef.value, {
      center,
      features: ['bg', 'road', 'point', 'building'],
      pitch: mapData.value.pitch ?? 58,
      rotation: mapData.value.rotation ?? -18,
      showLabel: true,
      viewMode: '3D',
      zoom: mapData.value.zoom ?? 17,
    });
    mapInstance.add(buildingLayer);
    setupMapContextMenu();
    renderCommunityArea();
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '地图加载失败';
  } finally {
    mapLoading.value = false;
  }
}

function renderCommunityArea() {
  if (!mapInstance || !window.AMap || !mapData.value?.boundary?.length) {
    return;
  }
  const boundary = mapData.value.boundary;
  if (boundary.length >= 3) {
    mapInstance.setCenter(mapData.value.center);
    mapInstance.setZoom(mapData.value.zoom ?? 17);
    mapInstance.setPitch?.(mapData.value.pitch ?? 58);
    mapInstance.setRotation?.(mapData.value.rotation ?? -18);
  }
  buildingLayer?.setStyle?.({
    areas: [
      {
        color1: mapData.value.buildingColor || '#ffcc00',
        color2: mapData.value.buildingColor || '#ffcc00',
        path: boundary,
        rejectTexture: true,
      },
    ],
    hideWithoutStyle: false,
  });
}

function setupMapContextMenu() {
  if (!mapInstance || !window.AMap?.ContextMenu) {
    return;
  }
  mapContextMenu = new window.AMap.ContextMenu();
  mapContextMenu.addItem('关闭所有弹窗', () => {
    setReportCardsVisible(false);
    clearReportCardMarkers();
    mapContextMenu?.close?.();
  }, 0);
  mapContextMenu.addItem('打开所有弹窗', () => {
    setReportCardsVisible(true);
    openAllReportCards();
    mapContextMenu?.close?.();
  }, 1);
  mapInstance.on?.('rightclick', (event: { lnglat: any }) => {
    mapContextMenu.open(mapInstance, event.lnglat);
  });
}

async function loadReportMapPoints() {
  if (!selectedCommunityId.value) {
    reportMapPoints.value = [];
    clearReportMarkers();
    return;
  }
  try {
    const points = await getReportMapPointsApi({
      communityId: selectedCommunityId.value,
      limit: 300,
      status: 'pending',
    });
    reportMapPoints.value = points;
    if (
      reportCategoryFilter.value !== 'all'
      && !points.some((point) => point.category === reportCategoryFilter.value)
    ) {
      reportCategoryFilter.value = 'all';
    }
    renderReportMarkers();
  } catch {
    reportMapPoints.value = [];
    clearReportMarkers();
  }
}

async function refreshCommunityMapStats() {
  if (!selectedCommunityId.value) {
    return;
  }
  mapData.value = await getDashboardCommunityMapApi(selectedCommunityId.value);
}

function renderReportMarkers() {
  if (!mapInstance || !window.AMap) {
    return;
  }
  clearReportMarkers();
  clearReportCardMarkers();
  const pointsWithCoordinate = reportPointsWithCoordinate();
  reportMarkers = pointsWithCoordinate.map((point) => createReportMarker(point));
  if (reportMarkers.length > 0) {
    if (window.AMap.MarkerCluster && reportMarkers.length > REPORT_CLUSTER_THRESHOLD) {
      reportCluster = new window.AMap.MarkerCluster(mapInstance, reportMarkers, {
        gridSize: 72,
        renderClusterMarker: renderClusterMarker,
      });
    } else {
      mapInstance.add(reportMarkers);
      openDefaultReportCards(pointsWithCoordinate);
    }
  }
}

function openDefaultReportCards(points: ReportMapPointRecord[]) {
  if (!reportCardsVisible.value || points.length > REPORT_CLUSTER_THRESHOLD) {
    return;
  }
  points.forEach((point) => openReportCard(point, false));
}

function openAllReportCards() {
  setReportCardsVisible(true);
  reportPointsWithCoordinate().forEach((point) => openReportCard(point, false));
}

function reportPointsWithCoordinate() {
  return filteredReportMapPoints.value.filter(
    (point) => point.longitude != null && point.latitude != null,
  );
}

function createReportMarker(point: ReportMapPointRecord) {
  const markerElement = document.createElement('div');
  const level = reportPointLevel(point);
  markerElement.className = `report-alert-marker is-${level}`;
  markerElement.innerHTML = '<span class="report-alert-marker__pulse"></span><span class="report-alert-marker__pulse report-alert-marker__pulse--delay"></span><span class="report-alert-marker__dot"></span>';
  const marker = new window.AMap.Marker({
    anchor: 'center',
    content: markerElement,
    position: [Number(point.longitude), Number(point.latitude)],
    zIndex: level === 'overdue' ? 130 : level === 'today' ? 120 : 110,
  });
  marker.on('click', () => openReportCard(point));
  return marker;
}

function renderClusterMarker(context: { count: number; marker: any }) {
  const clusterElement = document.createElement('div');
  clusterElement.className = 'report-cluster-marker';
  clusterElement.textContent = String(context.count);
  context.marker.setContent(clusterElement);
}

function openReportCard(point: ReportMapPointRecord, centerMap = true) {
  if (!mapInstance || !window.AMap) {
    return;
  }
  const existingMarker = reportCardMarkerMap.get(point.reportId);
  if (existingMarker) {
    if (centerMap) {
      mapInstance.setCenter([Number(point.longitude), Number(point.latitude)]);
    }
    existingMarker.marker.setzIndex?.(760);
    return;
  }
  const cardMarker = createReportCardMarker(point);
  mapInstance.add(cardMarker);
  reportCardMarkers.push(cardMarker);
  reportCardMarkerMap.set(point.reportId, { marker: cardMarker, point });
}

function focusReportPoint(point: ReportMapPointRecord) {
  if (!mapInstance) {
    return;
  }
  const position = [Number(point.longitude), Number(point.latitude)];
  const currentZoom = Number(mapInstance.getZoom?.() ?? 17);
  const targetZoom = currentZoom < 18 ? 18 : currentZoom;
  clearReportCardMarkers();
  focusMapPositionImmediately(position, targetZoom);
  openReportCard(point, false);
}

function focusMapPositionImmediately(position: number[], zoom: number) {
  if (typeof mapInstance?.setZoomAndCenter === 'function') {
    mapInstance.setZoomAndCenter(zoom, position, true);
    return;
  }
  mapInstance?.setCenter?.(position, true);
  if (Number(mapInstance?.getZoom?.() ?? 17) !== zoom) {
    mapInstance?.setZoom?.(zoom, true);
  }
}

function buildReportCardContent(point: ReportMapPointRecord) {
  const card = document.createElement('div');
  const showImage = Boolean(point.firstImageUrl);
  card.className = `report-map-card ${showImage ? 'has-image' : 'is-compact'}`;
  card.innerHTML = `
      <button class="report-map-card__close" type="button" aria-label="关闭">×</button>
      ${
        showImage && point.firstImageUrl
          ? `<img class="report-map-card__image" src="${escapeHtml(point.firstImageUrl)}" alt="" />`
          : ''
      }
      <div class="report-map-card__body">
        <div class="report-map-card__title">${escapeHtml(point.subCategory || point.category || '未分类问题')}</div>
        <div class="report-map-card__meta">${escapeHtml(formatSubmittedAt(point.submittedAt))}</div>
      </div>
  `;
  let cardMarker: any;
  card.querySelector('.report-map-card__close')?.addEventListener('click', (event) => {
    event.stopPropagation();
    if (cardMarker && mapInstance) {
      mapInstance.remove(cardMarker);
      reportCardMarkers = reportCardMarkers.filter((item) => item !== cardMarker);
      reportCardMarkerMap.delete(point.reportId);
    }
  });
  card.querySelector('.report-map-card__title')?.addEventListener('click', (event) => {
    event.stopPropagation();
    openReportDetail(point);
  });
  return {
    element: card,
    setMarker(marker: any) {
      cardMarker = marker;
    },
  };
}

function createReportCardMarker(point: ReportMapPointRecord) {
  const content = buildReportCardContent(point);
  const marker = new window.AMap.Marker({
    anchor: 'bottom-center',
    content: content.element,
    offset: new window.AMap.Pixel(0, -18),
    position: [Number(point.longitude), Number(point.latitude)],
    zIndex: 720 + reportCardMarkers.length,
  });
  content.setMarker(marker);
  return marker;
}

function openReportDetail(point: ReportMapPointRecord) {
  router.push({
    name: 'EvidenceReports',
    query: {
      reportId: point.reportId,
    },
  });
}

function reportPointLevel(point: ReportMapPointRecord) {
  if (isOverdueReport(point.submittedAt)) {
    return 'overdue';
  }
  if (isTodayReport(point.submittedAt)) {
    return 'today';
  }
  return 'normal';
}

function reportPointLevelText(point: ReportMapPointRecord) {
  const level = reportPointLevel(point);
  if (level === 'overdue') {
    return '超时';
  }
  if (level === 'today') {
    return '今日';
  }
  return '待处理';
}

function isOverdueReport(submittedAt?: string) {
  if (!submittedAt) {
    return false;
  }
  return Date.now() - new Date(submittedAt).getTime() > 24 * 60 * 60 * 1000;
}

function isTodayReport(submittedAt?: string) {
  if (!submittedAt) {
    return false;
  }
  const reportDate = new Date(submittedAt);
  const today = new Date();
  return reportDate.toDateString() === today.toDateString();
}

function formatSubmittedAt(submittedAt?: string) {
  if (!submittedAt) {
    return '-';
  }
  return submittedAt.replace('T', ' ').slice(0, 16);
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

async function loadCommunityMap(communityId?: string) {
  stopReportStream();
  if (!communityId) {
    mapData.value = undefined;
    cleanupMap();
    return;
  }
  mapData.value = await getDashboardCommunityMapApi(communityId);
  await initMap();
  await loadReportMapPoints();
  startReportStream();
  startMapPointPolling();
}

async function loadData() {
  loading.value = true;
  try {
    const communityList = await getDashboardMapCommunitiesApi();
    communities.value = communityList;
    selectedCommunityId.value = communityList[0]?.id;
    await loadCommunityMap(selectedCommunityId.value);
  } finally {
    loading.value = false;
  }
}

async function onCommunityChange(communityId: string) {
  stopReportStream();
  selectedCommunityId.value = communityId;
  await loadCommunityMap(communityId);
}

async function toggleMapFullscreen() {
  mapFullscreen.value = !mapFullscreen.value;
  await nextTick();
  mapInstance?.resize?.();
}

onMounted(loadData);
onBeforeUnmount(cleanupMap);
</script>

<template>
  <div class="dashboard-page">
    <a-spin :spinning="loading">
      <section class="map-panel" :class="{ 'is-fullscreen': mapFullscreen }">
        <div class="map-header">
          <div class="map-title-line">
            <h2>{{ mapData?.communityName || '小区地图工作台' }}</h2>
            <div v-if="mapData" class="map-title-stats">
              <span>记录总数 {{ mapData.reportCount }} 个</span>
              <span>今日上报 {{ mapData.todayReportCount }} 个</span>
            </div>
          </div>
          <div class="map-toolbar">
            <a-select
              v-model:value="selectedCommunityId"
              class="community-select"
              :disabled="communityOptions.length === 0"
              :options="communityOptions"
              placeholder="选择小区"
              @change="onCommunityChange"
            />
            <button
              :aria-label="mapFullscreen ? '缩小地图' : '放大地图'"
              class="map-fullscreen-btn is-hidden"
              type="button"
              @click="toggleMapFullscreen"
            >
              <IconifyIcon
                class="map-fullscreen-icon"
                :icon="mapFullscreen ? 'lucide:minimize-2' : 'lucide:maximize-2'"
              />
            </button>
          </div>
        </div>

        <div v-if="communityOptions.length === 0" class="empty-state">
          <a-empty description="暂无已配置地图的小区" />
        </div>
        <div v-else class="map-workbench">
          <div class="map-wrap">
            <div ref="mapContainerRef" class="map-container"></div>
            <button
              aria-label="缩小地图"
              class="map-fullscreen-float-btn"
              type="button"
              @click="toggleMapFullscreen"
            >
              <IconifyIcon
                class="map-fullscreen-icon"
                :icon="mapFullscreen ? 'lucide:minimize-2' : 'lucide:maximize-2'"
              />
            </button>
            <div v-if="mapLoading" class="map-mask">地图加载中...</div>
            <div v-if="mapError" class="map-error">{{ mapError }}</div>
            <Transition name="report-float">
              <button
                v-if="reportPanelCollapsed"
                class="report-panel-toggle"
                type="button"
                @click="setReportPanelCollapsed(false)"
              >
                待处理 {{ reportMapPoints.length }}
              </button>
            </Transition>
            <Transition name="report-float">
              <aside v-if="!reportPanelCollapsed" class="report-side-panel">
              <div class="report-side-header">
                <div class="report-filter-tabs">
                  <button
                    :class="{ active: reportFilterMode === 'all' }"
                    type="button"
                    @click="setReportFilterMode('all')"
                  >
                    全部 {{ allFilterReportCount }}
                  </button>
                  <button
                    :class="{ active: reportFilterMode === 'today' }"
                    type="button"
                    @click="setReportFilterMode('today')"
                  >
                    今日 {{ todayFilterReportCount }}
                  </button>
                  <button
                    :class="{ active: reportFilterMode === 'overdue' }"
                    type="button"
                    @click="setReportFilterMode('overdue')"
                  >
                    超时 {{ overdueFilterReportCount }}
                  </button>
                </div>
                <a-select
                  v-model:value="reportCategoryFilter"
                  class="report-filter-category"
                  :options="reportCategoryOptions"
                  size="small"
                  @change="onReportCategoryChange"
                />
                <div class="report-side-actions">
                  <button
                    class="report-side-collapse"
                    type="button"
                    @click="setReportPanelCollapsed(true)"
                  >
                    收起
                  </button>
                </div>
              </div>
              <div v-if="reportMapPoints.length === 0" class="report-empty">
                当前小区暂无待处理上报
              </div>
              <div v-if="reportMapPoints.length > 0 && filteredReportMapPoints.length === 0" class="report-empty">
                当前筛选下暂无待处理上报
              </div>
              <TransitionGroup
                v-else-if="filteredReportMapPoints.length > 0"
                class="report-list"
                name="report-list"
                tag="div"
              >
                <div
                  v-for="point in filteredReportMapPoints"
                  :key="point.reportId"
                  class="report-list-item"
                  :class="[`is-${reportPointLevel(point)}`, { 'is-new': isNewReport(point) }]"
                  role="button"
                  tabindex="0"
                  @click="focusReportPoint(point)"
                  @keydown.enter="focusReportPoint(point)"
                >
                  <div class="report-list-title">
                    <span>{{ point.category }} / {{ point.subCategory }}</span>
                    <div class="report-list-actions" @click.stop>
                      <button
                        :disabled="isQuickProcessing(point)"
                        type="button"
                        @click="quickUpdateReportStatus(point)"
                      >
                        处理中
                      </button>
                    </div>
                    <em>{{ reportPointLevelText(point) }}</em>
                  </div>
                  <div class="report-list-meta">{{ formatSubmittedAt(point.submittedAt) }}</div>
                </div>
              </TransitionGroup>
              </aside>
            </Transition>
          </div>
        </div>
      </section>
    </a-spin>
  </div>
</template>

<style scoped>
.dashboard-page {
  min-height: 100%;
  padding: 20px;
  background: #f5f7fb;
}

.map-panel {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.map-panel.is-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  border: 0;
  border-radius: 0;
}

.map-panel.is-fullscreen .map-header {
  display: none;
}

.map-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.map-title-line {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.map-header h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  font-weight: 650;
  white-space: nowrap;
}

.map-title-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.map-title-stats span {
  padding-left: 14px;
  border-left: 1px solid #e5e7eb;
}

.community-select {
  width: 260px;
}

.map-toolbar {
  display: flex;
  flex: none;
  align-items: center;
  gap: 8px;
}

.map-fullscreen-btn {
  width: 34px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.map-fullscreen-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
  box-shadow: 0 4px 12px rgb(22 119 255 / 12%);
}

.map-fullscreen-btn.is-hidden {
  display: none;
}

.map-fullscreen-icon {
  width: 16px;
  height: 16px;
}

.map-workbench {
  position: relative;
  min-height: 520px;
}

.map-panel.is-fullscreen .map-workbench {
  flex: 1;
  min-height: 0;
}

.map-wrap {
  position: relative;
  height: calc(100vh - 150px);
  min-height: 520px;
}

.map-panel.is-fullscreen .map-wrap {
  height: 100%;
  min-height: 0;
}

.map-fullscreen-float-btn {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 20;
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(219 234 254 / 88%);
  border-radius: 7px;
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  background: rgb(255 255 255 / 54%);
  box-shadow: 0 10px 24px rgb(15 23 42 / 16%);
  color: #334155;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.map-fullscreen-float-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
  box-shadow: 0 12px 28px rgb(22 119 255 / 18%);
}

.map-container {
  width: 100%;
  height: 100%;
}

.map-mask,
.map-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(255 255 255 / 72%);
  color: #475569;
}

.map-error {
  color: #d4380d;
}

.empty-state {
  display: flex;
  min-height: 420px;
  align-items: center;
  justify-content: center;
}

.report-side-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  width: 360px;
  height: calc(100vh - 150px);
  max-height: calc(100% - 32px);
  min-height: 0;
  overflow: auto;
  border: 1px solid rgb(191 219 254 / 52%);
  border-radius: 8px;
  -webkit-backdrop-filter: blur(18px) saturate(145%);
  backdrop-filter: blur(18px) saturate(145%);
  background: linear-gradient(180deg, rgb(255 255 255 / 68%), rgb(248 251 255 / 54%));
  box-shadow: 0 18px 42px rgb(15 23 42 / 18%), inset 0 1px 0 rgb(255 255 255 / 72%);
}

.report-panel-toggle {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  height: 40px;
  padding: 0 14px;
  border: 1px solid rgb(147 197 253 / 56%);
  border-radius: 8px;
  -webkit-backdrop-filter: blur(16px) saturate(140%);
  backdrop-filter: blur(16px) saturate(140%);
  background: linear-gradient(135deg, rgb(255 255 255 / 66%), rgb(239 246 255 / 52%));
  box-shadow: 0 12px 28px rgb(15 23 42 / 16%), inset 0 1px 0 rgb(255 255 255 / 70%);
  color: #1d4ed8;
  cursor: pointer;
  font-size: 13px;
  font-weight: 650;
}

.report-panel-toggle:hover {
  border-color: #60a5fa;
  color: #0958d9;
}

.report-float-enter-active,
.report-float-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.report-float-enter-from,
.report-float-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}

.report-side-header {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 10px;
  border-bottom: 1px solid rgb(219 234 254 / 80%);
  -webkit-backdrop-filter: blur(18px) saturate(145%);
  backdrop-filter: blur(18px) saturate(145%);
  background: rgb(255 255 255 / 48%);
}

.report-side-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 8px;
}

.report-side-header span {
  color: #64748b;
  font-size: 12px;
}

.report-side-collapse {
  height: 26px;
  padding: 0 8px;
  border: 1px solid rgb(191 219 254 / 88%);
  border-radius: 6px;
  background: rgb(255 255 255 / 48%);
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

.report-side-collapse:hover {
  border-color: #60a5fa;
  color: #0958d9;
}

.report-empty {
  padding: 24px 14px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

.report-filter-tabs {
  display: flex;
  flex: 1;
  overflow: hidden;
  border: 1px solid rgb(191 219 254 / 76%);
  border-radius: 6px;
  background: rgb(255 255 255 / 42%);
}

.report-filter-tabs button {
  height: 26px;
  min-width: 0;
  flex: 1;
  padding: 0 7px;
  border: 0;
  border-right: 1px solid rgb(219 234 254 / 70%);
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.report-filter-tabs button:last-child {
  border-right: 0;
}

.report-filter-tabs button.active {
  background: rgb(219 234 254 / 74%);
  color: #1d4ed8;
  font-weight: 650;
}

.report-filter-category {
  width: 104px;
  flex: none;
}

.report-filter-category :deep(.ant-select-selector) {
  height: 26px !important;
  border-color: rgb(191 219 254 / 76%) !important;
  background: rgb(255 255 255 / 42%) !important;
}

.report-filter-category :deep(.ant-select-selection-item),
.report-filter-category :deep(.ant-select-selection-placeholder) {
  line-height: 24px !important;
}

.report-list {
  position: relative;
}

.report-list-enter-active,
.report-list-leave-active,
.report-list-move {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.report-list-enter-from {
  opacity: 0;
  transform: translateX(18px);
}

.report-list-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.report-list-leave-active {
  position: absolute;
}

.report-list-item {
  display: block;
  width: calc(100% - 20px);
  margin: 10px;
  padding: 12px;
  border: 1px solid rgb(219 234 254 / 88%);
  border-left: 4px solid #1677ff;
  border-radius: 8px;
  background: rgb(255 255 255 / 54%);
  cursor: pointer;
  text-align: left;
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  will-change: transform, opacity;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.report-list-item.is-new {
  border-color: rgb(147 197 253 / 92%);
  border-left-color: #1677ff;
  animation: new-report-card 1.6s ease-out both;
}

.report-list-item:hover {
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgb(37 99 235 / 12%);
  transform: translateY(-1px);
}

.report-list-item.is-overdue {
  border-color: #fecaca;
  border-left-color: #ef4444;
}

.report-list-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.report-list-title span {
  overflow: hidden;
  flex: 1;
  color: #111827;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.report-list-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 4px;
}

.report-list-actions button {
  height: 22px;
  padding: 0 6px;
  border: 1px solid rgb(191 219 254 / 82%);
  border-radius: 5px;
  background: rgb(255 255 255 / 56%);
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

.report-list-actions button:hover {
  border-color: #60a5fa;
  color: #0958d9;
}

.report-list-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.report-list-title em {
  flex: none;
  padding: 1px 6px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-style: normal;
}

.report-list-item.is-overdue .report-list-title em {
  background: #fef2f2;
  color: #dc2626;
}

.report-list-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

:global(.report-cluster-marker) {
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: 3px solid rgb(255 255 255 / 88%);
  border-radius: 999px;
  background: #1677ff;
  box-shadow: 0 10px 24px rgb(22 119 255 / 32%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

:global(.report-alert-marker) {
  position: relative;
  width: 36px;
  height: 36px;
  cursor: pointer;
  z-index: 2;
}

:global(.report-alert-marker__dot) {
  position: absolute;
  z-index: 1;
  top: 11px;
  left: 11px;
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #5aa7ff;
  box-shadow: 0 6px 18px rgb(90 167 255 / 42%);
}

:global(.report-alert-marker__pulse) {
  position: absolute;
  z-index: 0;
  inset: 2px;
  border: 3px solid rgb(90 167 255 / 56%);
  border-radius: 999px;
  opacity: 0;
  pointer-events: none;
  transform: scale(0.42);
  will-change: opacity, transform;
}

:global(.report-alert-marker__pulse--delay) {
  animation-delay: 0.72s;
}

:global(.report-alert-marker.is-today .report-alert-marker__pulse) {
  animation: report-alert-pulse 1.55s ease-out infinite;
}

:global(.report-alert-marker.is-normal .report-alert-marker__pulse) {
  border-color: rgb(90 167 255 / 52%);
  animation: report-alert-pulse 1.85s ease-out infinite;
}

:global(.report-alert-marker.is-today .report-alert-marker__dot) {
  background: #3b8df2;
  box-shadow: 0 8px 18px rgb(59 141 242 / 34%);
}

:global(.report-alert-marker.is-overdue .report-alert-marker__pulse) {
  border-color: rgb(239 68 68 / 66%);
  animation: report-alert-pulse 1.28s ease-out infinite;
}

:global(.report-alert-marker .report-alert-marker__pulse--delay) {
  animation-delay: 0.72s;
}

:global(.report-alert-marker.is-overdue .report-alert-marker__dot) {
  background: #ef4444;
  box-shadow: 0 8px 18px rgb(239 68 68 / 45%);
}

:global(.report-map-card) {
  position: relative;
  display: grid;
  width: 184px;
  min-height: 54px;
  overflow: hidden;
  grid-template-columns: minmax(0, 1fr);
  border: 1px solid rgb(219 234 254 / 88%);
  border-radius: 7px;
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  background: rgb(255 255 255 / 54%);
  box-shadow: 0 10px 24px rgb(15 23 42 / 16%);
}

:global(.report-map-card.has-image) {
  width: 218px;
  grid-template-columns: 62px minmax(0, 1fr);
}

:global(.report-map-card__close) {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 1;
  display: flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(219 234 254 / 80%);
  border-radius: 999px;
  background: rgb(255 255 255 / 88%);
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
}

:global(.report-map-card__close:hover) {
  border-color: #1677ff;
  color: #0958d9;
}

:global(.report-map-card__image) {
  width: 62px;
  height: 62px;
  object-fit: cover;
  background: #f1f5f9;
}

:global(.report-map-card__body) {
  min-width: 0;
  padding: 8px 26px 8px 9px;
}

:global(.report-map-card.has-image .report-map-card__body) {
  padding-left: 9px;
}

:global(.report-map-card__title) {
  overflow: hidden;
  color: #111827;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.report-map-card__title:hover) {
  color: #1677ff;
}

:global(.report-map-card__meta) {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.25;
}

@keyframes report-alert-pulse {
  0% {
    opacity: 0.95;
    transform: scale(0.42);
  }

  58% {
    opacity: 0.42;
  }

  100% {
    opacity: 0;
    transform: scale(2.05);
  }
}

@keyframes new-report-card {
  0% {
    background: rgb(219 234 254 / 74%);
  }

  42% {
    background: rgb(239 246 255 / 70%);
  }

  100% {
    background: rgb(255 255 255 / 54%);
  }
}

@media (max-width: 900px) {
  .map-header {
    align-items: stretch;
    flex-direction: column;
  }

  .map-title-line,
  .map-title-stats {
    flex-wrap: wrap;
  }

  .community-select {
    width: 100%;
  }

  .map-workbench {
    min-height: 0;
  }

  .report-side-panel {
    inset: 12px;
    width: auto;
    height: auto;
    max-height: calc(100% - 24px);
  }

}
</style>
