<script setup lang="ts">
import { ChatLineSquare, Delete } from '@element-plus/icons-vue';
import type { Comment } from '@/types/job';

defineProps<{
  comment: Comment;
  showActions?: boolean;
}>();

defineEmits<{
  (e: 'reply', commentId: number): void;
  (e: 'delete', commentId: number): void;
}>();

// 格式化时间
function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))}分钟前`;
  }
  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 60 * 1000))}小时前`;
  }
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`;
  }
  return date.toLocaleDateString('zh-CN');
}
</script>

<template>
  <div class="comment-item">
    <div class="comment-avatar">
      <el-avatar :size="36" :src="comment.user?.avatar || undefined">
        {{ comment.user?.nickname?.charAt(0) || 'U' }}
      </el-avatar>
    </div>
    <div class="comment-body">
      <div class="comment-header">
        <span class="comment-user">{{ comment.user?.nickname || '未知用户' }}</span>
        <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div v-if="showActions" class="comment-actions">
        <el-button type="text" size="small" @click="$emit('reply', comment.id)">
          <el-icon><ChatLineSquare /></el-icon>
          回复
        </el-button>
        <el-button type="text" size="small" class="delete-btn" @click="$emit('delete', comment.id)">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.comment-user {
  font-weight: 500;
  font-size: 14px;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  word-break: break-word;
}

.comment-actions {
  margin-top: 6px;
  display: flex;
  gap: 4px;
}

.comment-actions .el-button {
  padding: 0 4px;
  font-size: 12px;
}

.comment-actions .delete-btn {
  color: #f56c6c;
}

.comment-actions .delete-btn:hover {
  color: #f23030;
}
</style>