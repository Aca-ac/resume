<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useJob } from '@/composables/useJob';
import { useAuthStore } from '@/stores/auth';
import { Share, Check } from '@element-plus/icons-vue';

const props = defineProps<{
  jobId: number;
  jobName: string;
  ownerId: number;
  size?: 'default' | 'small' | 'large';
}>();

const authStore = useAuthStore();
const { handleForkJob } = useJob();

const forking = ref(false);

// 判断是否为自己的岗位
const isOwnJob = computed(() => {
  return authStore.userInfo?.id === props.ownerId;
});

// Fork按钮文案
const buttonText = computed(() => {
  if (forking.value) return 'Forking...';
  return isOwnJob.value ? '我的岗位' : 'Fork';
});

// 按钮类型
const buttonType = computed(() => {
  return isOwnJob.value ? 'info' : 'primary';
});

// 是否禁用
const disabled = computed(() => {
  return forking.value || isOwnJob.value;
});

const emit = defineEmits<{
  (e: 'success'): void;
}>();

async function handleFork() {
  if (isOwnJob.value) {
    ElMessage.info('这是您自己的岗位');
    return;
  }

  const result = await handleForkJob(props.jobId, props.jobName);
  if (result) {
    emit('success');
  }
}
</script>

<template>
  <el-button
      :type="buttonType"
      :size="size || 'default'"
      :loading="forking"
      :disabled="disabled"
      @click="handleFork"
  >
    <el-icon v-if="!isOwnJob"><Share /></el-icon>
    <el-icon v-else><Check /></el-icon>
    {{ buttonText }}
  </el-button>
</template>

<style scoped>
/* 无需额外样式 */
</style>