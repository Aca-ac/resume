<!-- src/views/job/JobCreate.vue -->
<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, DocumentAdd, Plus, Search, MagicStick, Document, Check, Loading } from '@element-plus/icons-vue';
import { useJob } from '@/composables/useJob';
import type { JobCreateRequest } from '@/types/job';
import JdContent from '@/components/JdContent.vue';

const router = useRouter();
const { handleCreateJob, handleSearchJob, clearSearchResult, searchResult, isSearching } = useJob();

const form = ref<JobCreateRequest>({
  jobName: '',
  jdContent: '',
});

const rules = {
  jobName: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { min: 2, max: 100, message: '岗位名称长度为2-100字符', trigger: 'blur' },
  ],
};

const formRef = ref();
const submitting = ref(false);
const searchKeyword = ref('');
const useAiResult = ref(false);

// 用于焦点管理
const searchInputRef = ref<HTMLInputElement>();
const aiResultRef = ref<HTMLElement>();
const submitBtnRef = ref<HTMLElement>();

// 监听搜索关键词变化，自动同步到表单的岗位名称
watch(searchKeyword, (newVal) => {
  if (newVal.trim() && !form.value.jobName) {
    form.value.jobName = newVal.trim();
  }
});

// AI搜索结果
const aiResult = computed(() => {
  const result = searchResult?.value;
  if (!result) return null;
  return {
    jobName: result.jobName || '',
    jdContent: result.jdContent || '',
    sources: Array.isArray(result.sources) ? result.sources : [],
  };
});

// 执行AI搜索
async function handleAiSearch(keyword: string) {
  if (!keyword.trim()) {
    ElMessage.warning('请输入岗位名称');
    // 聚焦到搜索输入框
    searchInputRef.value?.focus();
    return;
  }

  clearSearchResult();

  const result = await handleSearchJob({
    jobName: keyword.trim(),
    existingJd: form.value.jdContent || undefined
  });

  if (result) {
    ElMessage.success('AI搜索完成，请查看结果并点击"应用此JD"');
    // 将焦点移到AI结果区域
    await nextTick();
    aiResultRef.value?.focus();
  }
}

// 应用AI结果到表单
function applyAiResult() {
  const result = aiResult.value;
  if (!result) {
    ElMessage.warning('没有可应用的AI搜索结果');
    return;
  }

  form.value.jdContent = result.jdContent;

  if (!form.value.jobName || form.value.jobName === searchKeyword.value) {
    form.value.jobName = result.jobName || searchKeyword.value;
  }

  useAiResult.value = true;
  ElMessage.success('已应用AI搜索补全的JD内容');

  // 聚焦到JD内容文本域
  nextTick(() => {
    const textarea = document.querySelector('.jd-content-textarea textarea') as HTMLTextAreaElement;
    if (textarea) {
      textarea.focus();
      // 滚动到文本域位置
      textarea.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  });
}

// 清除AI结果
function clearAiResult() {
  clearSearchResult();
  useAiResult.value = false;
  // 聚焦到搜索输入框
  searchInputRef.value?.focus();
}

// 提交表单 - 改为保存
async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    // 聚焦到第一个错误字段
    const firstError = document.querySelector('.el-form-item.is-error input, .el-form-item.is-error textarea') as HTMLElement;
    if (firstError) {
      firstError.focus();
    }
    return;
  }

  submitting.value = true;
  try {
    const result = await handleCreateJob(form.value);
    if (result) {
      ElMessage.success('岗位创建成功');
      router.push(`/jobs/${result.id}`);
    }
  } catch (error) {
    console.error('创建岗位失败:', error);
    ElMessage.error('创建岗位失败，请重试');
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  router.back();
}

// 键盘事件：ESC关闭AI结果
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && aiResult.value) {
    clearAiResult();
  }
}

onMounted(() => {
  // 监听全局键盘事件
  document.addEventListener('keydown', handleKeydown);
  // 设置页面标题
  document.title = '创建岗位 - 招聘管理系统';
});

// 组件卸载时移除事件监听
import { onUnmounted } from 'vue';
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div class="job-create-page" role="main" aria-labelledby="page-title">
    <!-- 主视觉标语 -->
    <section class="hero-section" aria-label="页面操作栏">
      <div class="hero-left">
        <el-button
            :icon="ArrowLeft"
            text
            class="back-btn"
            @click="goBack"
            aria-label="返回上一页"
        >
          返回
        </el-button>
        <h1 id="page-title" class="hero-title">📝 创建岗位</h1>
      </div>
      <div class="hero-right">
        <el-button
            ref="submitBtnRef"
            type="primary"
            :icon="DocumentAdd"
            :loading="submitting"
            :aria-disabled="submitting"
            @click="handleSubmit"
        >
          保存
        </el-button>
      </div>
    </section>

    <!-- 主体内容 -->
    <div class="page-body">
      <div class="create-form-wrapper">
        <!-- AI搜索区域 -->
        <section class="ai-search-section" aria-label="AI智能搜索补全JD">
          <div class="section-label">
            <span class="label-text">🤖 AI智能搜索</span>
            <el-tag size="small" type="warning" effect="plain">联网补全JD</el-tag>
          </div>

          <div class="ai-search-container">
            <label for="ai-search-input" class="visually-hidden">输入岗位名称进行AI搜索</label>
            <el-input
                id="ai-search-input"
                ref="searchInputRef"
                v-model="searchKeyword"
                size="large"
                placeholder="输入岗位名称，AI将帮你搜索补全JD..."
                clearable
                @keydown.enter="handleAiSearch(searchKeyword)"
                @clear="clearAiResult"
                aria-describedby="ai-search-description"
            >
              <template #append>
                <el-button
                    type="primary"
                    :loading="isSearching"
                    :aria-busy="isSearching"
                    @click="handleAiSearch(searchKeyword)"
                    class="ai-search-btn"
                >
                  <el-icon aria-hidden="true"><MagicStick /></el-icon>
                  AI搜索补全
                </el-button>
              </template>
            </el-input>
            <div id="ai-search-description" class="visually-hidden">
              输入岗位名称后点击AI搜索补全按钮，系统将联网搜索并生成岗位描述
            </div>
          </div>

          <!-- AI搜索结果预览 -->
          <div
              v-if="aiResult && !isSearching"
              ref="aiResultRef"
              class="ai-result-preview"
              role="region"
              aria-live="polite"
              aria-labelledby="ai-result-title"
              tabindex="-1"
          >
            <div class="ai-result-header">
              <span id="ai-result-title" class="result-title">
                <el-icon aria-hidden="true"><Document /></el-icon>
                AI搜索补全结果
              </span>
              <div class="result-actions">
                <el-button
                    size="small"
                    type="primary"
                    @click="applyAiResult"
                    aria-label="应用AI生成的JD内容到表单"
                >
                  <el-icon aria-hidden="true"><Check /></el-icon>
                  应用此JD
                </el-button>
                <el-button
                    size="small"
                    text
                    @click="clearAiResult"
                    aria-label="忽略AI搜索结果"
                >
                  忽略
                </el-button>
              </div>
            </div>
            <div class="ai-result-body">
              <div class="result-meta">
                <span class="result-job-name">{{ aiResult.jobName || searchKeyword }}</span>
                <el-tag size="small" type="success" effect="plain">AI生成</el-tag>
              </div>
              <div v-if="aiResult.sources && aiResult.sources.length" class="result-sources">
                <span class="sources-label">信息来源：</span>
                <ul class="sources-list" aria-label="信息来源列表">
                  <li v-for="(source, index) in aiResult.sources" :key="index">
                    <el-tag
                        size="small"
                        type="info"
                        effect="plain"
                    >
                      {{ source.length > 50 ? source.substring(0, 50) + '...' : source }}
                    </el-tag>
                  </li>
                </ul>
              </div>
              <div class="result-content-preview" role="document" aria-label="AI生成的岗位描述预览">
                <JdContent :content="aiResult.jdContent" :job-name="aiResult.jobName || searchKeyword" />
              </div>
            </div>
          </div>

          <!-- 搜索中加载状态 -->
          <div
              v-if="isSearching"
              class="ai-loading"
              role="status"
              aria-live="polite"
          >
            <el-icon class="is-loading" aria-hidden="true"><Loading /></el-icon>
            <span>AI正在联网搜索「{{ searchKeyword || '岗位' }}」的JD信息...</span>
          </div>
        </section>

        <!-- 表单 -->
        <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            class="create-form"
            novalidate
        >
          <el-form-item
              label="岗位名称"
              prop="jobName"
              :aria-required="true"
          >
            <label for="job-name-input" class="visually-hidden">岗位名称（必填）</label>
            <el-input
                id="job-name-input"
                v-model="form.jobName"
                placeholder="请输入岗位名称，如：前端开发工程师"
                maxlength="100"
                show-word-limit
                clearable
                aria-describedby="job-name-tip"
            />
            <div id="job-name-tip" class="form-tip">💡 可以手动输入，也可以使用AI搜索自动填充</div>
          </el-form-item>

          <el-form-item
              label="岗位描述"
              prop="jdContent"
              :aria-required="true"
          >
            <label for="jd-content-input" class="visually-hidden">岗位描述（必填）</label>
            <el-input
                id="jd-content-input"
                v-model="form.jdContent"
                type="textarea"
                placeholder="请详细描述岗位职责、任职要求、工作内容等..."
                :rows="12"
                maxlength="10000"
                show-word-limit
                resize="vertical"
                class="jd-content-textarea"
                aria-describedby="jd-content-tip"
            />
            <div id="jd-content-tip" class="form-tip">
              <span>💡 可以使用AI搜索补全JD，也可以手动填写</span>
              <span v-if="useAiResult" class="ai-badge" role="status">✓ 已应用AI生成内容</span>
            </div>
          </el-form-item>

          <el-form-item v-if="form.jdContent" label="预览">
            <div
                class="jd-preview"
                role="region"
                aria-label="岗位描述预览"
            >
              <JdContent :content="form.jdContent" :job-name="form.jobName || '未命名岗位'" />
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
                type="primary"
                :loading="submitting"
                @click="handleSubmit"
                :aria-disabled="submitting"
            >
              <el-icon aria-hidden="true"><Plus /></el-icon>
              保存
            </el-button>
            <el-button @click="goBack" aria-label="取消创建并返回">取消</el-button>
          </el-form-item>
        </el-form>
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

.job-create-page {
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

.back-btn:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
}

.hero-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #4a7a64;
}

.hero-right .el-button {
  border-radius: 10px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(100, 163, 134, 0.25);
  background: #64A386;
  border-color: #64A386;
}

.hero-right .el-button:hover {
  box-shadow: 0 6px 20px rgba(100, 163, 134, 0.35);
  transform: translateY(-1px);
  background: #558f73;
  border-color: #558f73;
}

.hero-right .el-button:focus-visible {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

/* ========== 主体 ========== */
.page-body {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 28px 32px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.create-form-wrapper {
  max-width: 820px;
  margin: 0 auto;
}

/* ========== AI搜索区域 ========== */
.ai-search-section {
  margin-bottom: 32px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  border: 1px solid rgba(200, 216, 210, 0.3);
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.label-text {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.ai-search-container :deep(.el-input-group__append) {
  padding: 0;
}

.ai-search-container :deep(.el-input-group__append .el-button) {
  border-radius: 0 8px 8px 0;
  padding: 0 20px;
  font-weight: 500;
  background: #64A386;
  border-color: #64A386;
  color: #fff;
}

.ai-search-container :deep(.el-input-group__append .el-button:hover) {
  background: #558f73;
  border-color: #558f73;
}

.ai-search-container :deep(.el-input-group__append .el-button:focus-visible) {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

/* ========== AI加载状态 ========== */
.ai-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(100, 163, 134, 0.08);
  border-radius: 8px;
  color: #4a7a64;
  font-size: 14px;
}

.ai-loading .el-icon {
  font-size: 20px;
  color: #64A386;
}

/* ========== AI结果预览 ========== */
.ai-result-preview {
  margin-top: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 10px;
  border: 1px solid rgba(200, 216, 210, 0.3);
}

.ai-result-preview:focus-visible {
  outline: 2px solid #64A386;
  outline-offset: 2px;
}

.ai-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(200, 216, 210, 0.3);
}

.result-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.result-title .el-icon {
  color: #64A386;
}

.result-actions {
  display: flex;
  gap: 8px;
}

.result-actions .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.result-actions .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

.result-actions .el-button:focus-visible {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

.ai-result-body {
  font-size: 14px;
  color: #555;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.result-job-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.result-sources {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.sources-label {
  font-size: 12px;
  color: #8aab9a;
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.sources-list li {
  display: inline;
}

.result-content-preview {
  max-height: 250px;
  overflow-y: auto;
  padding: 12px;
  background: rgba(250, 252, 250, 0.4);
  border-radius: 6px;
  border: 1px solid rgba(200, 216, 210, 0.2);
}

.result-content-preview::-webkit-scrollbar {
  width: 4px;
}

.result-content-preview::-webkit-scrollbar-thumb {
  background: #d0d7de;
  border-radius: 2px;
}

/* ========== 表单 ========== */
.create-form {
  margin-top: 8px;
}

.create-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.create-form :deep(.el-form-item.is-error .el-input__wrapper) {
  border-color: #f56c6c;
}

.create-form :deep(.el-form-item.is-error .el-textarea__inner) {
  border-color: #f56c6c;
}

.create-form :deep(.el-textarea__inner) {
  min-height: 200px;
  font-size: 14px;
  line-height: 1.8;
  border-color: rgba(200, 216, 210, 0.3);
}

.create-form :deep(.el-textarea__inner:focus) {
  border-color: #64A386;
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.2);
}

.create-form :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.2);
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  font-size: 13px;
  color: #8aab9a;
  flex-wrap: wrap;
}

.ai-badge {
  color: #67c23a;
  font-weight: 500;
}

.jd-preview {
  padding: 16px 20px;
  background: rgba(250, 252, 250, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(200, 216, 210, 0.3);
  max-height: 400px;
  overflow-y: auto;
}

.create-form .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.create-form .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

.create-form .el-button:focus-visible {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

/* 响应式 */
@media (max-width: 768px) {
  .job-create-page {
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

  .hero-right .el-button {
    width: 100%;
    justify-content: center;
  }

  .ai-search-container :deep(.el-input-group) {
    flex-direction: column;
  }

  .ai-search-container :deep(.el-input-group__append) {
    margin-top: 8px;
  }

  .ai-search-container :deep(.el-input-group__append .el-button) {
    border-radius: 8px;
    width: 100%;
    justify-content: center;
  }

  .ai-result-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .result-actions {
    justify-content: stretch;
  }

  .result-actions .el-button {
    flex: 1;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .job-create-page {
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

  .ai-search-section {
    background: #f9f9f9;
    border-color: #000;
  }
}
</style>