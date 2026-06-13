import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:building-2',
      order: -10,
      title: '物业取证',
    },
    name: 'Evidence',
    path: '/evidence',
    children: [
      {
        name: 'EvidenceDashboard',
        path: '/evidence/dashboard',
        component: () => import('#/views/evidence/dashboard/index.vue'),
        meta: {
          affixTab: true,
          icon: 'lucide:layout-dashboard',
          title: '工作台',
        },
      },
      {
        name: 'EvidenceReports',
        path: '/evidence/reports',
        component: () => import('#/views/evidence/reports/index.vue'),
        meta: {
          icon: 'lucide:clipboard-list',
          title: '记录管理',
        },
      },
      {
        name: 'EvidenceAnalytics',
        path: '/evidence/analytics',
        component: () => import('#/views/evidence/analytics/index.vue'),
        meta: {
          icon: 'lucide:chart-no-axes-combined',
          title: '统计分析',
        },
      },
      {
        name: 'EvidenceReportCategories',
        path: '/evidence/report-categories',
        component: () => import('#/views/evidence/report-categories/index.vue'),
        meta: {
          icon: 'lucide:tags',
          title: '问题分类',
        },
      },
      {
        name: 'EvidenceWatermarks',
        path: '/evidence/watermarks',
        component: () => import('#/views/evidence/watermarks/index.vue'),
        meta: {
          icon: 'lucide:stamp',
          title: '水印管理',
        },
      },
      {
        name: 'EvidenceSystemConfigs',
        path: '/evidence/system-configs',
        component: () => import('#/views/evidence/system-configs/index.vue'),
        meta: {
          icon: 'lucide:settings-2',
          title: '系统配置',
        },
      },
      {
        name: 'EvidenceCommunities',
        path: '/evidence/communities',
        component: () => import('#/views/evidence/communities/index.vue'),
        meta: {
          icon: 'lucide:map-pinned',
          title: '小区管理',
        },
      },
      {
        name: 'EvidenceInvitationCodes',
        path: '/evidence/invitation-codes',
        component: () => import('#/views/evidence/invitation-codes/index.vue'),
        meta: {
          icon: 'lucide:key-round',
          title: '邀请码管理',
        },
      },
      {
        name: 'EvidenceUsers',
        path: '/evidence/users',
        component: () => import('#/views/evidence/users/index.vue'),
        meta: {
          icon: 'lucide:users',
          title: '用户管理',
        },
      },
      {
        name: 'EvidenceRoles',
        path: '/evidence/roles',
        component: () => import('#/views/evidence/roles/index.vue'),
        meta: {
          icon: 'lucide:shield-check',
          title: '角色管理',
        },
      },
    ],
  },
];

export default routes;
