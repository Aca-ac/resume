<!-- src/views/job/JobList.vue -->
<template>
  <div class="job-list-page" role="main" aria-labelledby="page-title">
    <!-- 主视觉 -->
    <section class="hero-section" aria-label="页面标题区">
      <h1 id="page-title" class="hero-title" tabindex="-1">🎯 我的目标岗位</h1>
      <p class="hero-subtitle">
        管理你的目标岗位，AI智能搜索补全JD，一键Fork社区优质岗位，打造专属求职清单。
      </p>
    </section>

    <!-- 操作栏 -->
    <section class="toolbar-section" aria-label="搜索和操作工具栏">
      <div class="toolbar-left">
        <SearchForm
            ref="searchFormRef"
            v-model="keyword"
            :loading="isLoading"
            placeholder="搜索我的岗位..."
            show-ai-search
            @search="handleSearch"
            @ai-search="handleAISearch"
            @clear="handleClear"
        />
      </div>
      <div class="toolbar-right">
        <el-button
            type="primary"
            size="large"
            @click="goCreate"
            aria-label="创建新岗位"
        >
          <el-icon aria-hidden="true"><Plus /></el-icon>
          创建岗位
        </el-button>
      </div>
    </section>

    <!-- 统计信息 -->
    <section class="stats-section" aria-label="岗位统计信息">
      <span class="stats-text" aria-live="polite">
        共 <strong>{{ filteredJobs.length }}</strong> 个目标岗位
      </span>
      <span v-if="keyword.trim()" class="stats-tag" role="status">
        搜索: "{{ keyword }}"
        <el-icon
            class="clear-tag"
            @click="handleClear"
            aria-label="清除搜索关键词"
            role="button"
            tabindex="0"
            @keydown.enter="handleClear"
            @keydown.space.prevent="handleClear"
        >
          <Close />
        </el-icon>
      </span>
    </section>

    <!-- 岗位列表 -->
    <section
        v-loading="isLoading"
        class="job-grid-section"
        :aria-busy="isLoading"
        aria-label="岗位列表"
    >
      <template v-if="!isLoading && filteredJobs && filteredJobs.length > 0">
        <div class="job-grid" role="list" aria-label="岗位卡片列表">
          <JobCard
              v-for="job in paginatedJobs"
              :key="job.id"
              :job="job"
              show-owner
              clickable
              @click="handleCardClick"
          />
        </div>

        <div v-if="filteredJobs.length > pageSize" class="pagination-wrapper">
          <el-pagination
              v-model:current-page="page"
              :page-size="pageSize"
              :total="filteredJobs.length"
              layout="prev, pager, next, total"
              @current-change="onPageChange"
              aria-label="分页导航"
          />
        </div>
      </template>

      <div
          v-else-if="!isLoading && (!filteredJobs || filteredJobs.length === 0)"
          role="status"
          aria-live="polite"
      >
        <el-empty :description="emptyDescription" :image-size="120">
          <template #description>
            <p style="color: #8a9ba8; margin-bottom: 12px;">
              {{ emptyMessage }}
            </p>
            <p v-if="!keyword.trim()" style="color: #b0c4ce; font-size: 13px;">
              💡 点击「创建岗位」开始添加，或去
              <el-link type="primary" @click="goCommunity" aria-label="前往岗位社区发现优质岗位">
                岗位社区
              </el-link>
              发现优质岗位
            </p>
            <p v-else style="color: #b0c4ce; font-size: 13px;">
              💡 试试其他关键词，或
              <el-link type="primary" @click="handleClear" aria-label="清除搜索">
                清除搜索
              </el-link>
            </p>
          </template>
          <el-button
              v-if="!keyword.trim()"
              type="primary"
              @click="goCreate"
              aria-label="创建我的第一个岗位"
          >
            <el-icon aria-hidden="true"><Plus /></el-icon>
            创建我的第一个岗位
          </el-button>
          <el-button
              v-else
              type="primary"
              plain
              @click="handleClear"
              aria-label="清除搜索"
          >
            清除搜索
          </el-button>
        </el-empty>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Close } from '@element-plus/icons-vue';
import { useJob } from '@/composables/useJob';
import type { JobSimple, CommunityJobVO } from '@/types/job';
import JobCard from '@/components/JobCard.vue';
import SearchForm from '@/components/SearchForm.vue';

const router = useRouter();
const { myJobs, isLoading, refreshMyJobs } = useJob();

// 搜索关键词
const keyword = ref<string>('');
const page = ref<number>(1);
const pageSize = ref<number>(12);

// 搜索表单引用
const searchFormRef = ref<InstanceType<typeof SearchForm> | null>(null);

// 页面标题引用
const pageTitleRef = ref<HTMLElement>();

// 空状态描述
const emptyDescription = computed(() => {
  return keyword.value.trim() ? '未找到匹配的目标岗位' : '暂无目标岗位';
});

const emptyMessage = computed(() => {
  return keyword.value.trim() ? '未找到匹配的目标岗位' : '还没有创建任何目标岗位';
});

// 调试：监听 myJobs 变化
watch(myJobs, (newVal) => {
  console.log('myJobs changed:', newVal);
}, { deep: true });

/**
 * 前端过滤 - 根据关键词筛选岗位列表
 */
const filteredJobs = computed<JobSimple[]>(() => {
  const jobs = myJobs.value || [];
  console.log('filteredJobs - jobs:', jobs);

  if (!keyword.value.trim()) {
    return jobs;
  }
  const kw = keyword.value.trim().toLowerCase();
  return jobs.filter((job: JobSimple) => {
    return job.jobName.toLowerCase().includes(kw);
  });
});

/**
 * 分页后的数据
 */
const paginatedJobs = computed<JobSimple[]>(() => {
  const start = (page.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredJobs.value.slice(start, end);
});

/**
 * 加载岗位列表
 */
async function loadJobs() {
  console.log('loadJobs called');
  try {
    await refreshMyJobs();
    console.log('refreshMyJobs completed, myJobs:', myJobs.value);
    page.value = 1;
    // 加载完成后聚焦到标题
    await nextTick();
    pageTitleRef.value?.focus();
  } catch (error) {
    console.error('loadJobs error:', error);
  }
}

/**
 * 搜索处理 - 前端过滤
 */
function handleSearch(value: string) {
  keyword.value = value;
  page.value = 1;
}

/**
 * AI搜索补全 - 跳转到搜索页
 */
function handleAISearch(value: string) {
  if (searchFormRef.value) {
    searchFormRef.value.setAiLoading(false);
  }
  router.push({
    path: '/jobs/search',
    query: { keyword: value.trim() }
  });
}

/**
 * 清空搜索
 */
function handleClear() {
  keyword.value = '';
  page.value = 1;
  // 清空后聚焦到搜索框
  nextTick(() => {
    const searchInput = document.querySelector('.search-form input') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
    }
  });
}

/**
 * 点击卡片跳转详情 - 跳转到我的岗位详情（带编辑/删除权限）
 */
function handleCardClick(job: JobSimple | CommunityJobVO) {
  router.push(`/jobs/${job.id}`);
}

/**
 * 跳转创建页
 */
function goCreate() {
  router.push('/jobs/create');
}

/**
 * 跳转社区
 */
function goCommunity() {
  router.push('/community');
}

/**
 * 页码变化
 */
function onPageChange(p: number) {
  page.value = p;
  // 分页变化后滚动到列表顶部
  const gridSection = document.querySelector('.job-grid-section');
  if (gridSection) {
    gridSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

/**
 * 键盘事件：Ctrl+Enter 快速搜索
 */
function handleGlobalKeydown(event: KeyboardEvent) {
  // Ctrl+K 或 Cmd+K 聚焦到搜索框
  if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
    event.preventDefault();
    const searchInput = document.querySelector('.search-form input') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
    }
  }
}

/**
 * 刷新列表
 */
defineExpose({
  loadJobs,
});

onMounted(() => {
  console.log('JobList mounted, calling loadJobs');
  loadJobs();
  document.addEventListener('keydown', handleGlobalKeydown);
  document.title = '我的目标岗位 - 招聘管理系统';
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeydown);
});
</script>

<style scoped>
.job-list-page {
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
  background: rgba(251, 252, 205, 0.85);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.hero-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 600;
  color: #4a7a64;
  letter-spacing: 0.5px;
}

.hero-title:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
}

.hero-subtitle {
  margin: 0;
  color: #5a7a6a;
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
}

.toolbar-right .el-button:hover {
  box-shadow: 0 6px 20px rgba(100, 163, 134, 0.35);
  transform: translateY(-1px);
  background: #558f73;
  border-color: #558f73;
}

.toolbar-right .el-button:focus-visible {
  outline: 2px solid #2d5a4a;
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
  color: #5a7a6a;
}

.stats-text strong {
  color: #3d6b57;
  font-size: 18px;
}

.stats-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #4a7a64;
  background: rgba(255, 255, 255, 0.6);
  padding: 4px 12px 4px 16px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.clear-tag {
  cursor: pointer;
  font-size: 14px;
  color: #8aab9a;
  transition: color 0.2s;
}

.clear-tag:hover {
  color: #e74c3c;
}

.clear-tag:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
  border-radius: 50%;
}

/* ========== 岗位网格 ========== */
.job-grid-section {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  padding: 24px 24px 20px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  min-height: 320px;
}

.job-grid-section:focus-visible {
  outline: 2px solid #64A386;
  outline-offset: 2px;
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
  border-top: 1px solid rgba(200, 216, 210, 0.3);
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
}

.pagination-wrapper :deep(.el-pagination .btn-prev),
.pagination-wrapper :deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
}

.pagination-wrapper :deep(.el-pagination .btn-prev:focus-visible),
.pagination-wrapper :deep(.el-pagination .btn-next:focus-visible),
.pagination-wrapper :deep(.el-pagination .el-pager li:focus-visible) {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
}

.pagination-wrapper :deep(.el-pagination .el-pager li) {
  background: transparent;
  border-radius: 8px;
  font-weight: 500;
}

.pagination-wrapper :deep(.el-pagination .el-pager li.is-active) {
  background: #64A386;
  color: #fff;
}

/* ========== 空状态 ========== */
.job-grid-section :deep(.el-empty) {
  padding: 40px 0;
}

.job-grid-section :deep(.el-empty .el-button) {
  border-radius: 10px;
  padding: 12px 28px;
  font-weight: 500;
  background: #64A386;
  border-color: #64A386;
}

.job-grid-section :deep(.el-empty .el-button:hover) {
  background: #558f73;
  border-color: #558f73;
}

.job-grid-section :deep(.el-empty .el-button:focus-visible) {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

.job-grid-section :deep(.el-link:focus-visible) {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
  border-radius: 4px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .job-list-page {
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
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

/* ========== 高对比度模式 ========== */
@media (prefers-contrast: high) {
  .job-list-page {
    background: #ffffff;
  }

  .hero-section {
    background: #f5f5f5;
    border-color: #000;
  }

  .hero-title {
    color: #000;
  }

  .hero-subtitle {
    color: #000;
  }

  .job-grid-section {
    background: #ffffff;
    border-color: #000;
  }

  .stats-tag {
    background: #f0f0f0;
    border: 1px solid #000;
  }

  .pagination-wrapper {
    border-top-color: #000;
  }
}

/* ========== 打印样式 ========== */
@media print {
  .job-list-page {
    background: #fff;
    padding: 20px;
  }

  .hero-section {
    background: #f5f5f5;
    border: 1px solid #ddd;
  }

  .job-grid-section {
    background: #fff;
    border: 1px solid #ddd;
  }

  .toolbar-section {
    display: none;
  }
}
</style>