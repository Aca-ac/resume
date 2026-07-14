<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useComment } from '@/composables/useComment';
import CommentItem from './CommentItem.vue';
import type { Comment } from '@/types/job';

const props = defineProps<{
  jobId: number;
  autoLoad?: boolean;
}>();

const {
  commentContent,
  submitting,
  isLoading,
  comments,
  total,
  loadComments,
  submitComment,
  resetComment,
} = useComment(props.jobId);

// 分页
const page = ref(1);
const pageSize = ref(10);

// 是否为空
const isEmpty = computed(() => comments.value.length === 0);

// 加载评论
async function load(pageNum?: number) {
  if (pageNum !== undefined) page.value = pageNum;
  await loadComments(page.value, pageSize.value);
}

// 提交评论
async function handleSubmit() {
  const success = await submitComment();
  if (success) {
    // 刷新列表并回到第一页
    page.value = 1;
    await load(1);
  }
}

// 删除评论（预留，需要接口支持）
function handleDelete(commentId: number) {
  // TODO: 调用删除评论API
  console.log('删除评论', commentId);
}

// 回复评论（预留）
function handleReply(commentId: number) {
  // TODO: 实现回复功能
  console.log('回复评论', commentId);
}

// 自动加载
if (props.autoLoad !== false) {
  load();
}

// 监听jobId变化重新加载
watch(() => props.jobId, () => {
  page.value = 1;
  load(1);
});
</script>

<template>
  <div class="comment-section">
    <div class="comment-header">
      <span class="comment-title">评论</span>
      <span v-if="total > 0" class="comment-count">({{ total }})</span>
    </div>

    <!-- 评论输入框 -->
    <div class="comment-input-wrapper">
      <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          maxlength="5000"
          show-word-limit
          :disabled="submitting"
      />
      <div class="input-actions">
        <el-button
            type="primary"
            :loading="submitting"
            :disabled="!commentContent.trim()"
            @click="handleSubmit"
        >
          发表评论
        </el-button>
      </div>
    </div>

    <!-- 评论列表 -->
    <div v-loading="isLoading" class="comment-list">
      <template v-if="!isLoading && !isEmpty">
        <CommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :show-actions="true"
            @delete="handleDelete"
            @reply="handleReply"
        />
      </template>

      <el-empty v-else-if="!isLoading && isEmpty" description="暂无评论，快来发表第一条评论吧！" :image-size="80" />
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="comment-pagination">
      <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
      />
    </div>
  </div>
</template>

<style scoped>
.comment-section {
  padding: 16px 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.comment-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.comment-count {
  font-size: 14px;
  color: #999;
}

.comment-input-wrapper {
  margin-bottom: 20px;
}

.comment-input-wrapper :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: vertical;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  min-height: 60px;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>