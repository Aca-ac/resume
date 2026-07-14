<!-- src/views/job/JobDetails.vue -->
<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
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
  const success = await handleDeleteJob(jobId.value, currentJob.value.jobName);
  if (success) {
    router.push('/jobs');
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
}

function handleForkSuccess() {
  refreshMyJobs();
}

function goBack() {
  if (isCommunityMode.value) {
    router.push('/community');
  } else {
    router.push('/jobs');
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
});
</script>

<template>
  <div class="job-details-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <div class="hero-left">
        <el-button :icon="ArrowLeft" text class="back-btn" @click="goBack">返回</el-button>
        <h1 class="hero-title" v-if="currentJob">{{ currentJob.jobName }}</h1>
        <h1 class="hero-title" v-else>岗位详情</h1>
        <el-tag
            v-if="isCommunityMode"
            type="success"
            size="small"
            effect="plain"
        >
          社区公开
        </el-tag>
        <el-tag
            v-else
            type="info"
            size="small"
            effect="plain"
        >
          我的岗位
        </el-tag>
      </div>
      <div class="hero-right">
        <el-button :icon="Refresh" text @click="handleRefresh">
          刷新
        </el-button>
      </div>
    </section>

    <!-- 加载状态 -->
    <div v-loading="loading" class="page-body">
      <template v-if="!loading && currentJob">
        <!-- 岗位信息卡片 -->
        <div class="job-info-card">
          <div class="job-header">
            <div class="job-title-wrapper">
              <h1 class="job-title">{{ currentJob.jobName }}</h1>
              <el-tag
                  v-if="currentJobSource"
                  :type="currentJobSource.type"
                  size="default"
                  effect="plain"
              >
                {{ currentJobSource.icon }}
                {{ currentJobSource.label }}
              </el-tag>
            </div>
            <div class="job-actions">
              <template v-if="!isCommunityMode && canEdit">
                <el-button
                    type="primary"
                    :icon="Edit"
                    plain
                    @click="handleEdit"
                >
                  编辑
                </el-button>
                <el-button
                    type="danger"
                    :icon="Delete"
                    plain
                    @click="handleDelete"
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
                />
              </template>
            </div>
          </div>

          <!-- 元信息 -->
          <div class="job-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span class="meta-label">创建者</span>
              <el-avatar
                  :size="20"
                  :src="currentJob.owner?.avatar || undefined"
                  class="meta-avatar"
              >
                {{ currentJob.owner?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="meta-value">{{ currentJob.owner?.nickname || '未知用户' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Clock /></el-icon>
              <span class="meta-label">创建时间</span>
              <span class="meta-value">{{ formatDateTime(currentJob.createdAt) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Document /></el-icon>
              <span class="meta-label">来源ID</span>
              <span class="meta-value">
                #{{ currentJob.id }}
                <span v-if="currentJob.originalJobId" class="source-id">
                  (Fork自 #{{ currentJob.originalJobId }})
                </span>
              </span>
            </div>
            <div v-if="currentJob.updatedAt !== currentJob.createdAt" class="meta-item">
              <el-icon><Clock /></el-icon>
              <span class="meta-label">更新时间</span>
              <span class="meta-value">{{ formatDateTime(currentJob.updatedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- JD内容 -->
        <div class="jd-section">
          <div class="section-header">
            <span class="section-title">📋 岗位描述 (JD)</span>
            <el-tag v-if="currentJob.jdContent" size="small" type="success" effect="plain">
              已填写
            </el-tag>
            <el-tag v-else size="small" type="warning" effect="plain">
              暂未填写
            </el-tag>
          </div>
          <div class="jd-content-wrapper">
            <JdContent
                :content="currentJob.jdContent"
                :job-name="currentJob.jobName"
                :show-header="false"
            />
          </div>
        </div>

        <!-- 评论区域 -->
        <div class="comment-section-wrapper">
          <div class="section-header">
            <span class="section-title">
              <el-icon><ChatDotRound /></el-icon>
              评论
            </span>
          </div>
          <CommentSection
              ref="commentRef"
              :job-id="currentJob.id"
              :auto-load="true"
          />
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty v-else-if="!loading && !currentJob" description="岗位不存在或已被删除">
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
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

.hero-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #4a7a64;
}

.hero-right {
  display: flex;
  gap: 8px;
}

.hero-right .el-button {
  color: #4a7a64;
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

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px 32px;
  margin-top: 8px;
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

.meta-label {
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
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.section-title .el-icon {
  font-size: 18px;
  color: #64A386;
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
</style>