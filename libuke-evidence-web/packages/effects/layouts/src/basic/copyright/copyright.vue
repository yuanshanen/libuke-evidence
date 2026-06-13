<script lang="ts" setup>
interface Props {
  companyName?: string;
  companySiteLink?: string;
  date?: string;
  icp?: string;
  icpLink?: string;
  text?: string;
}

defineOptions({
  name: 'Copyright',
});

withDefaults(defineProps<Props>(), {
  companyName: 'Vben Admin',
  companySiteLink: '',
  date: '2024',
  icp: '',
  icpLink: '',
  text: '',
});
</script>

<template>
  <div class="text-md flex-center">
    <template v-if="text">
      {{ text }}
    </template>
    <template v-else>
      <!-- ICP Link -->
      <a
        v-if="icp"
        :href="icpLink || 'javascript:void(0)'"
        class="mx-1 hover:text-primary-hover"
        target="_blank"
      >
        {{ icp }}
      </a>

      <!-- Copyright Text -->
      Copyright © {{ date }}

      <!-- Company Link -->
      <a
        v-if="companyName"
        :href="companySiteLink || 'javascript:void(0)'"
        class="mx-1 hover:text-primary-hover"
        target="_blank"
      >
        {{ companyName }}
      </a>
    </template>
  </div>
</template>
