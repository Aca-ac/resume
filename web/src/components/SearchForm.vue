<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { MagicStick } from '@element-plus/icons-vue';

const props = defineProps<{
  modelValue: string;
  placeholder?: string;
  loading?: boolean;
  showAISearch?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'search', value: string): void;
  (e: 'ai-search', value: string): void;
  (e: 'clear'): void;
}>();

// 使用 computed 实现双向绑定
const keyword = computed({
  get: () => props.modelValue,
  set: (val: string) => {
    emit('update:modelValue', val);
  },
});

// AI搜索加载状态（组件内部管理）
const aiLoading = ref(false);

// 搜索处理
function handleSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }
  emit('search', keyword.value.trim());
}

// AI搜索处理
function handleAISearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入岗位名称');
    return;
  }
  aiLoading.value = true;
  emit('ai-search', keyword.value.trim());
}

// 清空处理
function handleClear() {
  emit('clear');
}

// 键盘事件
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    handleSearch();
  }
}

// 暴露 aiLoading 给父组件控制
defineExpose({
  aiLoading,
  setAiLoading: (loading: boolean) => {
    aiLoading.value = loading;
  },
});
</script>

<template>
  <div class="search-form">
    <div class="search-input-wrapper">
      <el-input
          :model-value="keyword"
          :placeholder="placeholder || '搜索岗位名称...'"
          clearable
          size="large"
          @update:model-value="(val: string) => keyword = val"
          @keydown="handleKeydown"
          @clear="handleClear"
      >
        <template #append>
          <el-button
              type="primary"
              :loading="loading"
              @click="handleSearch"
          >
            🔍
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- AI搜索按钮 -->
    <div v-if="showAISearch" class="ai-search-wrapper">
      <el-button
          type="warning"
          plain
          :loading="aiLoading"
          :disabled="!keyword.trim()"
          @click="handleAISearch"
      >
        <el-icon><MagicStick /></el-icon>
        AI搜索补全JD
      </el-button>
      <span class="ai-tip">AI联网搜索岗位详情，智能补全JD</span>
    </div>
  </div>
</template>

<style scoped>
.search-form {
  width: 100%;
  max-width: 720px;
}

.search-input-wrapper :deep(.el-input-group__append) {
  padding: 0;
}

.search-input-wrapper :deep(.el-input-group__append .el-button) {
  border-radius: 0 4px 4px 0;
  padding: 0 16px;
  font-size: 18px;
}

.ai-search-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.ai-tip {
  font-size: 12px;
  color: #999;
}

.ai-tip::before {
  content: '💡 ';
}
</style>