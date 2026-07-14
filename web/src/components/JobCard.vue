<script setup lang="ts">
import type { JobSimple, CommunityJobVO } from '@/types/job';

// 使用联合类型，并处理两种类型的差异
const props = defineProps<{
  job: JobSimple | CommunityJobVO;
  showOwner?: boolean;
  clickable?: boolean;
}>();

const emit = defineEmits<{
  (e: 'click', job: JobSimple | CommunityJobVO): void;
}>();

// 来源标签
const sourceMap: Record<number, { label: string; type: string }> = {
  0: { label: '手动创建', type: 'info' },
  1: { label: 'AI搜索', type: 'primary' },
  2: { label: 'Fork', type: 'warning' },
};

const sourceInfo = sourceMap[props.job.source] || { label: '未知', type: '' };

// 格式化时间
function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 24 * 60 * 60 * 1000) {
    return '今天';
  }
  if (diff < 2 * 24 * 60 * 60 * 1000) {
    return '昨天';
  }
  return date.toLocaleDateString('zh-CN');
}

function handleClick() {
  if (props.clickable !== false) {
    emit('click', props.job);
  }
}
</script>

<template>
  <div
      class="job-card"
      :class="{ 'is-clickable': clickable !== false }"
      @click="handleClick"
  >
    <div class="card-header">
      <span class="job-name">{{ job.jobName }}</span>
      <el-tag :type="sourceInfo.type" size="small" effect="plain">
        {{ sourceInfo.label }}
      </el-tag>
    </div>

    <div class="card-footer">
      <div v-if="showOwner && job.owner" class="owner-info">
        <el-avatar :size="20" :src="job.owner.avatar || undefined">
          {{ job.owner.nickname?.charAt(0) || 'U' }}
        </el-avatar>
        <span class="owner-name">{{ job.owner.nickname || '未知用户' }}</span>
      </div>
      <span class="create-time">{{ formatDate(job.createdAt) }}</span>
    </div>
  </div>
</template>

<style scoped>
.job-card {
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  transition: all 0.2s ease;
  cursor: default;
}

.job-card.is-clickable {
  cursor: pointer;
}

.job-card.is-clickable:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.job-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.owner-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.owner-name {
  color: #666;
}
</style>