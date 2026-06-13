import { reactive } from 'vue';

import { preferences, updatePreferences } from '@vben/preferences';

import { getRuntimeConfigApi } from '#/api';

export const runtimePlatformConfig = reactive({
  adminTitle: '邻应台物业工作台',
  copyrightText: '',
  systemName: '邻应台',
});

export async function loadRuntimePlatformConfig() {
  const config = await getRuntimeConfigApi();
  runtimePlatformConfig.systemName =
    config.platform.system_name || runtimePlatformConfig.systemName;
  runtimePlatformConfig.adminTitle =
    config.platform.admin_title ||
    config.platform.system_name ||
    runtimePlatformConfig.adminTitle;
  runtimePlatformConfig.copyrightText =
    config.platform.copyright_text || runtimePlatformConfig.copyrightText;

  preferences.app.name = runtimePlatformConfig.adminTitle;
  if (runtimePlatformConfig.copyrightText) {
    preferences.footer.enable = true;
    preferences.copyright.enable = true;
    (preferences.copyright as { text?: string }).text =
      runtimePlatformConfig.copyrightText;
    updatePreferences({
      app: {
        name: runtimePlatformConfig.adminTitle,
      },
      copyright: {
        enable: true,
        text: runtimePlatformConfig.copyrightText,
      },
      footer: {
        enable: true,
      },
    });
  }
  document.title = runtimePlatformConfig.adminTitle;
  return runtimePlatformConfig;
}
