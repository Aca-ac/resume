<!-- src/views/job/CommunityJobs.vue -->
<script setup lang="ts">
import {ref, onMounted, computed, onUnmounted} from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCommunity } from '@/composables/useCommunity';
import JobCard from '@/components/JobCard.vue';
import SearchForm from '@/components/SearchForm.vue';
import { View, Sort, Plus, Close } from '@element-plus/icons-vue';
import type { CommunityJobVO } from '@/types/job';

const router = useRouter();
const {
  searchKeyword,
  searching,
  isLoading,
  jobs,
  total,
  currentPage,
  pageSize,
  loadJobs,
  searchJobs,
  clearSearch,
  onPageChange,
} = useCommunity();

const sortBy = ref<'newest' | 'oldest'>('newest');

const isEmpty = computed(() => jobs.value.length === 0);

const sortedJobs = computed<CommunityJobVO[]>(() => {
  const list = [...jobs.value];
  if (sortBy.value === 'newest') {
    return list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  } else {
    return list.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
  }
});

function handleSearch(keyword: string) {
  searchJobs(keyword, 1);
}

function handleAISearch(keyword: string) {
  router.push({
    path: '/jobs/search',
    query: { keyword: keyword },
  });
}

function handleJobClick(job: CommunityJobVO) {
  router.push(`/community/jobs/${job.id}`);
}

function toggleSort() {
  sortBy.value = sortBy.value === 'newest' ? 'oldest' : 'newest';
}

function goCreate() {
  router.push('/jobs/create');
}

// 键盘快捷键：Alt+C 创建岗位
function handleKeydown(event: KeyboardEvent) {
  if (event.altKey && event.key === 'c') {
    event.preventDefault();
    goCreate();
  }
}

onMounted(() => {
  loadJobs(1);
  document.addEventListener('keydown', handleKeydown);
});

// 清理事件监听
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div
      class="community-jobs-page"
      role="main"
      aria-labelledby="page-title"
  >
    <!-- 主视觉 -->
    <header class="hero-section" aria-labelledby="page-title">
      <h1 id="page-title" class="hero-title">
        <span aria-hidden="true">🏢</span> 社区岗位
      </h1>
      <p class="hero-subtitle">
        浏览和 Fork 其他用户分享的目标岗位，发现更多职业机会
      </p>
    </header>

    <!-- 操作栏 -->
    <section class="toolbar-section" aria-label="搜索和操作工具栏">
      <div class="toolbar-left">
        <SearchForm
            v-model="searchKeyword"
            :loading="searching"
            placeholder="搜索岗位名称..."
            :show-ai-search="true"
            @search="handleSearch"
            @ai-search="handleAISearch"
            @clear="clearSearch"
        />
      </div>
      <div class="toolbar-right">
        <el-button
            type="primary"
            size="large"
            @click="goCreate"
            aria-label="创建新岗位 (快捷键 Alt+C)"
        >
          <el-icon aria-hidden="true"><Plus /></el-icon>
          创建岗位
        </el-button>
        <span class="visually-hidden">快捷键 Alt+C</span>
      </div>
    </section>

    <!-- 统计信息 -->
    <section class="stats-section" aria-label="岗位统计和筛选">
      <span class="stats-text" role="status" aria-live="polite">
        共 <strong>{{ total }}</strong> 个社区岗位
      </span>
      <span v-if="searchKeyword.trim()" class="stats-tag" role="status">
        搜索: "{{ searchKeyword }}"
        <el-icon
            class="clear-tag"
            @click="clearSearch"
            role="button"
            tabindex="0"
            aria-label="清除搜索关键词"
            @keydown.enter="clearSearch"
            @keydown.space.prevent="clearSearch"
        >
          <Close />
        </el-icon>
      </span>
      <div class="stats-right">
        <el-button
            size="small"
            :icon="Sort"
            @click="toggleSort"
            :aria-label="sortBy === 'newest' ? '切换为最早优先排序' : '切换为最新优先排序'"
        >
          {{ sortBy === 'newest' ? '最新优先' : '最早优先' }}
        </el-button>
        <el-button
            size="small"
            type="primary"
            plain
            @click="loadJobs(1)"
            aria-label="刷新岗位列表"
        >
          <el-icon aria-hidden="true"><View /></el-icon>
          刷新
        </el-button>
      </div>
    </section>

    <!-- 岗位列表 -->
    <section
        v-loading="isLoading"
        class="job-grid-section"
        aria-label="岗位列表"
    >
      <template v-if="!isLoading && !isEmpty">
        <div class="job-grid" role="list">
          <JobCard
              v-for="job in sortedJobs"
              :key="job.id"
              :job="job"
              :show-owner="true"
              clickable
              @click="handleJobClick"
          />
        </div>

        <div v-if="total > pageSize" class="pagination-wrapper">
          <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              :page-sizes="[12, 24, 48]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="onPageChange"
              @size-change="(size: number) => { pageSize = size; loadJobs(1); }"
              aria-label="岗位列表分页导航"
          />
        </div>
      </template>

      <!-- 空状态 -->
      <div
          v-else-if="!isLoading && isEmpty"
          role="status"
          aria-live="polite"
      >
        <el-empty
            description="暂无社区岗位"
            :image-size="120"
        >
          <template #description>
            <p class="empty-description">
              {{ searchKeyword ? '没有找到匹配的岗位，试试其他关键词' : '还没有人分享岗位，快来创建第一个吧！' }}
            </p>
          </template>
          <el-button type="primary" @click="goCreate">
            <el-icon aria-hidden="true"><Plus /></el-icon>
            创建岗位
          </el-button>
        </el-empty>
      </div>

      <!-- 骨架屏 -->
      <div v-if="isLoading && jobs.length === 0" class="skeleton-grid" role="status" aria-live="polite">
        <div v-for="i in 6" :key="i" class="skeleton-card">
          <el-skeleton :rows="3" animated />
        </div>
        <span class="visually-hidden">正在加载岗位列表</span>
      </div>
    </section>
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

.community-jobs-page {
  width: 100%;
  min-height: 100%;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
  border-radius: 12px;
}

/* ========== 主视觉 ========== */
.hero-section {
  text-align: center;
  margin-bottom: 28px;
  padding: 28px 36px;
  /* 提高对比度 */
  background: rgba(251, 252, 205, 0.92);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.hero-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 600;
  /* 提高对比度 */
  color: #1a3a2a;
  letter-spacing: 0.5px;
}

.hero-subtitle {
  margin: 0;
  /* 提高对比度 #5a7a6a -> #3d5a4a */
  color: #3d5a4a;
  font-size: 15px;
  line-height: 1.7;
}

/* ========== 操作栏 ========== */
.toolbar-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
  min-width: 280px;
  max-width: 660px;
}

.toolbar-right {
  flex-shrink: 0;
  padding-top: 2px;
}

.toolbar-right .el-button {
  border-radius: 10px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(100, 163, 134, 0.25);
  background: #64A386;
  border-color: #64A386;
  color: #ffffff;
}

.toolbar-right .el-button:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(100, 163, 134, 0.35);
  transform: translateY(-1px);
  background: #4a8a6e;
  border-color: #4a8a6e;
  color: #ffffff;
}

.toolbar-right .el-button:focus-visible {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

/* ========== 统计信息 ========== */
.stats-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 0 4px;
  flex-wrap: wrap;
}

.stats-text {
  font-size: 14px;
  /* 提高对比度 #5a7a6a -> #3d5a4a */
  color: #3d5a4a;
}

.stats-text strong {
  color: #1a3a2a;
  font-size: 18px;
}

.stats-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1a3a2a;
  background: rgba(255, 255, 255, 0.7);
  padding: 4px 12px 4px 16px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(100, 163, 134, 0.2);
}

.clear-tag {
  cursor: pointer;
  font-size: 14px;
  color: #4a7a64;
  transition: color 0.2s;
  border-radius: 50%;
  padding: 2px;
}

.clear-tag:hover,
.clear-tag:focus {
  color: #c0392b;
}

.clear-tag:focus-visible {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

.stats-right {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.stats-right .el-button {
  color: #1a3a2a;
}

.stats-right .el-button:focus-visible {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

.stats-right .el-button--primary.is-plain {
  color: #3d7a5e;
  border-color: #3d7a5e;
}

.stats-right .el-button--primary.is-plain:hover {
  background: #3d7a5e;
  color: #ffffff;
}

/* ========== 岗位网格 ========== */
.job-grid-section {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  padding: 24px 24px 20px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  min-height: 320px;
}

.job-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(150, 180, 165, 0.4);
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
}

.pagination-wrapper :deep(.el-pagination .btn-prev),
.pagination-wrapper :deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
}

.pagination-wrapper :deep(.el-pagination .btn-prev:focus-visible),
.pagination-wrapper :deep(.el-pagination .btn-next:focus-visible) {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

.pagination-wrapper :deep(.el-pagination .el-pager li) {
  background: transparent;
  border-radius: 8px;
  font-weight: 500;
  color: #1a3a2a;
}

.pagination-wrapper :deep(.el-pagination .el-pager li.is-active) {
  background: #64A386;
  color: #ffffff;
}

.pagination-wrapper :deep(.el-pagination .el-pager li:focus-visible) {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

/* ========== 骨架屏 ========== */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.skeleton-card {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(150, 180, 165, 0.3);
  border-radius: 12px;
}

/* ========== 空状态 ========== */
.empty-description {
  color: #3d5a4a;
  margin-bottom: 12px;
}

.job-grid-section :deep(.el-empty) {
  padding: 40px 0;
}

.job-grid-section :deep(.el-empty .el-button) {
  border-radius: 10px;
  padding: 12px 28px;
  font-weight: 500;
  background: #64A386;
  border-color: #64A386;
  color: #ffffff;
}

.job-grid-section :deep(.el-empty .el-button:hover:not(:disabled)) {
  background: #4a8a6e;
  border-color: #4a8a6e;
}

.job-grid-section :deep(.el-empty .el-button:focus-visible) {
  outline: 3px solid #1a3a2a;
  outline-offset: 2px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .community-jobs-page {
    padding: 16px;
  }

  .hero-section {
    padding: 20px 16px;
  }

  .hero-title {
    font-size: 22px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .toolbar-section {
    flex-direction: column;
  }

  .toolbar-left {
    max-width: 100%;
    min-width: auto;
  }

  .toolbar-right {
    width: 100%;
  }

  .toolbar-right .el-button {
    width: 100%;
    justify-content: center;
  }

  .job-grid {
    grid-template-columns: 1fr;
  }

  .job-grid-section {
    padding: 16px;
  }

  .stats-section {
    flex-wrap: wrap;
  }

  .stats-right {
    margin-left: 0;
    width: 100%;
  }
}

/* ========== 高对比度模式 ========== */
@media (prefers-contrast: high) {
  .hero-section {
    background: #f0f1c0;
    border: 3px solid #1a3a2a;
  }

  .job-grid-section {
    background: #ffffff;
    border: 2px solid #1a3a2a;
  }

  .hero-title {
    color: #000000;
  }

  .stats-text strong {
    color: #000000;
  }

  .stats-tag {
    border: 2px solid #1a3a2a;
  }

  .job-grid-section :deep(.el-empty .el-button) {
    background: #1a3a2a;
    border-color: #1a3a2a;
  }

  .pagination-wrapper {
    border-top: 2px solid #1a3a2a;
  }

  .pagination-wrapper :deep(.el-pagination .el-pager li.is-active) {
    background: #1a3a2a;
  }
}

/* ========== 减少动画 ========== */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .toolbar-right .el-button {
    transition: none;
  }

  .toolbar-right .el-button:hover:not(:disabled) {
    transform: none;
  }
}
</style>