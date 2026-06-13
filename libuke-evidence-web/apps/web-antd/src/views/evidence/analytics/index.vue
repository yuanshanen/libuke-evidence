<script lang="ts" setup>
/**
 * 问题统计分析页面
 */
import type {
  AnalyticsNameCountItem,
  AnalyticsOverviewData,
  AnalyticsOverdueReportItem,
  CommunityRecord,
} from '#/api';
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { Dayjs } from 'dayjs';

import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getAnalyticsOverviewApi,
  getDashboardMapCommunitiesApi,
  getReportCategoriesApi,
} from '#/api';
import { categoryOptions, statusMeta, statusOptions } from '#/views/evidence/constants';

interface SelectOption {
  label: string;
  value: string;
}

const router = useRouter();
const loading = ref(false);
const communities = ref<SelectOption[]>([]);
const categories = ref<SelectOption[]>([...categoryOptions]);
const analyticsData = ref<AnalyticsOverviewData>();
const trendChartRef = ref<EchartsUIType>();
const statusChartRef = ref<EchartsUIType>();
const categoryChartRef = ref<EchartsUIType>();
const subCategoryChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderStatusChart } = useEcharts(statusChartRef);
const { renderEcharts: renderCategoryChart } = useEcharts(categoryChartRef);
const { renderEcharts: renderSubCategoryChart } = useEcharts(subCategoryChartRef);

const filters = reactive<{
  category?: string;
  communityId?: string;
  dateRange: [Dayjs, Dayjs];
  status?: string;
}>({
  dateRange: [dayjs().subtract(29, 'day'), dayjs()],
});

const metricCards = computed(() => {
  const data = analyticsData.value;
  return [
    {
      color: '#2563eb',
      label: '上报总数',
      suffix: '条',
      value: data?.totalCount ?? 0,
    },
    {
      color: '#f59e0b',
      label: '待处理',
      suffix: '条',
      value: data?.pendingCount ?? 0,
    },
    {
      color: '#06b6d4',
      label: '处理中',
      suffix: '条',
      value: data?.processingCount ?? 0,
    },
    {
      color: '#16a34a',
      label: '已处理',
      suffix: '条',
      value: data?.resolvedCount ?? 0,
    },
    {
      color: '#ef4444',
      label: '超时未处理',
      suffix: '条',
      value: data?.overdueCount ?? 0,
    },
    {
      color: '#7c3aed',
      label: '平均处理时长',
      suffix: '小时',
      value: data?.avgProcessHours ?? 0,
    },
  ];
});

const overdueColumns = [
  {
    dataIndex: 'subCategory',
    key: 'subCategory',
    title: '问题',
  },
  {
    dataIndex: 'communityName',
    key: 'communityName',
    title: '小区',
    width: 150,
  },
  {
    dataIndex: 'locationAddress',
    ellipsis: true,
    key: 'locationAddress',
    title: '位置',
  },
  {
    dataIndex: 'submittedAt',
    key: 'submittedAt',
    title: '上报时间',
    width: 170,
  },
  {
    dataIndex: 'status',
    key: 'status',
    title: '状态',
    width: 100,
  },
];

onMounted(async () => {
  await Promise.all([loadCommunities(), loadCategories()]);
  await loadAnalytics();
});

watch(
  () => [filters.communityId, filters.category, filters.status, filters.dateRange],
  () => {
    loadAnalytics();
  },
  { deep: true },
);

async function loadCommunities() {
  const result = await getDashboardMapCommunitiesApi();
  communities.value = result.map((item: CommunityRecord) => ({
    label: item.name,
    value: item.id,
  }));
}

async function loadCategories() {
  const result = await getReportCategoriesApi({ enabled: true });
  const parentCategories = result
    .filter((item) => !item.parentId)
    .map((item) => ({ label: item.name, value: item.name }));
  categories.value = parentCategories.length > 0 ? parentCategories : [...categoryOptions];
}

async function loadAnalytics() {
  loading.value = true;
  try {
    analyticsData.value = await getAnalyticsOverviewApi({
      category: filters.category,
      communityId: filters.communityId,
      endDate: filters.dateRange?.[1]?.format('YYYY-MM-DD'),
      startDate: filters.dateRange?.[0]?.format('YYYY-MM-DD'),
      status: filters.status,
    });
    await nextTick();
    renderCharts();
  } catch {
    message.error('加载统计分析数据失败');
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  const data = analyticsData.value;
  if (!data) {
    return;
  }
  renderTrendChart({
    color: ['#2563eb', '#16a34a'],
    grid: { bottom: 20, containLabel: true, left: 16, right: 20, top: 36 },
    legend: { right: 12, top: 0 },
    series: [
      {
        areaStyle: { opacity: 0.12 },
        data: data.dailyTrend.map((item) => item.submittedCount),
        name: '上报',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        type: 'line',
      },
      {
        areaStyle: { opacity: 0.1 },
        data: data.dailyTrend.map((item) => item.resolvedCount),
        name: '处理完成',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        type: 'line',
      },
    ],
    tooltip: { trigger: 'axis' as const },
    xAxis: {
      axisLabel: { color: '#64748b' },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      boundaryGap: false,
      data: data.dailyTrend.map((item) => dayjs(item.date).format('MM-DD')),
      type: 'category',
    },
    yAxis: {
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#edf2f7' } },
      type: 'value',
    },
  });
  renderStatusChart({
    color: ['#f59e0b', '#06b6d4', '#3b82f6', '#16a34a', '#94a3b8', '#ef4444', '#a855f7'],
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        data: data.statusDistribution
          .filter((item) => item.count > 0)
          .map((item) => ({ name: item.label, value: item.count })),
        emphasis: {
          itemStyle: {
            shadowBlur: 14,
            shadowColor: 'rgba(15, 23, 42, 0.18)',
          },
        },
        radius: ['50%', '72%'],
        type: 'pie',
      },
    ],
    tooltip: { trigger: 'item' as const },
  });
  renderCategoryChart(buildBarChartOption(data.categoryDistribution, '#2563eb'));
  renderSubCategoryChart(buildBarChartOption(data.subCategoryTop, '#16a34a'));
}

function buildBarChartOption(
  items: AnalyticsNameCountItem[],
  color: string,
) {
  return {
    grid: { bottom: 20, containLabel: true, left: 12, right: 20, top: 20 },
    series: [
      {
        barMaxWidth: 18,
        data: items.map((item) => item.count),
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color,
        },
        type: 'bar' as const,
      },
    ],
    tooltip: { trigger: 'axis' as const },
    xAxis: {
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#edf2f7' } },
      type: 'value' as const,
    },
    yAxis: {
      axisLabel: { color: '#475569', width: 92 },
      axisTick: { show: false },
      data: items.map((item) => item.name),
      type: 'category' as const,
    },
  };
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function openReportDetail(record: AnalyticsOverdueReportItem) {
  router.push({
    name: 'EvidenceReports',
    query: { reportId: record.id },
  });
}

function overdueRowProps(record: AnalyticsOverdueReportItem) {
  return {
    onClick: () => openReportDetail(record),
  };
}
</script>

<template>
  <Page auto-content-height>
    <div class="analytics-page">
      <a-card :bordered="false" class="filter-panel">
        <div class="filter-row">
          <a-range-picker
            v-model:value="filters.dateRange"
            allow-clear
            class="filter-control date-filter"
            format="YYYY-MM-DD"
          />
          <a-select
            v-model:value="filters.communityId"
            allow-clear
            class="filter-control"
            :options="communities"
            placeholder="全部小区"
          />
          <a-select
            v-model:value="filters.category"
            allow-clear
            class="filter-control"
            :options="categories"
            placeholder="全部大类"
          />
          <a-select
            v-model:value="filters.status"
            allow-clear
            class="filter-control"
            :options="statusOptions"
            placeholder="全部状态"
          />
        </div>
      </a-card>

      <a-spin :spinning="loading">
        <div class="analytics-content">
        <div class="metric-grid">
          <div
            v-for="item in metricCards"
            :key="item.label"
            class="metric-card"
          >
            <div class="metric-label">{{ item.label }}</div>
            <div class="metric-value" :style="{ color: item.color }">
              {{ item.value }}
              <span>{{ item.suffix }}</span>
            </div>
          </div>
        </div>

        <div class="chart-grid">
          <a-card :bordered="false" class="chart-card chart-card-wide" title="上报与处理趋势">
            <EchartsUI ref="trendChartRef" height="320px" />
          </a-card>
          <a-card :bordered="false" class="chart-card" title="状态分布">
            <EchartsUI ref="statusChartRef" height="300px" />
          </a-card>
          <a-card :bordered="false" class="chart-card" title="问题大类分布">
            <EchartsUI ref="categoryChartRef" height="300px" />
          </a-card>
          <a-card :bordered="false" class="chart-card chart-card-wide" title="常见问题 Top 10">
            <EchartsUI ref="subCategoryChartRef" height="320px" />
          </a-card>
        </div>

        <a-card :bordered="false" class="overdue-panel" title="超时未处理记录">
          <a-table
            :columns="overdueColumns"
            :data-source="analyticsData?.overdueReports ?? []"
            :pagination="false"
            row-key="id"
            size="middle"
            @row="overdueRowProps"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'submittedAt'">
                {{ formatDateTime(record.submittedAt) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="statusMeta(record.status).color">
                  {{ statusMeta(record.status).label }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'subCategory'">
                <span class="report-link">{{ record.subCategory || '-' }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
        </div>
      </a-spin>
    </div>
  </Page>
</template>

<style scoped>
.analytics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analytics-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.filter-panel,
.metric-card,
.chart-card,
.overdue-panel {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 5%);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-control {
  min-width: 180px;
}

.date-filter {
  min-width: 260px;
}

.metric-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.metric-card {
  min-height: 96px;
  padding: 18px 18px 16px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #eef2f7;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 14px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.metric-value span {
  margin-left: 4px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}

.chart-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.chart-card-wide {
  grid-column: 1 / -1;
}

.report-link {
  color: #1677ff;
  cursor: pointer;
  font-weight: 500;
}

:deep(.ant-card-head) {
  min-height: 48px;
  border-bottom: 1px solid #eef2f7;
}

:deep(.ant-card-head-title) {
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

:deep(.ant-table-row) {
  cursor: pointer;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .chart-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .filter-control,
  .date-filter {
    width: 100%;
  }
}
</style>
