-- Libuke Evidence init SQL
-- Sanitized demo seed data. Real secrets, WeChat users and access tokens are not included.

CREATE DATABASE IF NOT EXISTS libuke_evidence DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE libuke_evidence;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- communities.sql
-- ------------------------------------------------------------
create table communities
(
    id              bigint                             not null
        primary key,
    name            varchar(100)                       not null,
    address         varchar(255)                       null,
    principal_name  varchar(100)                       null,
    principal_phone varchar(50)                        null,
    enabled         tinyint  default 1                 not null,
    center_lng      decimal(10, 7)                     null,
    center_lat      decimal(10, 7)                     null,
    boundary_json   text                               null,
    building_color  varchar(20)                        null,
    map_zoom        int                                null comment '小区地图默认缩放级别',
    map_pitch       int                                null comment '小区地图默认俯仰角',
    map_rotation    int                                null comment '小区地图默认旋转角',
    created_at      datetime default CURRENT_TIMESTAMP not null,
    updated_at      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted         tinyint  default 0                 not null
);

INSERT INTO communities (id, name, address, principal_name, principal_phone, enabled, center_lng, center_lat, boundary_json, building_color, map_zoom, map_pitch, map_rotation, created_at, updated_at, deleted) VALUES (2060763860469374978, '御锦城阳光里', '西安市灞桥区席王街道御锦城阳光里', null, null, 1, 109.0457410, 34.2792150, '[[109.04406,34.278539],[109.044743,34.277451],[109.044835,34.277453],[109.045072,34.277208],[109.045771,34.278018],[109.047447,34.279953],[109.047513,34.28054],[109.046494,34.280599],[109.046285,34.280511],[109.04617,34.280538],[109.046142,34.280838],[109.04521,34.280808],[109.045339,34.280231],[109.04547,34.279796],[109.045379,34.279327],[109.04517,34.278999],[109.044405,34.27867]]', '#e5b3c7', 17, 58, -18, '2026-05-31 00:42:45', '2026-06-06 11:20:32', 0);

-- ------------------------------------------------------------
-- roles.sql
-- ------------------------------------------------------------
create table roles
(
    id         bigint                             not null
        primary key,
    code       varchar(64)                        not null,
    name       varchar(100)                       not null,
    remark     varchar(255)                       null,
    enabled    tinyint  default 1                 not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted    tinyint  default 0                 not null,
    constraint uk_roles_code
        unique (code)
);

INSERT INTO roles (id, code, name, remark, enabled, created_at, updated_at, deleted) VALUES (2058936896896917506, 'admin', '管理员', null, 1, '2026-05-25 23:43:04', '2026-05-25 23:43:04', 0);
INSERT INTO roles (id, code, name, remark, enabled, created_at, updated_at, deleted) VALUES (2061013963499171842, 'super_admin', '超级管理员', null, 1, '2026-05-31 17:16:34', '2026-05-31 17:16:34', 0);
INSERT INTO roles (id, code, name, remark, enabled, created_at, updated_at, deleted) VALUES (2061025845643636737, 'show_data', '数据运营', null, 1, '2026-05-31 18:03:47', '2026-05-31 18:03:47', 0);
INSERT INTO roles (id, code, name, remark, enabled, created_at, updated_at, deleted) VALUES (2061029260236288002, 'ordinary_user', '普通用户', '小程序默认注册用户', 1, '2026-05-31 18:17:22', '2026-05-31 18:17:22', 0);

-- ------------------------------------------------------------
-- menus.sql
-- ------------------------------------------------------------
create table menus
(
    id                    bigint                               not null
        primary key,
    parent_id             bigint                               null,
    type                  varchar(20)                          not null,
    name                  varchar(100)                         not null,
    title                 varchar(100)                         not null,
    path                  varchar(255)                         null,
    active_path           varchar(255)                         null,
    component             varchar(255)                         null,
    icon                  varchar(100)                         null,
    active_icon           varchar(100)                         null,
    permission_code       varchar(100)                         null,
    sort_order            int        default 0                 not null,
    enabled               tinyint(1) default 1                 not null,
    hidden                tinyint(1) default 0                 not null,
    affix_tab             tinyint(1) default 0                 not null,
    keep_alive            tinyint(1) default 0                 not null,
    hide_children_in_menu tinyint(1) default 0                 not null,
    hide_in_breadcrumb    tinyint(1) default 0                 not null,
    hide_in_tab           tinyint(1) default 0                 not null,
    badge_type            varchar(20)                          null,
    badge                 varchar(50)                          null,
    badge_variants        varchar(30)                          null,
    link_src              varchar(500)                         null,
    created_at            datetime   default CURRENT_TIMESTAMP not null,
    updated_at            datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted               tinyint    default 0                 not null
)
    collate = utf8mb4_unicode_ci;

create index idx_menus_parent
    on menus (parent_id);

create index idx_menus_permission
    on menus (permission_code);

create index idx_menus_type_enabled
    on menus (type, enabled);

INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1000, null, 'catalog', 'EvidenceCommunityOps', '小区运营', '/evidence/community-ops', null, '', 'lucide:building-2', null, null, 40, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1010, null, 'menu', 'EvidenceDashboard', '工作台', '/evidence/dashboard', null, '/evidence/dashboard/index', 'lucide:layout-dashboard', null, 'dashboard:view', 10, 1, 0, 1, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1015, null, 'menu', 'EvidenceAnalytics', '统计分析', '/evidence/analytics', null, '/evidence/analytics/index', 'lucide:chart-no-axes-combined', null, null, 15, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-06-07 12:14:54', '2026-06-07 12:14:54', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1020, null, 'menu', 'EvidenceReports', '问题记录', '/evidence/reports', null, '/evidence/reports/index', 'lucide:clipboard-list', null, 'reports:view', 20, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1030, 1000, 'menu', 'EvidenceCommunities', '小区管理', '/evidence/communities', null, '/evidence/communities/index', 'lucide:building', null, 'communities:view', 10, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1040, 1000, 'menu', 'EvidenceInvitationCodes', '邀请码', '/evidence/invitation-codes', null, '/evidence/invitation-codes/index', 'lucide:ticket', null, 'invitation-codes:view', 20, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1050, 1300, 'menu', 'EvidenceWxUsers', '业主用户', '/evidence/users', null, '/evidence/users/index', 'lucide:users', null, 'wx-users:view', 20, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1100, null, 'catalog', 'EvidencePermissionManage', '权限管理', '/evidence/permission-manage', null, '', 'lucide:shield-check', null, null, 60, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1110, 1300, 'menu', 'EvidencePlatformUsers', '后台账号', '/evidence/platform-users', null, '/evidence/platform-users/index', 'lucide:user-cog', null, 'platform-users:view', 10, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1120, 1100, 'menu', 'EvidenceRoles', '角色管理', '/evidence/roles', null, '/evidence/roles/index', 'lucide:key-round', null, 'roles:view', 10, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1130, 1100, 'menu', 'EvidenceMenus', '菜单管理', '/evidence/menus', null, '/evidence/menus/index', 'lucide:menu-square', null, 'menus:view', 20, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1200, null, 'catalog', 'EvidenceBusinessConfig', '业务配置', '/evidence/business-config', null, '', 'lucide:sliders-horizontal', null, null, 50, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1210, 1200, 'menu', 'EvidenceSystemConfigs', '平台配置', '/evidence/system-configs', null, '/evidence/system-configs/index', 'lucide:settings-2', null, 'system-configs:view', 30, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1220, 1200, 'menu', 'EvidenceWatermarks', '水印配置', '/evidence/watermarks', null, '/evidence/watermarks/index', 'lucide:stamp', null, 'watermarks:view', 20, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1230, 1200, 'menu', 'EvidenceReportCategories', '问题分类', '/evidence/report-categories', null, '/evidence/report-categories/index', 'lucide:list-tree', null, 'report-categories:view', 10, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (1300, null, 'catalog', 'EvidenceUserAccounts', '用户账号', '/evidence/user-accounts', null, '', 'lucide:users-round', null, null, 30, 1, 0, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 16:36:39', '2026-06-07 11:16:24', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2010, 1030, 'button', 'CommunitiesCreate', '新增小区', null, null, null, null, null, 'communities:create', 10, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2020, 1030, 'button', 'CommunitiesUpdate', '编辑小区', null, null, null, null, null, 'communities:update', 20, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2030, 1030, 'button', 'CommunitiesDelete', '删除小区', null, null, null, null, null, 'communities:delete', 30, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2110, 1110, 'button', 'PlatformUsersCreate', '新增用户', null, null, null, null, null, 'platform-users:create', 10, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 16:38:10', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2120, 1110, 'button', 'PlatformUsersUpdate', '编辑用户', null, null, null, null, null, 'platform-users:update', 20, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 16:38:10', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2130, 1110, 'button', 'PlatformUsersResetPassword', '重置密码', null, null, null, null, null, 'platform-users:reset-password', 30, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2140, 1110, 'button', 'PlatformUsersDelete', '删除用户', null, null, null, null, null, 'platform-users:delete', 40, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 16:38:10', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2210, 1120, 'button', 'RolesCreate', '新增角色', null, null, null, null, null, 'roles:create', 10, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2220, 1120, 'button', 'RolesUpdate', '编辑角色', null, null, null, null, null, 'roles:update', 20, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2230, 1120, 'button', 'RolesDelete', '删除角色', null, null, null, null, null, 'roles:delete', 30, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2310, 1130, 'button', 'MenusCreate', '新增菜单', null, null, null, null, null, 'menus:create', 10, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2320, 1130, 'button', 'MenusUpdate', '编辑菜单', null, null, null, null, null, 'menus:update', 20, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2330, 1130, 'button', 'MenusDelete', '删除菜单', null, null, null, null, null, 'menus:delete', 30, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 12:07:20', '2026-05-31 12:07:20', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2410, 1050, 'button', 'WxUsersUpdate', '微信用户', null, null, null, null, null, 'wx-users:update', 10, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 16:50:35', '2026-05-31 16:50:35', 0);
INSERT INTO menus (id, parent_id, type, name, title, path, active_path, component, icon, active_icon, permission_code, sort_order, enabled, hidden, affix_tab, keep_alive, hide_children_in_menu, hide_in_breadcrumb, hide_in_tab, badge_type, badge, badge_variants, link_src, created_at, updated_at, deleted) VALUES (2420, 1050, 'button', 'WxUsersDelete', '删除用户', null, null, null, null, null, 'wx-users:delete', 20, 1, 1, 0, 0, 0, 0, 0, null, null, null, null, '2026-05-31 16:50:35', '2026-05-31 16:50:35', 0);

-- ------------------------------------------------------------
-- role_menus.sql
-- ------------------------------------------------------------
create table role_menus
(
    id         bigint                             not null
        primary key,
    role_id    bigint                             not null,
    menu_id    bigint                             not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint uk_role_menus_role_menu
        unique (role_id, menu_id)
)
    collate = utf8mb4_unicode_ci;

create index idx_role_menus_menu
    on role_menus (menu_id);

INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101000, 2058936896896917500, 1000, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101010, 2058936896896917500, 1010, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101020, 2058936896896917500, 1020, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101030, 2058936896896917500, 1030, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101040, 2058936896896917500, 1040, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101050, 2058936896896917500, 1050, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101100, 2058936896896917500, 1100, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101110, 2058936896896917500, 1110, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101120, 2058936896896917500, 1120, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101130, 2058936896896917500, 1130, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101200, 2058936896896917500, 1200, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101210, 2058936896896917500, 1210, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101220, 2058936896896917500, 1220, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (101230, 2058936896896917500, 1230, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102010, 2058936896896917500, 2010, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102020, 2058936896896917500, 2020, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102030, 2058936896896917500, 2030, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102110, 2058936896896917500, 2110, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102120, 2058936896896917500, 2120, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102130, 2058936896896917500, 2130, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102140, 2058936896896917500, 2140, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102210, 2058936896896917500, 2210, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102220, 2058936896896917500, 2220, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102230, 2058936896896917500, 2230, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102310, 2058936896896917500, 2310, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102320, 2058936896896917500, 2320, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (102330, 2058936896896917500, 2330, '2026-05-31 12:07:26');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (300000000000000001, 2058936896896917506, 1300, '2026-05-31 16:50:45');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (300000000000000002, 2058936896896917506, 2410, '2026-05-31 16:50:45');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (300000000000000003, 2058936896896917506, 2420, '2026-05-31 16:50:45');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2060957920601677826, 2058936896896917506, 1010, '2026-05-31 13:33:52');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2060957920958193665, 2058936896896917506, 1020, '2026-05-31 13:33:52');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013964304478209, 2061013963499171842, 1010, '2026-05-31 17:16:34');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013964652605442, 2061013963499171842, 1000, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013964992344066, 2061013963499171842, 1030, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013965344665602, 2061013963499171842, 1040, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013965696987138, 2061013963499171842, 2010, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013966053502978, 2061013963499171842, 2020, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013966397435905, 2061013963499171842, 2030, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013966670065666, 2061013963499171842, 1020, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013966997221377, 2061013963499171842, 1300, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013967324377090, 2061013963499171842, 1110, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013967647338498, 2061013963499171842, 1050, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013967907385345, 2061013963499171842, 2410, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013968234541057, 2061013963499171842, 2420, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013968557502466, 2061013963499171842, 2110, '2026-05-31 17:16:35');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013968821743617, 2061013963499171842, 2120, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013969216008194, 2061013963499171842, 2130, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013969538969601, 2061013963499171842, 2140, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013969861931010, 2061013963499171842, 1200, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013970193281026, 2061013963499171842, 1210, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013970520436738, 2061013963499171842, 1220, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013970780483585, 2061013963499171842, 1230, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013971107639298, 2061013963499171842, 1100, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013971434795009, 2061013963499171842, 1120, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013971703230465, 2061013963499171842, 1130, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013972034580481, 2061013963499171842, 2210, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013972290433026, 2061013963499171842, 2220, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013972621783041, 2061013963499171842, 2230, '2026-05-31 17:16:36');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013972948938754, 2061013963499171842, 2310, '2026-05-31 17:16:37');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013973284483073, 2061013963499171842, 2320, '2026-05-31 17:16:37');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061013973607444481, 2061013963499171842, 2330, '2026-05-31 17:16:37');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061025846365057026, 2061025845643636737, 1010, '2026-05-31 18:03:47');
INSERT INTO role_menus (id, role_id, menu_id, created_at) VALUES (2061025846755127298, 2061025845643636737, 1020, '2026-05-31 18:03:47');

-- ------------------------------------------------------------
-- report_categories.sql
-- ------------------------------------------------------------
create table report_categories
(
    id         bigint                             not null
        primary key,
    parent_id  bigint                             null,
    name       varchar(100)                       not null,
    code       varchar(64)                        null,
    sort_order int      default 0                 not null,
    enabled    tinyint  default 1                 not null,
    remark     varchar(255)                       null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted    tinyint  default 0                 not null
);

create index idx_report_categories_enabled
    on report_categories (enabled);

create index idx_report_categories_parent_id
    on report_categories (parent_id);

INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000301, null, '公共卫生类', 'public_health', 10, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000302, null, '公共设施类', 'public_facility', 20, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000303, null, '电梯问题类', 'elevator', 30, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000304, null, '消防安全类', 'fire_safety', 40, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000305, null, '秩序维护类', 'order_maintenance', 50, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000311, 1000000000000000301, '楼道卫生不干净', 'corridor_cleaning', 10, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000312, 1000000000000000301, '垃圾堆放', 'garbage_pile', 20, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000321, 1000000000000000302, '门禁损坏', 'access_control_broken', 10, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000322, 1000000000000000302, '路灯损坏', 'street_light_broken', 20, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000331, 1000000000000000303, '电梯停运', 'elevator_outage', 10, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000332, 1000000000000000303, '电梯异响', 'elevator_noise', 20, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000341, 1000000000000000304, '消防通道堵塞', 'fire_escape_blocked', 10, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);
INSERT INTO report_categories (id, parent_id, name, code, sort_order, enabled, remark, created_at, updated_at, deleted) VALUES (1000000000000000351, 1000000000000000305, '车辆乱停', 'parking_disorder', 10, 1, null, '2026-05-27 00:10:28', '2026-05-27 00:10:28', 0);

-- ------------------------------------------------------------
-- system_configs.sql
-- ------------------------------------------------------------
create table system_configs
(
    id           bigint                                not null
        primary key,
    config_group varchar(64)                           not null,
    config_key   varchar(128)                          not null,
    config_name  varchar(100)                          not null,
    config_value text                                  null,
    value_type   varchar(32) default 'string'          not null,
    encrypted    tinyint     default 0                 not null,
    `sensitive`  tinyint     default 0                 not null,
    editable     tinyint     default 1                 not null,
    remark       varchar(255)                          null,
    created_at   datetime    default CURRENT_TIMESTAMP not null,
    updated_at   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted      tinyint     default 0                 not null,
    constraint uk_system_configs_group_key
        unique (config_group, config_key)
);

create index idx_system_configs_group
    on system_configs (config_group);

INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000501, 'basic', 'system_name', '系统名称', '邻应台', 'string', 0, 0, 1, '后台系统展示名称', '2026-05-28 01:06:45', '2026-06-07 15:01:44', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000502, 'basic', 'platform_name', '平台名称', '邻应台', 'string', 0, 0, 1, '业务平台名称', '2026-05-28 01:06:45', '2026-06-07 15:01:44', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000503, 'basic', 'admin_title', '后台标题', '邻应台物业工作台', 'string', 0, 0, 1, '浏览器标题、后台顶部标题等展示文案', '2026-05-28 21:21:01', '2026-06-07 15:01:44', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000504, 'basic', 'miniapp_name', '小程序名称', '邻应台', 'string', 0, 0, 1, '微信小程序端展示名称', '2026-05-28 21:21:01', '2026-06-07 15:01:44', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000505, 'basic', 'operator_name', '运营主体', '物业服务中心', 'string', 0, 0, 1, '平台运营主体或物业服务单位名称', '2026-05-28 21:21:01', '2026-05-28 21:21:01', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000506, 'basic', 'support_phone', '客服电话', '', 'string', 0, 0, 1, '用户遇到问题时展示的联系电话', '2026-05-28 21:21:01', '2026-05-28 21:21:01', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000507, 'basic', 'support_email', '联系邮箱', '', 'string', 0, 0, 1, '平台联系邮箱', '2026-05-28 21:21:01', '2026-05-28 21:21:01', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000508, 'basic', 'copyright_text', '版权信息', 'Copyright © 2026 邻应台', 'string', 0, 0, 1, '后台页脚或关于页展示的版权信息', '2026-05-28 21:21:01', '2026-06-07 15:01:44', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000509, 'basic', 'icp_record_no', '备案号', '', 'string', 0, 0, 1, '网站备案号，没有可留空', '2026-05-28 21:21:01', '2026-05-28 21:21:01', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000510, 'basic', 'platform_description', '平台简介', '小区问题上报与物业处理平台', 'TEXT', 0, 0, 1, '平台说明文案', '2026-05-28 21:21:01', '2026-06-07 15:01:44', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000541, 'security', 'allow_report_delete', '允许删除记录', 'true', 'boolean', 0, 0, 1, '后台是否允许删除取证记录', '2026-05-28 01:06:45', '2026-05-28 01:06:45', 0);
INSERT INTO system_configs (id, config_group, config_key, config_name, config_value, value_type, encrypted, `sensitive`, editable, remark, created_at, updated_at, deleted) VALUES (1000000000000000542, 'security', 'operation_log_enabled', '启用操作日志', 'true', 'boolean', 0, 0, 1, '是否记录后台操作日志', '2026-05-28 01:06:45', '2026-05-28 01:06:45', 0);

-- ------------------------------------------------------------
-- storage_configs.sql
-- ------------------------------------------------------------
create table storage_configs
(
    id                    bigint                             not null
        primary key,
    name                  varchar(100)                       not null,
    provider              varchar(32)                        not null,
    endpoint              varchar(255)                       null,
    region                varchar(64)                        null,
    bucket_name           varchar(128)                       null,
    access_key_id         varchar(255)                       null,
    access_key_secret     varchar(500)                       null,
    upload_dir            varchar(255)                       null,
    original_dir          varchar(255)                       null,
    watermarked_dir       varchar(255)                       null,
    avatar_dir            varchar(255)                       null,
    presigned_url_minutes int      default 30                not null,
    enabled               tinyint  default 1                 not null,
    system_config         tinyint  default 0                 not null,
    remark                varchar(255)                       null,
    created_at            datetime default CURRENT_TIMESTAMP not null,
    updated_at            datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted               tinyint  default 0                 not null
);

create index idx_storage_configs_enabled
    on storage_configs (enabled);

INSERT INTO storage_configs (id, name, provider, endpoint, region, bucket_name, access_key_id, access_key_secret, upload_dir, original_dir, watermarked_dir, avatar_dir, presigned_url_minutes, enabled, system_config, remark, created_at, updated_at, deleted) VALUES (1000000000000000601, 'Object Storage Config', 'aliyun_oss', null, null, null, null, null, 'reports', 'reports/original', 'reports/watermarked', 'avatars', 30, 0, 1, 'Demo seed does not include real object storage credentials.', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);

-- ------------------------------------------------------------
-- map_configs.sql
-- ------------------------------------------------------------
create table map_configs
(
    id                  bigint                                   not null
        primary key,
    reverse_geocode_key varchar(255)                             null comment '高德 WebService Key，用于逆地址解析',
    js_api_key          varchar(255)                             null comment '高德 JS API Key，用于后台地图展示',
    js_api_security_key varchar(500)                             null comment '高德 JS API 安全密钥 Security JS Code',
    default_zoom        int            default 17                not null,
    default_longitude   decimal(10, 6) default 116.397128        not null comment '默认经度',
    default_latitude    decimal(10, 6) default 39.916527         not null comment '默认纬度',
    enabled             tinyint        default 1                 not null,
    system_config       tinyint        default 0                 not null,
    remark              varchar(255)                             null,
    created_at          datetime       default CURRENT_TIMESTAMP not null,
    updated_at          datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted             tinyint        default 0                 not null
);

create index idx_map_configs_enabled
    on map_configs (enabled);

INSERT INTO map_configs (id, reverse_geocode_key, js_api_key, js_api_security_key, default_zoom, default_longitude, default_latitude, enabled, system_config, remark, created_at, updated_at, deleted) VALUES (1000000000000000611, null, null, null, 17, 116.397128, 39.916527, 0, 1, 'Demo seed does not include real map keys.', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);

-- ------------------------------------------------------------
-- upload_policies.sql
-- ------------------------------------------------------------
create table upload_policies
(
    id                 bigint                             not null
        primary key,
    name               varchar(100)                       not null,
    scene              varchar(64)                        not null,
    max_file_count     int      default 4                 not null,
    max_file_size_mb   int      default 20                not null,
    allowed_mime_types varchar(500)                       not null,
    compress_enabled   tinyint  default 0                 not null,
    enabled            tinyint  default 1                 not null,
    system_config      tinyint  default 0                 not null,
    remark             varchar(255)                       null,
    created_at         datetime default CURRENT_TIMESTAMP not null,
    updated_at         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted            tinyint  default 0                 not null
);

create index idx_upload_policies_enabled
    on upload_policies (enabled);

create index idx_upload_policies_scene
    on upload_policies (scene);

INSERT INTO upload_policies (id, name, scene, max_file_count, max_file_size_mb, allowed_mime_types, compress_enabled, enabled, system_config, remark, created_at, updated_at, deleted) VALUES (1000000000000000621, '上报图片上传策略', 'report_image', 4, 20, 'image/jpeg,image/png', 0, 1, 1, '小程序上报图片上传限制', '2026-05-28 01:06:45', '2026-05-28 01:06:45', 0);

-- ------------------------------------------------------------
-- export_templates.sql
-- ------------------------------------------------------------
create table export_templates
(
    id                        bigint                             not null
        primary key,
    name                      varchar(100)                       not null,
    scene                     varchar(64)                        not null,
    fields_json               text                               not null,
    include_original_links    tinyint  default 1                 not null,
    include_watermarked_links tinyint  default 1                 not null,
    file_retention_days       int      default 7                 not null,
    enabled                   tinyint  default 1                 not null,
    system_config             tinyint  default 0                 not null,
    remark                    varchar(255)                       null,
    created_at                datetime default CURRENT_TIMESTAMP not null,
    updated_at                datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted                   tinyint  default 0                 not null
);

create index idx_export_templates_enabled
    on export_templates (enabled);

create index idx_export_templates_scene
    on export_templates (scene);

INSERT INTO export_templates (id, name, scene, fields_json, include_original_links, include_watermarked_links, file_retention_days, enabled, system_config, remark, created_at, updated_at, deleted) VALUES (1000000000000000631, '记录台账导出模板', 'report_export', '["reportNo","communityName","category","subCategory","locationAddress","longitude","latitude","submittedAt","status","witnessInfo","remark","adminNote"]', 1, 1, 7, 1, 1, '记录管理 Excel 导出字段配置', '2026-05-28 01:06:45', '2026-05-28 01:06:45', 0);

-- ------------------------------------------------------------
-- watermark_templates.sql
-- ------------------------------------------------------------
create table watermark_templates
(
    id                 bigint                                  not null
        primary key,
    name               varchar(100)                            not null,
    enabled            tinyint       default 1                 not null,
    position           varchar(32)   default 'bottom'          not null,
    opacity            decimal(3, 2) default 1.00              not null,
    background_opacity decimal(3, 2) default 0.58              not null,
    font_size          int                                     null,
    text_color         varchar(20)   default '#FFFFFF'         not null,
    background_color   varchar(20)   default '#000000'         not null,
    content_template   text                                    not null,
    system_template    tinyint       default 0                 not null,
    created_at         datetime      default CURRENT_TIMESTAMP not null,
    updated_at         datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted            tinyint       default 0                 not null
);

create index idx_watermark_templates_enabled
    on watermark_templates (enabled);

INSERT INTO watermark_templates (id, name, enabled, position, opacity, background_opacity, font_size, text_color, background_color, content_template, system_template, created_at, updated_at, deleted) VALUES (1000000000000000401, '默认水印模板', 0, 'bottom', 1.00, 0.58, null, '#FFFFFF', '#000000', '{{communityName}}｜{{category}} / {{subCategory}}
位置：{{locationAddress}}
上报时间：{{submittedAt}}
编号：{{reportNo}}', 1, '2026-05-27 21:10:03', '2026-05-27 22:02:57', 0);
INSERT INTO watermark_templates (id, name, enabled, position, opacity, background_opacity, font_size, text_color, background_color, content_template, system_template, created_at, updated_at, deleted) VALUES (2059625206753624065, '份额v从', 1, 'bottom', 1.00, 0.58, 13, '#FFFFFF', '#000000', '{{communityName}}｜{{category}} / {{subCategory}}
位置：{{locationAddress}}
上报时间：{{submittedAt}}
编号：{{reportNo}}', 0, '2026-05-27 21:18:09', '2026-05-27 22:04:10', 1);
INSERT INTO watermark_templates (id, name, enabled, position, opacity, background_opacity, font_size, text_color, background_color, content_template, system_template, created_at, updated_at, deleted) VALUES (2059637061014355969, '测试水印模板', 1, 'bottom', 0.90, 0.43, 24, '#df7272', '#000000', '{{communityName}}｜{{category}} / {{subCategory}}
位置：{{locationAddress}}
上报时间：{{submittedAt}}
编号：{{reportNo}}', 0, '2026-05-27 22:05:16', '2026-05-27 22:05:16', 0);

-- ------------------------------------------------------------
-- platform_users.sql
-- ------------------------------------------------------------
create table platform_users
(
    id            bigint                               not null
        primary key,
    username      varchar(64)                          not null,
    display_name  varchar(100)                         not null,
    phone         varchar(30)                          null,
    password_hash varchar(255)                         not null,
    access_token  varchar(128)                         null,
    enabled       tinyint(1) default 1                 not null,
    super_admin   tinyint(1) default 0                 not null,
    last_login_at datetime                             null,
    created_at    datetime   default CURRENT_TIMESTAMP not null,
    updated_at    datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted       tinyint    default 0                 not null,
    constraint uk_platform_users_token
        unique (access_token),
    constraint uk_platform_users_username_deleted
        unique (username, deleted)
)
    collate = utf8mb4_unicode_ci;

create index idx_platform_users_enabled
    on platform_users (enabled);

INSERT INTO platform_users (id, username, display_name, phone, password_hash, access_token, enabled, super_admin, last_login_at, created_at, updated_at, deleted) VALUES (1, 'admin', 'Admin', null, 'pbkdf2$120000$bGlidWtlLWRlbW8tc2FsdA==$kJ7jMPHS25K1HLWM3xFiZPLPFiesLZAm3QQQuabICNU=', null, 1, 1, null, '2026-01-01 00:00:00', '2026-01-01 00:00:00', 0);

-- ------------------------------------------------------------
-- platform_user_roles.sql
-- ------------------------------------------------------------
create table platform_user_roles
(
    id         bigint                             not null
        primary key,
    user_id    bigint                             not null,
    role_id    bigint                             not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint uk_platform_user_roles_user_role
        unique (user_id, role_id)
)
    collate = utf8mb4_unicode_ci;

create index idx_platform_user_roles_role
    on platform_user_roles (role_id);

INSERT INTO platform_user_roles (id, user_id, role_id, created_at) VALUES (1, 1, 2061013963499171842, '2026-05-31 17:17:30');

-- ------------------------------------------------------------
-- platform_user_communities.sql
-- ------------------------------------------------------------
create table platform_user_communities
(
    id           bigint                             not null
        primary key,
    user_id      bigint                             not null,
    community_id bigint                             not null,
    created_at   datetime default CURRENT_TIMESTAMP not null,
    constraint uk_platform_user_communities_user_community
        unique (user_id, community_id)
)
    collate = utf8mb4_unicode_ci;

create index idx_platform_user_communities_community
    on platform_user_communities (community_id);


-- ------------------------------------------------------------
-- wx_users.sql
-- ------------------------------------------------------------
create table wx_users
(
    id            bigint                                not null
        primary key,
    openid        varchar(64)                           not null,
    nickname      varchar(100)                          null,
    avatar_url    varchar(500)                          null,
    community_id  bigint                                null,
    witness_info  varchar(200)                          null,
    auth_status   varchar(32) default 'pending'         not null,
    last_login_at datetime                              null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted       tinyint     default 0                 not null,
    constraint uk_wx_users_openid
        unique (openid)
);

create index idx_wx_users_community_id
    on wx_users (community_id);


-- ------------------------------------------------------------
-- user_roles.sql
-- ------------------------------------------------------------
create table user_roles
(
    id         bigint                             not null
        primary key,
    user_id    bigint                             not null,
    role_id    bigint                             not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint uk_user_roles_user_role
        unique (user_id, role_id)
);


-- ------------------------------------------------------------
-- invitation_codes.sql
-- ------------------------------------------------------------
create table invitation_codes
(
    id              bigint                             not null
        primary key,
    community_id    bigint                             not null,
    code            varchar(64)                        not null,
    max_usage_count int                                null,
    used_count      int      default 0                 not null,
    expires_at      datetime                           null,
    enabled         tinyint  default 1                 not null,
    created_at      datetime default CURRENT_TIMESTAMP not null,
    updated_at      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted         tinyint  default 0                 not null,
    constraint uk_invitation_code_code
        unique (code)
);

create index idx_invitation_code_community_id
    on invitation_codes (community_id);

-- ------------------------------------------------------------
-- reports.sql
-- ------------------------------------------------------------
create table reports
(
    id               bigint                                not null
        primary key,
    report_no        varchar(64)                           not null,
    user_id          bigint                                not null,
    community_id     bigint                                not null,
    category         varchar(50)                           not null,
    sub_category     varchar(50)                           not null,
    longitude        decimal(10, 7)                        null,
    latitude         decimal(10, 7)                        null,
    location_address varchar(255)                          null,
    remark           varchar(200)                          null,
    status           varchar(32) default 'pending'         not null,
    admin_note       varchar(500)                          null,
    upload_status    varchar(32) default 'submitted'       not null,
    submitted_at     datetime    default CURRENT_TIMESTAMP not null,
    created_at       datetime    default CURRENT_TIMESTAMP not null,
    updated_at       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted          tinyint     default 0                 not null,
    constraint uk_reports_report_no
        unique (report_no)
);

create index idx_reports_community_id
    on reports (community_id);

create index idx_reports_user_category
    on reports (user_id, category);

-- ------------------------------------------------------------
-- report_images.sql
-- ------------------------------------------------------------
create table report_images
(
    id                     bigint                                not null
        primary key,
    report_id              bigint                                not null,
    original_object_key    varchar(500)                          not null,
    original_file_size     bigint                                null comment '原图文件大小',
    original_mime_type     varchar(100)                          null comment '原图MIME类型',
    original_file_name     varchar(255)                          null comment '原图文件名',
    client_uploaded_at     datetime                              null comment '客户端上传时间',
    server_received_at     datetime                              null comment '服务端封存时间',
    watermarked_object_key varchar(500)                          null,
    image_width            int                                   null,
    image_height           int                                   null,
    sort_order             int         default 0                 not null,
    process_status         varchar(32) default 'pending'         not null,
    created_at             datetime    default CURRENT_TIMESTAMP not null,
    updated_at             datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted                tinyint     default 0                 not null
);

create index idx_report_images_report_id
    on report_images (report_id);

-- ------------------------------------------------------------
-- report_events.sql
-- ------------------------------------------------------------
create table report_events
(
    id            bigint auto_increment comment '事件ID'
        primary key,
    report_id     bigint                                not null comment '问题记录ID',
    event_type    varchar(32)                           not null comment '事件类型',
    from_status   varchar(32)                           null comment '变更前状态',
    to_status     varchar(32)                           null comment '变更后状态',
    operator_type varchar(32) default 'system'          not null comment '操作人类型',
    operator_id   bigint                                null comment '操作人ID',
    operator_name varchar(100)                          null comment '操作人名称',
    content       varchar(500)                          null comment '事件内容',
    created_at    datetime    default CURRENT_TIMESTAMP not null comment '事件时间'
)
    comment '问题记录事件时间线';

create index idx_report_events_event_type
    on report_events (event_type);

create index idx_report_events_report_id_created_at
    on report_events (report_id, created_at);

-- ------------------------------------------------------------
-- watermark_tasks.sql
-- ------------------------------------------------------------
create table watermark_tasks
(
    id            bigint                                not null
        primary key,
    report_id     bigint                                not null,
    image_id      bigint                                not null,
    template_id   bigint                                null,
    status        varchar(32) default 'pending'         not null,
    retry_count   int         default 0                 not null,
    error_message varchar(500)                          null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_watermark_tasks_image_id
    on watermark_tasks (image_id);

create index idx_watermark_tasks_report_id
    on watermark_tasks (report_id);

create index idx_watermark_tasks_status
    on watermark_tasks (status);


SET FOREIGN_KEY_CHECKS = 1;
