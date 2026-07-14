<!-- src/views/job/CommunityJobDetail.vue -->
<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
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

function copyJD() {
  if (currentJob.value?.jdContent) {
    navigator.clipboard.writeText(currentJob.value.jdContent).then(() => {
      ElMessage.success('JD内容已复制到剪贴板');
    }).catch(() => {
      ElMessage.warning('复制失败，请手动复制');
    });
  }
}

watch(() => route.params.id, () => {
  if (jobId.value) {
    loadJobDetail();
  }
}, { immediate: true });

onMounted(() => {
  loadJobDetail();
});
</script>

<template>
  <div class="community-job-detail-page">
    <!-- 主视觉 -->
    <section class="hero-section">
      <div class="hero-left">
        <el-button :icon="ArrowLeft" text class="back-btn" @click="goBack">
          返回社区
        </el-button>
        <h1 class="hero-title" v-if="currentJob">{{ currentJob.jobName }}</h1>
        <h1 class="hero-title" v-else>岗位详情</h1>
      </div>
      <div class="hero-right">
        <el-button :icon="View" text @click="loadJobDetail">
          刷新
        </el-button>
      </div>
    </section>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 详情内容 -->
    <template v-else-if="currentJob">
      <div class="detail-card">
        <!-- 岗位头部 -->
        <div class="job-header">
          <div class="header-main">
            <h1 class="job-title">{{ currentJob.jobName }}</h1>
            <div class="header-tags">
              <el-tag :type="sourceMap[currentJob.source]?.type || 'info'" size="small">
                {{ sourceMap[currentJob.source]?.label || '未知' }}
              </el-tag>
              <el-tag
                  v-if="currentJob.originalJobId"
                  type="warning"
                  size="small"
                  effect="plain"
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
              <el-button type="primary" size="large" disabled>
                登录后 Fork
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 岗位元信息 -->
        <div class="job-meta">
          <div class="meta-item">
            <el-icon><User /></el-icon>
            <span>发布者：</span>
            <el-avatar :size="20" :src="currentJob.owner?.avatar || undefined" class="owner-avatar">
              {{ currentJob.owner?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="owner-name">{{ currentJob.owner?.nickname || '未知用户' }}</span>
            <el-tag v-if="isJobOwner" size="small" type="success" effect="plain">
              我的岗位
            </el-tag>
          </div>
          <div class="meta-item">
            <el-icon><Clock /></el-icon>
            <span>创建时间：</span>
            <span>{{ new Date(currentJob.createdAt).toLocaleString('zh-CN') }}</span>
          </div>
          <div v-if="currentJob.updatedAt !== currentJob.createdAt" class="meta-item">
            <el-icon><Clock /></el-icon>
            <span>更新时间：</span>
            <span>{{ new Date(currentJob.updatedAt).toLocaleString('zh-CN') }}</span>
          </div>
        </div>

        <!-- JD内容 -->
        <div class="jd-section">
          <div class="section-header">
            <div class="section-title">
              <el-icon><Document /></el-icon>
              <span>岗位描述 (JD)</span>
            </div>
            <el-button size="small" text :icon="CopyDocument" @click="copyJD">
              复制JD
            </el-button>
          </div>
          <div class="jd-wrapper">
            <JdContent
                :content="currentJob.jdContent"
                :job-name="currentJob.jobName"
                :show-header="false"
            />
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-divider />
          <div class="action-buttons">
            <el-button class="browse-more-btn" @click="router.push('/community')">
              <el-icon><Share /></el-icon>
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
        </div>
      </div>

      <!-- 评论区域 -->
      <div class="comment-wrapper">
        <CommentSection :job-id="currentJob.id" :auto-load="true" />
      </div>
    </template>

    <!-- 空状态 -->
    <el-empty v-else description="岗位不存在或已被删除" :image-size="120">
      <el-button type="primary" @click="goBack">返回社区</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
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
  gap: 16px;
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
  font-size: 22px;
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

.loading-container {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.detail-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 28px 32px;
  border: 1px solid rgba(255, 255, 255, 0.4);
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
  color: #1a1a2e;
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
}

.header-actions .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

/* 元信息 */
.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  padding: 12px 0 16px;
  border-bottom: 1px solid rgba(200, 216, 210, 0.3);
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.meta-item .el-icon {
  font-size: 16px;
  color: #8aab9a;
}

.owner-avatar {
  margin: 0 2px;
}

.owner-name {
  font-weight: 500;
  color: #333;
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
  color: #333;
}

.section-title .el-icon {
  font-size: 18px;
  color: #64A386;
}

.jd-wrapper {
  background: rgba(250, 252, 250, 0.8);
  border-radius: 8px;
  padding: 16px 20px;
  border: 1px solid rgba(200, 216, 210, 0.3);
  max-height: 600px;
  overflow-y: auto;
}

.jd-wrapper::-webkit-scrollbar {
  width: 4px;
}

.jd-wrapper::-webkit-scrollbar-thumb {
  background: #d0d7de;
  border-radius: 2px;
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

/* 浏览更多按钮 - 半透明背景，白色文字 */
.browse-more-btn {
  background: rgba(100, 163, 134, 0.3);
  border-color: rgba(100, 163, 134, 0.3);
  color: #ffffff;
}

.browse-more-btn:hover {
  background: rgba(100, 163, 134, 0.5);
  border-color: rgba(100, 163, 134, 0.5);
  color: #ffffff;
}

.action-buttons .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.action-buttons .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

/* 评论区域 */
.comment-wrapper {
  margin-top: 24px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 4px 32px 24px;
  border: 1px solid rgba(255, 255, 255, 0.4);
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
</style>