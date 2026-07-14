<!-- src/views/job/JobSearch.vue -->
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Search,
  MagicStick,
  Document,
  Plus,
  Refresh,
  InfoFilled,
  Loading,
  ArrowLeft,
} from '@element-plus/icons-vue';
import { useJob } from '@/composables/useJob';
import JdContent from '@/components/JdContent.vue';
import type { JobSearchResult } from '@/types/job';

const router = useRouter();
const route = useRoute();
const { handleSearchJob, handleCreateJob: createJob, isSearching, isLoading } = useJob();

const searchKeyword = ref<string>('');
const searchResult = ref<JobSearchResult | null>(null);
const creating = ref(false);
const searchStep = ref(0);
let stepTimer: ReturnType<typeof setTimeout> | null = null;

const examples = [
  'Java后端开发工程师',
  '前端开发工程师',
  '产品经理',
  '数据分析师',
  'UI/UX设计师',
];

async function handleSearch() {
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    ElMessage.warning('请输入岗位名称');
    return;
  }

  searchStep.value = 0;

  try {
    startStepAnimation();
    const result = await handleSearchJob({ jobName: keyword });
    if (result) {
      searchResult.value = result;
      router.replace({ query: { keyword: keyword } });
    } else {
      searchResult.value = null;
    }
  } catch (error) {
    ElMessage.error('搜索失败，请稍后重试');
    searchResult.value = null;
  } finally {
    stopStepAnimation();
  }
}

function startStepAnimation() {
  searchStep.value = 1;
  let step = 1;
  stepTimer = setInterval(() => {
    step++;
    if (step <= 3) {
      searchStep.value = step;
    } else {
      clearInterval(stepTimer!);
      stepTimer = null;
    }
  }, 1200);
}

function stopStepAnimation() {
  if (stepTimer) {
    clearInterval(stepTimer);
    stepTimer = null;
  }
  searchStep.value = 3;
}

function handleClear() {
  searchKeyword.value = '';
  searchResult.value = null;
  searchStep.value = 0;
  stopStepAnimation();
  router.replace({ query: {} });
}

async function handleCreateJob() {
  if (!searchResult.value) return;

  creating.value = true;
  try {
    const result = await createJob({
      jobName: searchResult.value.jobName,
      jdContent: searchResult.value.jdContent,
    });
    if (result) {
      ElMessage.success('岗位创建成功！');
      router.push('/jobs');
    }
  } catch (error) {
    ElMessage.error('创建失败，请重试');
  } finally {
    creating.value = false;
  }
}

function goBack() {
  router.back();
}

function restoreFromQuery() {
  const keyword = route.query.keyword as string;
  if (keyword) {
    searchKeyword.value = keyword;
    setTimeout(() => {
      handleSearch();
    }, 300);
  }
}

onMounted(() => {
  restoreFromQuery();
});

onUnmounted(() => {
  stopStepAnimation();
});
</script>

<template>
  <div class="job-search-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <div class="hero-left">
        <el-button :icon="ArrowLeft" text class="back-btn" @click="goBack">返回</el-button>
        <h1 class="hero-title">🔍 AI 智能搜索</h1>
      </div>
    </section>

    <!-- 搜索栏 -->
    <section class="search-section">
      <div class="search-container">
        <el-input
            v-model="searchKeyword"
            size="large"
            placeholder="请输入岗位名称，如：Java后端开发工程师"
            clearable
            @keydown.enter="handleSearch"
            @clear="handleClear"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button
                type="primary"
                :loading="isSearching"
                @click="handleSearch"
            >
              <el-icon><MagicStick /></el-icon>
              AI 搜索补全
            </el-button>
          </template>
        </el-input>
        <p class="search-tip">
          <el-icon><InfoFilled /></el-icon>
          支持岗位名称模糊搜索，AI 将联网获取最新招聘信息
        </p>
        <div class="example-tags">
          <span class="example-label">热门搜索：</span>
          <el-tag
              v-for="example in examples"
              :key="example"
              size="small"
              class="example-tag"
              @click="searchKeyword = example"
          >
            {{ example }}
          </el-tag>
        </div>
      </div>
    </section>

    <!-- 搜索结果 -->
    <section v-if="searchResult" class="result-section">
      <div class="result-header">
        <div class="result-title-wrapper">
          <el-icon class="result-icon"><Document /></el-icon>
          <h2 class="result-title">{{ searchResult.jobName }}</h2>
          <el-tag type="success" size="large" effect="plain">
            AI 生成
          </el-tag>
        </div>
        <div class="result-actions">
          <el-button
              type="primary"
              :loading="creating"
              @click="handleCreateJob"
          >
            <el-icon><Plus /></el-icon>
            创建为我的岗位
          </el-button>
          <el-button @click="handleClear">
            <el-icon><Refresh /></el-icon>
            重新搜索
          </el-button>
        </div>
      </div>

      <div v-if="searchResult.sources && searchResult.sources.length > 0" class="sources-section">
        <span class="sources-label">📌 数据来源：</span>
        <el-link
            v-for="(source, index) in searchResult.sources"
            :key="index"
            :href="source"
            target="_blank"
            type="primary"
            class="source-link"
        >
          {{ source }}
        </el-link>
      </div>

      <div class="jd-wrapper">
        <JdContent
            :content="searchResult.jdContent"
            :job-name="searchResult.jobName"
            show-header
        />
      </div>

      <div class="result-footer">
        <el-alert
            type="info"
            :closable="false"
            show-icon
        >
          <template #title>
            <span>
              💡 以上信息由 AI 联网搜索生成，仅供参考。
              请结合自身情况核实确认后，点击「创建为我的岗位」保存到目标岗位列表。
            </span>
          </template>
        </el-alert>
      </div>
    </section>

    <!-- 加载中占位 -->
    <section v-else-if="isSearching" class="loading-section">
      <div class="loading-wrapper">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <h3 class="loading-title">AI 正在联网搜索...</h3>
        <p class="loading-desc">
          正在从各大招聘平台收集「{{ searchKeyword || '目标岗位' }}」的 JD 信息，
          预计需要 3-5 秒
        </p>
        <div class="loading-steps">
          <div class="step" :class="{ active: searchStep >= 1 }">
            <span class="step-num">1</span>
            <span class="step-text">理解岗位需求</span>
          </div>
          <div class="step-line" :class="{ active: searchStep >= 2 }"></div>
          <div class="step" :class="{ active: searchStep >= 2 }">
            <span class="step-num">2</span>
            <span class="step-text">联网搜索信息</span>
          </div>
          <div class="step-line" :class="{ active: searchStep >= 3 }"></div>
          <div class="step" :class="{ active: searchStep >= 3 }">
            <span class="step-num">3</span>
            <span class="step-text">生成 JD 内容</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 空状态 / 初始状态 -->
    <section v-else class="empty-section">
      <el-empty description="输入岗位名称开始 AI 搜索" :image-size="160">
        <template #description>
          <p style="color: #8a9ba8; font-size: 15px; margin-bottom: 8px;">
            输入你想要了解的岗位名称
          </p>
        </template>
        <el-icon class="empty-icon"><MagicStick /></el-icon>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.job-search-page {
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
  font-size: 24px;
  font-weight: 600;
  color: #4a7a64;
}

/* ========== 搜索栏 ========== */
.search-section {
  margin-bottom: 24px;
  padding: 24px 32px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.search-container {
  max-width: 720px;
  margin: 0 auto;
}

.search-container :deep(.el-input-group__append) {
  padding: 0;
}

.search-container :deep(.el-input-group__append .el-button) {
  border-radius: 0 8px 8px 0;
  padding: 0 24px;
  font-weight: 500;
  background: #64A386;
  border-color: #64A386;
}

.search-container :deep(.el-input-group__append .el-button:hover) {
  background: #558f73;
  border-color: #558f73;
}

.search-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 0 0;
  font-size: 13px;
  color: #8aab9a;
}

.search-tip .el-icon {
  font-size: 16px;
}

.example-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.example-label {
  font-size: 13px;
  color: #8aab9a;
}

.example-tag {
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(100, 163, 134, 0.12);
  border-color: rgba(100, 163, 134, 0.2);
  color: #4a7a64;
}

.example-tag:hover {
  background: #64A386;
  color: #fff;
  border-color: #64A386;
}

/* ========== 结果区域 ========== */
.result-section {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 28px 32px 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(200, 216, 210, 0.3);
}

.result-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.result-icon {
  font-size: 24px;
  color: #64A386;
}

.result-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #333;
}

.result-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.result-actions .el-button {
  border-radius: 10px;
  font-weight: 500;
}

.result-actions .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.result-actions .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

/* ========== 数据来源 ========== */
.sources-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: rgba(100, 163, 134, 0.08);
  border-radius: 10px;
}

.sources-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.source-link {
  font-size: 13px;
  word-break: break-all;
}

/* ========== JD 内容 ========== */
.jd-wrapper {
  background: rgba(250, 252, 250, 0.6);
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid rgba(200, 216, 210, 0.3);
}

/* ========== 结果底部 ========== */
.result-footer {
  margin-top: 20px;
}

.result-footer :deep(.el-alert) {
  border-radius: 10px;
  background: rgba(100, 163, 134, 0.06);
  border-color: rgba(100, 163, 134, 0.2);
}

/* ========== 加载中占位 ========== */
.loading-section {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 48px 32px;
  min-height: 320px;
}

.loading-wrapper {
  text-align: center;
  max-width: 500px;
  margin: 0 auto;
}

.loading-icon {
  font-size: 48px;
  color: #64A386;
}

.loading-title {
  margin: 16px 0 8px;
  font-size: 20px;
  font-weight: 500;
  color: #4a7a64;
}

.loading-desc {
  margin: 0 0 28px;
  font-size: 14px;
  color: #8aab9a;
}

.loading-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  opacity: 0.4;
  transition: opacity 0.4s;
}

.step.active {
  opacity: 1;
}

.step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(200, 216, 210, 0.3);
  color: #8aab9a;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.4s;
}

.step.active .step-num {
  background: #64A386;
  color: #fff;
  box-shadow: 0 4px 12px rgba(100, 163, 134, 0.3);
}

.step-text {
  font-size: 12px;
  color: #666;
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(200, 216, 210, 0.3);
  margin: 0 4px;
  margin-bottom: 22px;
  transition: background 0.6s;
}

.step-line.active {
  background: #64A386;
}

/* ========== 空状态 ========== */
.empty-section {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 40px 20px;
  min-height: 320px;
}

.empty-section :deep(.el-empty) {
  padding: 40px 0;
}

.empty-section :deep(.el-empty .el-empty__description) {
  margin-top: 12px;
}

.empty-icon {
  font-size: 56px;
  color: #64A386;
  margin-top: 8px;
  display: block;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .job-search-page {
    padding: 16px;
  }

  .hero-section {
    padding: 16px 20px;
  }

  .hero-title {
    font-size: 20px;
  }

  .search-section {
    padding: 16px 20px;
  }

  .result-section {
    padding: 20px 16px 16px;
  }

  .result-header {
    flex-direction: column;
    align-items: stretch;
  }

  .result-title-wrapper {
    justify-content: center;
  }

  .result-title {
    font-size: 18px;
  }

  .result-actions {
    justify-content: center;
  }

  .result-actions .el-button {
    flex: 1;
  }

  .jd-wrapper {
    padding: 16px;
  }

  .loading-section {
    padding: 32px 16px;
  }

  .loading-steps {
    flex-wrap: wrap;
    gap: 8px;
  }

  .step-line {
    width: 20px;
  }

  .search-container :deep(.el-input-group) {
    flex-direction: column;
  }

  .search-container :deep(.el-input-group__append) {
    margin-top: 8px;
  }

  .search-container :deep(.el-input-group__append .el-button) {
    border-radius: 8px;
    width: 100%;
    justify-content: center;
  }
}
</style>