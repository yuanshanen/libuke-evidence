<script lang="ts" setup>
import { AuthPageLayout } from '@vben/layouts';
</script>

<template>
  <AuthPageLayout
    class="linyingtai-auth"
    page-description="小区问题上报与物业处理平台"
    page-title="邻应台物业工作台"
  >
    <template #logo></template>
  </AuthPageLayout>
</template>

<style scoped>
.linyingtai-auth :deep(.login-background) {
  background:
    linear-gradient(90deg, rgb(255 255 255 / 0%) 0%, rgb(255 255 255 / 62%) 54%, #f8fbff 100%),
    url('../assets/brand/linyingtai-login-bg.png') left center / auto 100% no-repeat;
  filter: none;
}

.linyingtai-auth :deep(.dark .login-background),
.linyingtai-auth.dark :deep(.login-background) {
  background:
    linear-gradient(90deg, rgb(15 23 42 / 10%) 0%, rgb(15 23 42 / 70%) 54%, #08111f 100%),
    url('../assets/brand/linyingtai-login-bg.png') left center / auto 100% no-repeat;
  filter: none;
}

.linyingtai-auth :deep(.login-background + .mr-20) {
  display: none;
}
</style>
