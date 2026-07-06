<template>
  <el-card class="preview-card" :class="{ highlight }">
    <template #header>
      <span class="header-title">{{ title }}</span>
      <el-tag v-if="highlight" type="success" size="small" effect="dark" class="header-tag">AI</el-tag>
    </template>
    <pre class="content">{{ content }}</pre>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ title: string; content: string; highlight?: boolean }>();
</script>

<style scoped>
.preview-card {
  border-radius: var(--app-radius);
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

.preview-card.highlight {
  border: 1px solid rgba(103, 194, 58, 0.4);
  box-shadow: 0 8px 24px rgba(103, 194, 58, 0.12);
}

.preview-card :deep(.el-card__header) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-weight: 600;
}

.content {
  white-space: pre-wrap;
  font-family: inherit;
  margin: 0;
  line-height: 1.7;
  color: #606266;
}
</style>