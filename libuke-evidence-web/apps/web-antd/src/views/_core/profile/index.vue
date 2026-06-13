<script setup lang="ts">
/**
 * 个人中心页面
 */
import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { preferences } from '@vben/preferences';
import { useUserStore } from '@vben/stores';

import { message, Modal } from 'ant-design-vue';

import {
  updateCurrentPasswordApi,
  updateCurrentProfileApi,
} from '#/api';
import { useAuthStore } from '#/store';

const userStore = useUserStore();
const authStore = useAuthStore();
const activeTab = ref('profile');
const profileSaving = ref(false);
const passwordSaving = ref(false);

const profileForm = reactive({
  displayName: userStore.userInfo?.realName || '',
  phone: (userStore.userInfo?.phone as string | undefined) || '',
});

const passwordForm = reactive({
  confirmPassword: '',
  newPassword: '',
  oldPassword: '',
});

const userInfo = computed(() => userStore.userInfo);
const avatar = computed(() => userInfo.value?.avatar || preferences.app.defaultAvatar);
const roleText = computed(() => {
  const roles = userInfo.value?.roles ?? [];
  if (roles.includes('super_admin')) {
    return '超级管理员';
  }
  return roles.length > 0 ? roles.join('、') : '普通管理员';
});
const communityText = computed(() => {
  const communities = (userInfo.value?.communities ?? []) as Array<{
    name?: string;
  }>;
  if (communities.length === 0) {
    return '未绑定小区';
  }
  return communities.map((item) => item.name).filter(Boolean).join('、');
});

async function saveProfile() {
  if (!profileForm.displayName.trim()) {
    message.warning('请填写姓名');
    return;
  }
  profileSaving.value = true;
  try {
    const user = await updateCurrentProfileApi({
      displayName: profileForm.displayName.trim(),
      phone: profileForm.phone?.trim() || undefined,
    });
    userStore.setUserInfo(user);
    message.success('个人资料已更新');
  } finally {
    profileSaving.value = false;
  }
}

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    message.warning('请填写当前密码和新密码');
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    message.warning('新密码至少 6 位');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次输入的新密码不一致');
    return;
  }
  passwordSaving.value = true;
  try {
    await updateCurrentPasswordApi({
      newPassword: passwordForm.newPassword,
      oldPassword: passwordForm.oldPassword,
    });
    Modal.success({
      content: '密码已修改，请重新登录',
      onOk: () => authStore.logout(false),
      title: '修改成功',
    });
  } finally {
    passwordSaving.value = false;
  }
}
</script>

<template>
  <Page auto-content-height title="个人中心">
    <div class="profile-page">
      <a-card :bordered="false" class="profile-summary">
        <a-avatar :size="72" :src="avatar" />
        <div class="summary-main">
          <div class="summary-name">
            {{ userInfo?.realName || userInfo?.username || '-' }}
          </div>
          <div class="summary-sub">
            {{ userInfo?.username || '-' }}
          </div>
        </div>
        <div class="summary-meta">
          <a-tag color="blue">{{ roleText }}</a-tag>
        </div>
      </a-card>

      <a-card :bordered="false" class="profile-card">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="profile" tab="基本资料">
            <a-form
              class="profile-form"
              layout="vertical"
              :model="profileForm"
            >
              <a-form-item label="登录账号">
                <a-input :value="userInfo?.username" disabled />
              </a-form-item>
              <a-form-item label="姓名" required>
                <a-input
                  v-model:value="profileForm.displayName"
                  maxlength="100"
                  placeholder="请输入姓名"
                />
              </a-form-item>
              <a-form-item label="联系电话">
                <a-input
                  v-model:value="profileForm.phone"
                  maxlength="50"
                  placeholder="请输入联系电话"
                />
              </a-form-item>
              <a-form-item label="绑定小区">
                <a-textarea :value="communityText" auto-size disabled />
              </a-form-item>
              <a-button
                type="primary"
                :loading="profileSaving"
                @click="saveProfile"
              >
                保存资料
              </a-button>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="security" tab="账号安全">
            <a-descriptions bordered :column="1" size="middle">
              <a-descriptions-item label="账号状态">
                <a-tag color="green">正常</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="权限角色">
                {{ roleText }}
              </a-descriptions-item>
              <a-descriptions-item label="数据范围">
                {{ communityText }}
              </a-descriptions-item>
              <a-descriptions-item label="说明">
                当前账号只能查看和处理已绑定小区的数据。
              </a-descriptions-item>
            </a-descriptions>
          </a-tab-pane>

          <a-tab-pane key="password" tab="修改密码">
            <a-form
              class="profile-form"
              layout="vertical"
              :model="passwordForm"
            >
              <a-form-item label="当前密码" required>
                <a-input-password
                  v-model:value="passwordForm.oldPassword"
                  placeholder="请输入当前密码"
                />
              </a-form-item>
              <a-form-item label="新密码" required>
                <a-input-password
                  v-model:value="passwordForm.newPassword"
                  placeholder="请输入新密码"
                />
              </a-form-item>
              <a-form-item label="确认新密码" required>
                <a-input-password
                  v-model:value="passwordForm.confirmPassword"
                  placeholder="请再次输入新密码"
                />
              </a-form-item>
              <a-button
                type="primary"
                :loading="passwordSaving"
                @click="savePassword"
              >
                修改密码
              </a-button>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>
  </Page>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.profile-summary,
.profile-card {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 5%);
}

.profile-summary :deep(.ant-card-body) {
  display: flex;
  align-items: center;
  gap: 18px;
}

.summary-main {
  min-width: 0;
  flex: 1;
}

.summary-name {
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
}

.summary-sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.summary-meta {
  display: flex;
  align-items: center;
}

.profile-form {
  max-width: 520px;
  padding-top: 8px;
}
</style>
