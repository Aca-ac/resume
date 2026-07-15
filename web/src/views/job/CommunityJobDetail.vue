<!-- src/views/job/CommunityJobDetail.vue -->
<script setup lang="ts">
import {ref, onMounted, computed, watch, onUnmounted} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useJob } from '@/composables/useJob';
import { useAuthStore } from '@/stores/auth';
import JdContent from '@/components/JdContent.vue';
import CommentSection from '@/components/CommentSection.vue';
import ForkButton from '@/components/ForkButton.vue';
import type { JobVO } from '@/types/job';
import {
  ArrowLeft,
  User,
  Clock,
  Document,
  Share,
  CopyDocument,
  View,
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const {
  currentJob,
  isLoading,
  isJobOwner,
  fetchJobDetail,
} = useJob();

const jobId = computed(() => Number(route.params.id));
const loading = ref(false);

const sourceMap: Record<number, { label: string; type: string }> = {
  0: { label: '手动创建', type: 'info' },
  1: { label: 'AI搜索', type: 'primary' },
  2: { label: 'Fork', type: 'warning' },
};

const isLoggedIn = computed(() => !!authStore.accessToken);

async function loadJobDetail() {
  if (!jobId.value) return;
  loading.value = true;
  try {
    await fetchJobDetail(jobId.value);
  } catch (err) {
    ElMessage.error('加载岗位详情失败');
  } finally {
    loading.value = false;
  }
}

function onForkSuccess() {
  ElMessage.success('Fork成功！已添加到我的岗位');
}

function goBack() {
  router.push('/community');
}

async function copyJD() {
  if (currentJob.value?.jdContent) {
    try {
      await navigator.clipboard.writeText(currentJob.value.jdContent);
      ElMessage.success('JD内容已复制到剪贴板');
    } catch {
      ElMessage.warning('复制失败，请手动复制');
    }
  }
}

// 键盘快捷键支持：Alt+← 返回
function handleKeydown(event: KeyboardEvent) {
  if (event.altKey && event.key === 'ArrowLeft') {
    event.preventDefault();
    goBack();
  }
}

watch(() => route.params.id, () => {
  if (jobId.value) {
    loadJobDetail();
  }
}, { immediate: true });

onMounted(() => {
  loadJobDetail();
  document.addEventListener('keydown', handleKeydown);
});

// 清理事件监听
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div
      class="community-job-detail-page"
      role="main"
      aria-labelledby="page-title"
  >
    <!-- 主视觉 -->
    <section class="hero-section" aria-label="页面导航">
      <div class="hero-left">
        <el-button
            :icon="ArrowLeft"
            text
            class="back-btn"
            @click="goBack"
            aria-label="返回社区列表"
        >
          返回社区
        </el-button>
        <h1 id="page-title" class="hero-title" v-if="currentJob">
          {{ currentJob.jobName }}
        </h1>
        <h1 id="page-title" class="hero-title" v-else>岗位详情</h1>
      </div>
      <div class="hero-right">
        <el-button
            :icon="View"
            text
            @click="loadJobDetail"
            aria-label="刷新岗位详情"
        >
          刷新
        </el-button>
      </div>
    </section>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container" role="status" aria-live="polite">
      <el-skeleton :rows="10" animated />
      <span class="visually-hidden">正在加载岗位详情</span>
    </div>

    <!-- 详情内容 -->
    <template v-else-if="currentJob">
      <article class="detail-card" aria-labelledby="job-title">
        <!-- 岗位头部 -->
        <header class="job-header">
          <div class="header-main">
            <h2 id="job-title" class="job-title">{{ currentJob.jobName }}</h2>
            <div class="header-tags" role="list">
              <el-tag
                  :type="sourceMap[currentJob.source]?.type || 'info'"
                  size="small"
                  role="listitem"
              >
                {{ sourceMap[currentJob.source]?.label || '未知' }}
              </el-tag>
              <el-tag
                  v-if="currentJob.originalJobId"
                  type="warning"
                  size="small"
                  effect="plain"
                  role="listitem"
              >
                Fork 自 #{{ currentJob.originalJobId }}
              </el-tag>
            </div>
          </div>

          <div class="header-actions">
            <ForkButton
                v-if="isLoggedIn"
                :job-id="currentJob.id"
                :job-name="currentJob.jobName"
                :owner-id="currentJob.owner?.id || 0"
                size="large"
                @success="onForkSuccess"
            />
            <el-tooltip v-else content="请先登录才能 Fork">
              <el-button type="primary" size="large" disabled aria-disabled="true">
                登录后 Fork
              </el-button>
            </el-tooltip>
          </div>
        </header>

        <!-- 岗位元信息 -->
        <dl class="job-meta">
          <div class="meta-item">
            <dt class="visually-hidden">发布者</dt>
            <dd class="meta-content">
              <el-icon aria-hidden="true"><User /></el-icon>
              <span>发布者：</span>
              <el-avatar
                  :size="20"
                  :src="currentJob.owner?.avatar || undefined"
                  class="owner-avatar"
                  :alt="`${currentJob.owner?.nickname || '用户'}的头像`"
              >
                {{ currentJob.owner?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="owner-name">{{ currentJob.owner?.nickname || '未知用户' }}</span>
              <el-tag v-if="isJobOwner" size="small" type="success" effect="plain">
                我的岗位
              </el-tag>
            </dd>
          </div>
          <div class="meta-item">
            <dt class="visually-hidden">创建时间</dt>
            <dd class="meta-content">
              <el-icon aria-hidden="true"><Clock /></el-icon>
              <span>创建时间：</span>
              <time :datetime="currentJob.createdAt">
                {{ new Date(currentJob.createdAt).toLocaleString('zh-CN') }}
              </time>
            </dd>
          </div>
          <div v-if="currentJob.updatedAt !== currentJob.createdAt" class="meta-item">
            <dt class="visually-hidden">更新时间</dt>
            <dd class="meta-content">
              <el-icon aria-hidden="true"><Clock /></el-icon>
              <span>更新时间：</span>
              <time :datetime="currentJob.updatedAt">
                {{ new Date(currentJob.updatedAt).toLocaleString('zh-CN') }}
              </time>
            </dd>
          </div>
        </dl>

        <!-- JD内容 -->
        <section class="jd-section" aria-labelledby="jd-section-title">
          <div class="section-header">
            <div id="jd-section-title" class="section-title">
              <el-icon aria-hidden="true"><Document /></el-icon>
              <span>岗位描述 (JD)</span>
            </div>
            <el-button
                size="small"
                text
                :icon="CopyDocument"
                @click="copyJD"
                aria-label="复制岗位描述内容"
            >
              复制JD
            </el-button>
          </div>
          <div class="jd-wrapper" role="document" aria-label="岗位描述内容">
            <JdContent
                :content="currentJob.jdContent"
                :job-name="currentJob.jobName"
                :show-header="false"
            />
          </div>
        </section>

        <!-- 操作栏 -->
        <footer class="action-bar">
          <el-divider aria-hidden="true" />
          <div class="action-buttons">
            <el-button
                class="browse-more-btn"
                @click="router.push('/community')"
                aria-label="浏览更多岗位"
            >
              <el-icon aria-hidden="true"><Share /></el-icon>
              浏览更多
            </el-button>
            <ForkButton
                v-if="isLoggedIn"
                :job-id="currentJob.id"
                :job-name="currentJob.jobName"
                :owner-id="currentJob.owner?.id || 0"
                @success="onForkSuccess"
            />
          </div>
        </footer>
      </article>

      <!-- 评论区域 -->
      <section class="comment-wrapper" aria-labelledby="comment-section-title">
        <h3 id="comment-section-title" class="visually-hidden">评论区域</h3>
        <CommentSection :job-id="currentJob.id" :auto-load="true" />
      </section>
    </template>

    <!-- 空状态 -->
    <div v-else role="status" aria-live="polite">
      <el-empty description="岗位不存在或已被删除" :image-size="120">
        <el-button type="primary" @click="goBack">返回社区</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
/* ===== 视觉隐藏辅助类 ===== */
.visually-hidden {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
}

.community-job-detail-page {
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
  /* 提高对比度 */
  background: rgba(251, 252, 205, 0.92);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  flex-wrap: wrap;
  gap: 12px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  /* 提高对比度 */
  color: #2d5a4a;
  font-weight: 500;
}

.back-btn:hover {
  color: #1a3a2a;
}

/* 焦点样式 */
.back-btn:focus-visible,
.hero-right .el-button:focus-visible {
  outline: 3px solid #2d5a4a;
  outline-offset: 2px;
}

.hero-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  /* 提高对比度 */
  color: #1a3a2a;
}

.hero-right {
  display: flex;
  gap: 8px;
}

.hero-right .el-button {
  /* 提高对比度 */
  color: #2d5a4a;
}

.loading-container {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.detail-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 28px 32px;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* 岗位头部 */
.job-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.header-main {
  flex: 1;
  min-width: 200px;
}

.job-title {
  font-size: 24px;
  font-weight: 700;
  /* 提高对比度 #1a1a2e -> #0d0d1a */
  color: #0d0d1a;
  margin: 0 0 8px 0;
}

.header-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.header-actions {
  flex-shrink: 0;
  padding-top: 4px;
}

.header-actions .el-button--primary {
  background: #64A386;
  border-color: #64A386;
  color: #ffffff;
}

.header-actions .el-button--primary:hover:not(:disabled) {
  background: #4a8a6e;
  border-color: #4a8a6e;
}

.header-actions .el-button--primary:focus-visible {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

.header-actions .el-button--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 元信息 - 使用DL语义 */
.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  padding: 12px 0 16px;
  border-bottom: 1px solid rgba(150, 180, 165, 0.4);
  margin-bottom: 20px;
  margin-top: 0;
}

.meta-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  /* 提高对比度 #666 -> #3d3d3d */
  color: #3d3d3d;
}

.meta-content {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0;
}

.meta-item .el-icon {
  font-size: 16px;
  /* 提高对比度 */
  color: #4a7a64;
}

.owner-avatar {
  margin: 0 2px;
}

.owner-name {
  font-weight: 500;
  /* 提高对比度 #333 -> #1a1a1a */
  color: #1a1a1a;
}

/* JD区域 */
.jd-section {
  margin: 8px 0 4px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  /* 提高对比度 #333 -> #1a1a1a */
  color: #1a1a1a;
}

.section-title .el-icon {
  font-size: 18px;
  color: #3d7a5e;
}

.section-header .el-button {
  color: #2d5a4a;
}

.section-header .el-button:focus-visible {
  outline: 3px solid #2d5a4a;
  outline-offset: 2px;
}

.jd-wrapper {
  background: rgba(248, 250, 248, 0.9);
  border-radius: 8px;
  padding: 16px 20px;
  border: 1px solid rgba(150, 180, 165, 0.3);
  max-height: 600px;
  overflow-y: auto;
}

.jd-wrapper::-webkit-scrollbar {
  width: 6px;
}

.jd-wrapper::-webkit-scrollbar-track {
  background: rgba(200, 216, 210, 0.2);
  border-radius: 3px;
}

.jd-wrapper::-webkit-scrollbar-thumb {
  background: #a0b8aa;
  border-radius: 3px;
}

.jd-wrapper::-webkit-scrollbar-thumb:hover {
  background: #8aa898;
}

/* 操作栏 */
.action-bar {
  margin-top: 8px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 浏览更多按钮 - 提高对比度 */
.browse-more-btn {
  background: rgba(44, 77, 61, 0.85);
  border-color: rgba(44, 77, 61, 0.85);
  color: #ffffff;
}

.browse-more-btn:hover {
  background: rgba(44, 77, 61, 0.95);
  border-color: rgba(44, 77, 61, 0.95);
  color: #ffffff;
}

.browse-more-btn:focus-visible {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

.browse-more-btn .el-icon {
  color: #ffffff;
}

.action-buttons .el-button--primary {
  background: #64A386;
  border-color: #64A386;
  color: #ffffff;
}

.action-buttons .el-button--primary:hover:not(:disabled) {
  background: #4a8a6e;
  border-color: #4a8a6e;
}

.action-buttons .el-button--primary:focus-visible {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

/* 评论区域 */
.comment-wrapper {
  margin-top: 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 4px 32px 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* 响应式 */
@media (max-width: 768px) {
  .community-job-detail-page {
    padding: 16px;
  }

  .hero-section {
    padding: 16px 20px;
  }

  .hero-title {
    font-size: 18px;
  }

  .detail-card {
    padding: 20px 16px;
  }

  .job-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    width: 100%;
    justify-content: center;
  }

  .job-title {
    font-size: 20px;
  }

  .comment-wrapper {
    padding: 4px 16px 20px;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .hero-section {
    background: #f0f1c0;
    border: 3px solid #1a3a2a;
  }

  .detail-card {
    background: #ffffff;
    border: 2px solid #1a3a2a;
  }

  .comment-wrapper {
    background: #ffffff;
    border: 2px solid #1a3a2a;
  }

  .job-title {
    color: #000000;
  }

  .browse-more-btn {
    background: #1a3a2a;
    border-color: #1a3a2a;
    color: #ffffff;
  }

  .meta-item {
    color: #000000;
  }

  .jd-wrapper {
    border: 2px solid #1a3a2a;
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>