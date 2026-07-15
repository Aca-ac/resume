<!-- src/views/job/JobDetails.vue -->
<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ArrowLeft,
  Edit,
  Delete,
  Refresh,
  User,
  Clock,
  Document,
  ChatDotRound,
  View,
} from '@element-plus/icons-vue';
import { useJob } from '@/composables/useJob';
import { useAuthStore } from '@/stores/auth';
import JdContent from '@/components/JdContent.vue';
import ForkButton from '@/components/ForkButton.vue';
import CommentSection from '@/components/CommentSection.vue';
import type { JobVO } from '@/types/job';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const {
  currentJob,
  isJobOwner,
  isLoading,
  fetchJobDetail,
  fetchCommunityJobDetail,
  handleDeleteJob,
  refreshMyJobs,
} = useJob();

const isCommunityMode = computed(() => route.query.mode === 'community');
const jobId = computed(() => Number(route.params.id));
const loading = ref(false);
const loaded = ref(false);
const commentRef = ref();

// 用于焦点管理
const pageTitleRef = ref<HTMLElement>();
const jobActionsRef = ref<HTMLElement>();
const deleteDialogRef = ref<HTMLElement>();

const canEdit = computed(() => {
  if (!currentJob?.value) return false;
  return isJobOwner.value;
});

const sourceMap: Record<number, { label: string; type: string; icon: string }> = {
  [0 as number]: { label: '手动创建', type: 'info', icon: '📝' },
  [1 as number]: { label: 'AI搜索', type: 'primary', icon: '🤖' },
  [2 as number]: { label: 'Fork', type: 'warning', icon: '🔀' },
};

function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const currentJobSource = computed(() => {
  if (!currentJob?.value) return null;
  return sourceMap[currentJob?.value.source] || { label: '未知', type: 'info', icon: '📄' };
});

async function loadJobDetail() {
  if (!jobId.value) return;
  loading.value = true;
  try {
    let result: JobVO | null;
    if (isCommunityMode.value) {
      result = await fetchCommunityJobDetail(jobId.value);
    } else {
      result = await fetchJobDetail(jobId.value);
    }
    if (result) {
      loaded.value = true;
      // 加载完成后聚焦到标题
      await new Promise(resolve => setTimeout(resolve, 100));
      pageTitleRef.value?.focus();
    } else {
      ElMessage.error('获取岗位详情失败');
    }
  } catch (err) {
    ElMessage.error('加载岗位详情失败');
  } finally {
    loading.value = false;
  }
}

async function handleDelete() {
  if (!currentJob?.value) return;

  // 使用确认对话框
  try {
    await ElMessageBox.confirm(
        `确定要删除岗位「${currentJob.value.jobName}」吗？此操作不可撤销。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger',
        }
    );

    const success = await handleDeleteJob(jobId.value, currentJob.value.jobName);
    if (success) {
      ElMessage.success('岗位已删除');
      router.push('/jobs');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
}

function handleEdit() {
  router.push(`/jobs/${jobId.value}/edit`);
}

function handleRefresh() {
  loadJobDetail();
  if (commentRef.value) {
    commentRef.value.load?.();
  }
  // 刷新后聚焦到标题
  setTimeout(() => {
    pageTitleRef.value?.focus();
  }, 100);
}

function handleForkSuccess() {
  refreshMyJobs();
  ElMessage.success('Fork成功！已添加到我的岗位');
}

function goBack() {
  if (isCommunityMode.value) {
    router.push('/community');
  } else {
    router.push('/jobs');
  }
}

// 键盘事件：ESC返回列表
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && !loading.value) {
    goBack();
  }
}

watch(
    () => route.params.id,
    () => {
      loaded.value = false;
      loadJobDetail();
    }
);

onMounted(() => {
  loadJobDetail();
  document.addEventListener('keydown', handleKeydown);
  // 设置页面标题
  document.title = '岗位详情 - 招聘管理系统';
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});

// 导入ElMessageBox
import { ElMessageBox } from 'element-plus';
</script>

<template>
  <div class="job-details-page" role="main" aria-labelledby="page-title">
    <!-- 主视觉标语 -->
    <section class="hero-section" aria-label="页面操作栏">
      <div class="hero-left">
        <el-button
            :icon="ArrowLeft"
            text
            class="back-btn"
            @click="goBack"
            aria-label="返回列表"
        >
          返回
        </el-button>
        <h1
            id="page-title"
            ref="pageTitleRef"
            class="hero-title"
            v-if="currentJob"
            tabindex="-1"
        >
          {{ currentJob.jobName }}
        </h1>
        <h1 class="hero-title" v-else tabindex="-1">岗位详情</h1>
        <el-tag
            v-if="isCommunityMode"
            type="success"
            size="small"
            effect="plain"
            aria-label="社区公开岗位"
        >
          社区公开
        </el-tag>
        <el-tag
            v-else
            type="info"
            size="small"
            effect="plain"
            aria-label="我的岗位"
        >
          我的岗位
        </el-tag>
      </div>
      <div class="hero-right">
        <el-button
            :icon="Refresh"
            text
            @click="handleRefresh"
            aria-label="刷新岗位详情"
        >
          刷新
        </el-button>
      </div>
    </section>

    <!-- 加载状态 -->
    <div
        v-loading="loading"
        class="page-body"
        :aria-busy="loading"
        role="region"
        aria-label="岗位详情内容"
    >
      <template v-if="!loading && currentJob">
        <!-- 岗位信息卡片 -->
        <section class="job-info-card" aria-label="岗位基本信息">
          <div class="job-header">
            <div class="job-title-wrapper">
              <h2 class="job-title" id="job-title">{{ currentJob.jobName }}</h2>
              <el-tag
                  v-if="currentJobSource"
                  :type="currentJobSource.type"
                  size="default"
                  effect="plain"
                  :aria-label="`来源：${currentJobSource.label}`"
              >
                {{ currentJobSource.icon }}
                {{ currentJobSource.label }}
              </el-tag>
            </div>
            <div
                class="job-actions"
                ref="jobActionsRef"
                role="group"
                aria-label="岗位操作按钮"
            >
              <template v-if="!isCommunityMode && canEdit">
                <el-button
                    type="primary"
                    :icon="Edit"
                    plain
                    @click="handleEdit"
                    aria-label="编辑岗位"
                >
                  编辑
                </el-button>
                <el-button
                    type="danger"
                    :icon="Delete"
                    plain
                    @click="handleDelete"
                    aria-label="删除岗位"
                >
                  删除
                </el-button>
              </template>

              <template v-if="isCommunityMode || !isJobOwner">
                <ForkButton
                    :job-id="currentJob.id"
                    :job-name="currentJob.jobName"
                    :owner-id="currentJob.owner.id"
                    size="default"
                    @success="handleForkSuccess"
                    aria-label="Fork此岗位到我的岗位列表"
                />
              </template>
            </div>
          </div>

          <!-- 元信息 -->
          <dl class="job-meta" aria-label="岗位元数据">
            <div class="meta-item">
              <dt>
                <el-icon aria-hidden="true"><User /></el-icon>
                <span class="visually-hidden">创建者</span>
              </dt>
              <dd>
                <el-avatar
                    :size="20"
                    :src="currentJob.owner?.avatar || undefined"
                    class="meta-avatar"
                    :alt="`${currentJob.owner?.nickname || '用户'}的头像`"
                >
                  {{ currentJob.owner?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="meta-value">{{ currentJob.owner?.nickname || '未知用户' }}</span>
              </dd>
            </div>
            <div class="meta-item">
              <dt>
                <el-icon aria-hidden="true"><Clock /></el-icon>
                <span class="visually-hidden">创建时间</span>
              </dt>
              <dd>
                <span class="meta-value">{{ formatDateTime(currentJob.createdAt) }}</span>
              </dd>
            </div>
            <div class="meta-item">
              <dt>
                <el-icon aria-hidden="true"><Document /></el-icon>
                <span class="visually-hidden">岗位ID</span>
              </dt>
              <dd>
                <span class="meta-value">
                  #{{ currentJob.id }}
                  <span v-if="currentJob.originalJobId" class="source-id">
                    (Fork自 #{{ currentJob.originalJobId }})
                  </span>
                </span>
              </dd>
            </div>
            <div v-if="currentJob.updatedAt !== currentJob.createdAt" class="meta-item">
              <dt>
                <el-icon aria-hidden="true"><Clock /></el-icon>
                <span class="visually-hidden">更新时间</span>
              </dt>
              <dd>
                <span class="meta-value">{{ formatDateTime(currentJob.updatedAt) }}</span>
              </dd>
            </div>
          </dl>
        </section>

        <!-- JD内容 -->
        <section class="jd-section" aria-labelledby="jd-section-title">
          <div class="section-header">
            <h3 id="jd-section-title" class="section-title">
              <span aria-hidden="true">📋</span>
              岗位描述 (JD)
            </h3>
            <el-tag
                v-if="currentJob.jdContent"
                size="small"
                type="success"
                effect="plain"
                aria-label="已填写"
            >
              已填写
            </el-tag>
            <el-tag
                v-else
                size="small"
                type="warning"
                effect="plain"
                aria-label="暂未填写"
            >
              暂未填写
            </el-tag>
          </div>
          <div
              class="jd-content-wrapper"
              role="document"
              aria-label="岗位描述内容"
          >
            <JdContent
                :content="currentJob.jdContent"
                :job-name="currentJob.jobName"
                :show-header="false"
            />
          </div>
        </section>

        <!-- 评论区域 -->
        <section class="comment-section-wrapper" aria-labelledby="comment-section-title">
          <div class="section-header">
            <h3 id="comment-section-title" class="section-title">
              <el-icon aria-hidden="true"><ChatDotRound /></el-icon>
              评论
              <span class="comment-count" v-if="currentJob.commentCount !== undefined">
                ({{ currentJob.commentCount }})
              </span>
            </h3>
          </div>
          <CommentSection
              ref="commentRef"
              :job-id="currentJob.id"
              :auto-load="true"
              aria-label="岗位评论区域"
          />
        </section>
      </template>

      <!-- 空状态 -->
      <div
          v-else-if="!loading && !currentJob"
          role="status"
          aria-live="polite"
      >
        <el-empty description="岗位不存在或已被删除">
          <el-button type="primary" @click="goBack" aria-label="返回列表">返回列表</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 视觉隐藏但屏幕阅读器可访问 */
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.job-details-page {
  width: 100%;
  min-height: 100%;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
  border-radius: 12px;
}

/* ========== 主视觉 ========== */
.hero-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 20px 32px;
  background: rgba(251, 252, 205, 0.85);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  flex-wrap: wrap;
  gap: 12px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.back-btn {
  color: #4a7a64;
  font-weight: 500;
}

.back-btn:hover {
  color: #2d5a4a;
}

.back-btn:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
}

.hero-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #4a7a64;
}

.hero-title:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
}

.hero-right {
  display: flex;
  gap: 8px;
}

.hero-right .el-button {
  color: #4a7a64;
}

.hero-right .el-button:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
}

/* ========== 主体 ========== */
.page-body {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 28px 32px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  min-height: 400px;
}

/* 岗位信息卡片 */
.job-info-card {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(200, 216, 210, 0.3);
}

.job-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.job-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.job-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
}

.job-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.job-actions .el-button--primary {
  border-color: #64A386;
  color: #64A386;
}

.job-actions .el-button--primary:hover {
  background: #64A386;
  color: #fff;
}

.job-actions .el-button--primary:focus-visible,
.job-actions .el-button--danger:focus-visible {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px 32px;
  margin-top: 8px;
}

.job-meta dt {
  display: flex;
  align-items: center;
  gap: 6px;
}

.job-meta dd {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.meta-item .el-icon {
  color: #8aab9a;
}

.meta-value {
  color: #333;
}

.meta-avatar {
  margin-left: 2px;
}

.source-id {
  color: #909399;
  font-size: 12px;
}

/* 通用区块 */
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.section-title .el-icon {
  font-size: 18px;
  color: #64A386;
}

.comment-count {
  font-weight: 400;
  font-size: 14px;
  color: #909399;
}

.jd-section {
  margin-bottom: 32px;
}

.jd-content-wrapper {
  padding: 16px 20px;
  background: rgba(250, 252, 250, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(200, 216, 210, 0.3);
  min-height: 80px;
}

.jd-content-wrapper:focus-visible {
  outline: 2px solid #64A386;
  outline-offset: 2px;
}

.comment-section-wrapper {
  padding-top: 8px;
  border-top: 1px solid rgba(200, 216, 210, 0.3);
}

/* 响应式 */
@media (max-width: 768px) {
  .job-details-page {
    padding: 16px;
  }

  .hero-section {
    padding: 16px 20px;
  }

  .hero-title {
    font-size: 18px;
  }

  .page-body {
    padding: 20px 16px;
  }

  .job-header {
    flex-direction: column;
  }

  .job-actions {
    width: 100%;
  }

  .job-actions .el-button {
    flex: 1;
    justify-content: center;
  }

  .job-title {
    font-size: 20px;
  }

  .job-meta {
    gap: 12px 20px;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .job-details-page {
    background: #ffffff;
  }

  .hero-section {
    background: #f5f5f5;
    border-color: #000;
  }

  .page-body {
    background: #ffffff;
    border-color: #000;
  }

  .jd-content-wrapper {
    border-color: #000;
    background: #f9f9f9;
  }

  .job-info-card {
    border-bottom-color: #000;
  }
}
</style>