<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import { computed, markRaw, onMounted } from 'vue';

import { AuthenticationLogin, SliderCaptcha, z } from '@vben/common-ui';
import { $t } from '@vben/locales';
import { preferences } from '@vben/preferences';

import { useAuthStore } from '#/store';
import {
  loadRuntimePlatformConfig,
  runtimePlatformConfig,
} from '#/store/runtime-config';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();
const adminTitle = computed(() => runtimePlatformConfig.adminTitle);

onMounted(async () => {
  try {
    await loadRuntimePlatformConfig();
  } catch {
    // 配置接口异常时保留默认标题。
  }
});

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: '请输入管理员账号',
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
    {
      component: markRaw(SliderCaptcha),
      fieldName: 'captcha',
      rules: z.boolean().refine((value) => value, {
        message: $t('authentication.verifyRequiredTip'),
      }),
    },
  ];
});
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :show-code-login="false"
    :show-forget-password="false"
    :show-qrcode-login="false"
    :show-register="false"
    :show-third-party-login="false"
    @submit="authStore.authLogin"
  >
    <template #title>
      <div class="mb-8 flex flex-col items-center text-center">
        <img
          :src="preferences.logo.source"
          :alt="adminTitle"
          class="mb-4 size-14 object-contain"
        />
        <div class="text-2xl font-semibold">{{ adminTitle }}</div>
      </div>
    </template>
  </AuthenticationLogin>
</template>
