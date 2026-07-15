<!-- src/views/job/JobEdit.vue -->
<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, onUnmounted, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  MagicStick,
  View,
  InfoFilled,
  Search,
  Check,
  ArrowLeft,
} from '@element-plus/icons-vue';
import { useJob } from '@/composables/useJob';
import JdContent from '@/components/JdContent.vue';
import type { JobUpdateRequest, JobSearchResult } from '@/types/job';

const router = useRouter();
const route = useRoute();

const {
  currentJob,
  isLoading,
  isSearching,
  fetchJobDetail,
  handleUpdateJob,
  handleSearchJob,
  clearSearchResult,
} = useJob();

const formRef = ref<FormInstance | null>(null);

const formData = reactive({
  jobName: '',
  jdContent: '',
});

const jobDetail = computed(() => {
  if (!currentJob?.value) return null;
  return currentJob?.value;
});

const formRules: FormRules = {
  jobName: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { min: 1, max: 100, message: '岗位名称长度在 1 到 100 个字符', trigger: 'blur' },
  ],
};

const submitting = ref(false);

const sourceMap: Record<number, { label: string; type: 'info' | 'primary' | 'warning' | 'success' | 'danger' }> = {
  0: { label: '手动创建', type: 'info' },
  1: { label: 'AI搜索', type: 'primary' },
  2: { label: 'Fork', type: 'warning' },
};

const sourceInfo = computed(() => {
  if (!jobDetail.value) return { label: '未知', type: 'info' as const };
  return sourceMap[jobDetail.value.source] || { label: '未知', type: 'info' as const };
});

function formatDate(dateStr?: string): string {
  if (!dateStr) return '--';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function getJobId(): number {
  const id = Number(route.params.id);
  if (isNaN(id) || id <= 0) return 0;
  return id;
}

async function loadJobData() {
  const id = getJobId();
  if (!id) {
    ElMessage.error('无效的岗位 ID');
    router.push('/jobs');
    return;
  }

  try {
    const result = await fetchJobDetail(id);
    if (result) {
      formData.jobName = result.jobName || '';
      formData.jdContent = result.jdContent || '';
      // 加载完成后聚焦到标题
      await nextTick();
      pageTitleRef.value?.focus();
    } else {
      ElMessage.error('岗位不存在或已被删除');
      router.push('/jobs');
    }
  } catch (error) {
    console.error('加载岗位数据失败:', error);
  }
}

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

  const id = getJobId();
  if (!id) {
    ElMessage.error('无效的岗位 ID');
    return;
  }

  const updateData: JobUpdateRequest = {
    jobName: formData.jobName.trim(),
    jdContent: formData.jdContent.trim() || undefined,
  };

  submitting.value = true;
  try {
    const result = await handleUpdateJob(id, updateData);
    if (result) {
      ElMessage.success('岗位更新成功！');
      router.push(`/jobs/${id}`);
    }
  } catch (error) {
    console.error('更新失败:', error);
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  const id = getJobId();
  if (id) {
    router.push(`/jobs/${id}`);
  } else {
    router.push('/jobs');
  }
}

function goBack() {
  router.back();
}

// 用于焦点管理
const pageTitleRef = ref<HTMLElement>();
const aiKeywordInputRef = ref<HTMLInputElement>();
const aiResultRef = ref<HTMLElement>();

// AI 搜索对话框
const aiDialogVisible = ref(false);
const aiKeyword = ref('');
const aiSearching = ref(false);
const aiSearchResult = ref<JobSearchResult | null>(null);

function handleAISearch() {
  aiKeyword.value = formData.jobName || '';
  aiSearchResult.value = null;
  aiDialogVisible.value = true;
  // 对话框打开后聚焦到输入框
  nextTick(() => {
    aiKeywordInputRef.value?.focus();
  });
}

async function handleAISearchConfirm() {
  const keyword = aiKeyword.value.trim();
  if (!keyword) {
    ElMessage.warning('请输入岗位名称');
    aiKeywordInputRef.value?.focus();
    return;
  }

  aiSearching.value = true;
  aiSearchResult.value = null;
  try {
    const result = await handleSearchJob({
      jobName: keyword,
      existingJd: formData.jdContent.trim() || undefined,
    });
    if (result) {
      aiSearchResult.value = result;
      ElMessage.success('AI 搜索完成，请查看结果');
      // 聚焦到搜索结果
      await nextTick();
      aiResultRef.value?.focus();
    } else {
      ElMessage.error('AI 搜索失败，请稍后重试');
    }
  } catch (error) {
    console.error('AI 搜索失败:', error);
  } finally {
    aiSearching.value = false;
  }
}

function handleApplyAIResult() {
  if (!aiSearchResult.value) return;
  formData.jobName = aiSearchResult.value.jobName;
  formData.jdContent = aiSearchResult.value.jdContent;
  aiSearchResult.value = null;
  aiDialogVisible.value = false;
  ElMessage.success('已应用 AI 生成的 JD 内容');
  // 聚焦到JD内容文本域
  nextTick(() => {
    const textarea = document.querySelector('.jd-textarea textarea') as HTMLTextAreaElement;
    if (textarea) {
      textarea.focus();
      textarea.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  });
}

// 预览
const previewDialogVisible = ref(false);

function handlePreview() {
  previewDialogVisible.value = true;
  // 对话框打开后聚焦到关闭按钮
  nextTick(() => {
    const closeBtn = document.querySelector('.preview-dialog .el-dialog__close') as HTMLElement;
    if (closeBtn) {
      setTimeout(() => closeBtn.focus(), 100);
    }
  });
}

function goToOriginalJob() {
  const originalId = jobDetail.value?.originalJobId;
  if (!originalId) return;
  router.push(`/jobs/${originalId}`);
}

// 键盘事件：ESC返回
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && !aiDialogVisible.value && !previewDialogVisible.value) {
    goBack();
  }
}

watch(
    () => route.params.id,
    (newId) => {
      if (newId) {
        const id = Number(newId);
        if (id > 0) loadJobData();
      }
    }
);

onMounted(() => {
  loadJobData();
  document.addEventListener('keydown', handleKeydown);
  document.title = '编辑岗位 - 招聘管理系统';
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div class="job-edit-page" role="main" aria-labelledby="page-title">
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
        <h1 id="page-title" ref="pageTitleRef" class="hero-title" tabindex="-1">✏️ 编辑岗位</h1>
        <el-tag
            v-if="jobDetail"
            :type="sourceInfo.type"
            size="small"
            effect="plain"
            :aria-label="`来源：${sourceInfo.label}`"
        >
          {{ sourceInfo.label }}
        </el-tag>
      </div>
      <div class="hero-right">
        <el-button
            type="primary"
            :loading="submitting"
            :aria-disabled="submitting"
            @click="handleSubmit"
        >
          <el-icon aria-hidden="true"><Check /></el-icon>
          保存更新
        </el-button>
      </div>
    </section>

    <!-- 表单区域 -->
    <section class="form-section" aria-label="编辑岗位表单">
      <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
          label-position="right"
          class="edit-form"
          novalidate
      >
        <!-- 岗位名称 -->
        <el-form-item label="岗位名称" prop="jobName" required :aria-required="true">
          <label for="job-name-input" class="visually-hidden">岗位名称（必填）</label>
          <el-input
              id="job-name-input"
              v-model="formData.jobName"
              placeholder="请输入岗位名称，如：Java后端开发工程师"
              size="large"
              maxlength="100"
              show-word-limit
              aria-describedby="job-name-tip"
          />
          <div id="job-name-tip" class="form-tip">请输入2-100个字符的岗位名称</div>
        </el-form-item>

        <!-- JD 内容 -->
        <el-form-item label="JD 内容" prop="jdContent">
          <div class="jd-editor-wrapper">
            <div class="editor-toolbar">
              <span class="toolbar-title" id="jd-editor-title">📋 岗位描述</span>
              <div class="toolbar-actions" role="group" aria-label="编辑器操作">
                <el-button size="small" text @click="handleAISearch" aria-label="使用AI重新生成JD内容">
                  <el-icon aria-hidden="true"><MagicStick /></el-icon>
                  AI 重新生成
                </el-button>
                <el-button size="small" text @click="handlePreview" aria-label="预览JD内容">
                  <el-icon aria-hidden="true"><View /></el-icon>
                  预览
                </el-button>
              </div>
            </div>
            <label for="jd-content-input" class="visually-hidden">岗位描述内容</label>
            <el-input
                id="jd-content-input"
                v-model="formData.jdContent"
                type="textarea"
                :rows="12"
                placeholder="请输入岗位 JD 内容，包含公司名称、薪资范围、工作地点、岗位职责、任职要求等"
                maxlength="10000"
                show-word-limit
                class="jd-textarea"
                aria-describedby="jd-editor-tip"
            />
            <div id="jd-editor-tip" class="editor-tip">
              <el-icon aria-hidden="true"><InfoFilled /></el-icon>
              <span>建议结构：公司名称 | 薪资范围 | 工作地点 | 岗位职责 | 任职要求</span>
            </div>
          </div>
        </el-form-item>

        <!-- 岗位信息 -->
        <el-divider content-position="left">
          <span class="divider-title">📊 岗位信息</span>
        </el-divider>

        <dl class="info-grid" aria-label="岗位元数据">
          <div class="info-item">
            <dt class="info-label">来源</dt>
            <dd>
              <el-tag :type="sourceInfo.type" size="default">
                {{ sourceInfo.label }}
              </el-tag>
            </dd>
          </div>
          <div class="info-item">
            <dt class="info-label">创建时间</dt>
            <dd class="info-value">{{ formatDate(jobDetail?.createdAt) }}</dd>
          </div>
          <div class="info-item">
            <dt class="info-label">更新时间</dt>
            <dd class="info-value">{{ formatDate(jobDetail?.updatedAt) }}</dd>
          </div>
          <div v-if="jobDetail?.originalJobId" class="info-item">
            <dt class="info-label">原始岗位</dt>
            <dd>
              <el-link type="primary" @click="goToOriginalJob" aria-label="查看原始岗位 #{{ jobDetail.originalJobId }}">
                #{{ jobDetail.originalJobId }}
              </el-link>
            </dd>
          </div>
        </dl>

        <!-- 操作按钮 -->
        <el-form-item class="form-actions">
          <el-button size="large" @click="handleCancel" aria-label="取消编辑并返回">取消</el-button>
          <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :aria-disabled="submitting"
              @click="handleSubmit"
          >
            <el-icon aria-hidden="true"><Check /></el-icon>
            保存更新
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- AI 搜索对话框 -->
    <el-dialog
        v-model="aiDialogVisible"
        title="🤖 AI 重新生成 JD"
        width="700px"
        :close-on-click-modal="false"
        destroy-on-close
        :aria-label="'AI 重新生成 JD 对话框'"
        class="ai-dialog"
    >
      <div class="ai-dialog-content">
        <p id="ai-dialog-desc" class="ai-dialog-desc">
          输入岗位名称，AI 将联网搜索最新的 JD 信息，帮助你完善或更新岗位描述。
          <span class="ai-limit-tip">（每分钟限 5 次）</span>
        </p>
        <label for="ai-keyword-input" class="visually-hidden">输入岗位名称进行AI搜索</label>
        <el-input
            id="ai-keyword-input"
            ref="aiKeywordInputRef"
            v-model="aiKeyword"
            placeholder="请输入岗位名称，如：Java后端开发工程师"
            size="large"
            clearable
            aria-describedby="ai-dialog-desc"
            @keydown.enter="handleAISearchConfirm"
        >
          <template #prefix>
            <el-icon aria-hidden="true"><Search /></el-icon>
          </template>
          <template #append>
            <el-button
                type="primary"
                :loading="aiSearching"
                :aria-busy="aiSearching"
                @click="handleAISearchConfirm"
            >
              <el-icon aria-hidden="true"><MagicStick /></el-icon>
              AI 搜索
            </el-button>
          </template>
        </el-input>

        <div
            v-if="aiSearchResult"
            ref="aiResultRef"
            class="ai-result-preview"
            role="region"
            aria-live="polite"
            aria-labelledby="ai-result-title"
            tabindex="-1"
        >
          <el-divider content-position="left">
            <span id="ai-result-title" class="result-title">📎 搜索结果</span>
          </el-divider>
          <div class="result-content" role="document" aria-label="AI生成的JD内容">
            <JdContent
                :content="aiSearchResult.jdContent"
                :job-name="aiSearchResult.jobName"
                show-header
            />
          </div>
          <div v-if="aiSearchResult.sources && aiSearchResult.sources.length" class="result-sources">
            <span class="sources-label">🔗 来源：</span>
            <ul class="sources-list" aria-label="信息来源列表">
              <li v-for="(source, index) in aiSearchResult.sources" :key="index">
                <el-link
                    :href="source"
                    target="_blank"
                    type="primary"
                    :underline="false"
                    class="source-link"
                    aria-label="信息来源链接 {{ index + 1 }}"
                >
                  {{ source.length > 40 ? source.substring(0, 40) + '...' : source }}
                </el-link>
              </li>
            </ul>
          </div>
          <div class="result-actions">
            <el-button @click="aiSearchResult = null" aria-label="取消应用AI结果">取消</el-button>
            <el-button type="primary" @click="handleApplyAIResult" aria-label="应用AI生成的JD到表单">
              <el-icon aria-hidden="true"><Check /></el-icon>
              应用此 JD
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="aiDialogVisible = false" aria-label="关闭AI搜索对话框">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
        v-model="previewDialogVisible"
        title="👁️ JD 预览"
        width="700px"
        destroy-on-close
        :aria-label="'JD 预览对话框'"
        class="preview-dialog"
    >
      <div class="preview-content" role="document" aria-label="JD预览内容">
        <JdContent
            :content="formData.jdContent || '暂无 JD 内容'"
            :job-name="formData.jobName || '未命名岗位'"
            show-header
        />
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false" aria-label="关闭预览">关闭</el-button>
      </template>
    </el-dialog>
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

.job-edit-page {
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
  font-size: 22px;
  font-weight: 600;
  color: #4a7a64;
}

.hero-title:focus-visible {
  outline: 2px solid #4a7a64;
  outline-offset: 2px;
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

/* ========== 表单区域 ========== */
.form-section {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 28px 32px;
}

.edit-form {
  max-width: 860px;
  margin: 0 auto;
}

.edit-form :deep(.el-form-item) {
  margin-bottom: 28px;
}

.edit-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #4a7a64;
}

.edit-form :deep(.el-form-item.is-error .el-input__wrapper) {
  border-color: #f56c6c;
}

.edit-form :deep(.el-form-item.is-error .el-textarea__inner) {
  border-color: #f56c6c;
}

.edit-form :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.2);
}

.form-tip {
  font-size: 12px;
  color: #8aab9a;
  margin-top: 4px;
}

/* ========== JD 编辑器 ========== */
.jd-editor-wrapper {
  width: 100%;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(100, 163, 134, 0.06);
  border-radius: 8px 8px 0 0;
  border: 1px solid rgba(200, 216, 210, 0.3);
  border-bottom: none;
}

.toolbar-title {
  font-size: 14px;
  font-weight: 500;
  color: #4a7a64;
}

.toolbar-actions {
  display: flex;
  gap: 4px;
}

.toolbar-actions .el-button {
  color: #64A386;
}

.toolbar-actions .el-button:hover {
  color: #4a7a64;
  background: rgba(100, 163, 134, 0.1);
}

.toolbar-actions .el-button:focus-visible {
  outline: 2px solid #64A386;
  outline-offset: 2px;
}

.jd-textarea :deep(.el-textarea__inner) {
  border-radius: 0 0 8px 8px;
  border-color: rgba(200, 216, 210, 0.3);
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.8;
  min-height: 280px;
}

.jd-textarea :deep(.el-textarea__inner:focus) {
  border-color: #64A386;
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.2);
}

.editor-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #8aab9a;
}

.editor-tip .el-icon {
  font-size: 16px;
}

/* ========== 信息网格 ========== */
.divider-title {
  font-size: 15px;
  font-weight: 500;
  color: #4a7a64;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px 24px;
  padding: 8px 0 16px;
  margin: 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item dt {
  font-size: 14px;
  color: #8aab9a;
  min-width: 56px;
  margin: 0;
}

.info-item dd {
  margin: 0;
  font-size: 14px;
  color: #333;
}

/* ========== 操作按钮 ========== */
.form-actions {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid rgba(200, 216, 210, 0.3);
}

.form-actions :deep(.el-form-item__content) {
  justify-content: center;
  gap: 16px;
}

.form-actions .el-button {
  min-width: 120px;
  border-radius: 10px;
  font-weight: 500;
}

.form-actions .el-button:focus-visible {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

.form-actions .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.form-actions .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

/* ========== AI 对话框 ========== */
.ai-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.ai-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(200, 216, 210, 0.3);
  padding: 20px 24px 16px;
}

.ai-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #4a7a64;
}

.ai-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.ai-dialog-content {
  padding: 4px 0;
}

.ai-dialog-desc {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;
}

.ai-limit-tip {
  color: #f56c6c;
  font-size: 12px;
}

.ai-dialog-content :deep(.el-input-group__append) {
  padding: 0;
}

.ai-dialog-content :deep(.el-input-group__append .el-button) {
  border-radius: 0 8px 8px 0;
  padding: 0 20px;
  font-weight: 500;
  background: #64A386;
  border-color: #64A386;
}

.ai-dialog-content :deep(.el-input-group__append .el-button:hover) {
  background: #558f73;
  border-color: #558f73;
}

.ai-dialog-content :deep(.el-input-group__append .el-button:focus-visible) {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

.ai-result-preview {
  margin-top: 20px;
}

.ai-result-preview:focus-visible {
  outline: 2px solid #64A386;
  outline-offset: 2px;
}

.result-title {
  font-size: 14px;
  font-weight: 500;
  color: #4a7a64;
}

.result-content {
  padding: 16px;
  background: rgba(250, 252, 250, 0.6);
  border-radius: 10px;
  border: 1px solid rgba(200, 216, 210, 0.3);
  max-height: 400px;
  overflow-y: auto;
}

.result-sources {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(100, 163, 134, 0.06);
  border-radius: 8px;
}

.sources-label {
  font-size: 13px;
  color: #8aab9a;
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.sources-list li {
  display: inline;
}

.source-link {
  font-size: 12px;
  color: #64A386;
  word-break: break-all;
}

.source-link:hover {
  color: #4a7a64;
}

.source-link:focus-visible {
  outline: 2px solid #64A386;
  outline-offset: 2px;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.result-actions .el-button:focus-visible {
  outline: 2px solid #2d5a4a;
  outline-offset: 2px;
}

.result-actions .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.result-actions .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

/* ========== 预览对话框 ========== */
.preview-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.preview-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(200, 216, 210, 0.3);
  padding: 20px 24px 16px;
}

.preview-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #4a7a64;
}

.preview-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.preview-content {
  padding: 4px 0;
  max-height: 500px;
  overflow-y: auto;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .job-edit-page {
    padding: 16px;
  }

  .hero-section {
    padding: 16px 20px;
  }

  .hero-title {
    font-size: 18px;
  }

  .form-section {
    padding: 20px 16px;
  }

  .edit-form {
    max-width: 100%;
  }

  .info-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .editor-toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }

  .form-actions :deep(.el-form-item__content) {
    flex-direction: column;
    gap: 12px;
  }

  .form-actions .el-button {
    width: 100%;
  }

  .ai-result-preview .result-content {
    max-height: 300px;
  }

  .ai-dialog-content :deep(.el-input-group) {
    flex-direction: column;
  }

  .ai-dialog-content :deep(.el-input-group__append) {
    margin-top: 8px;
  }

  .ai-dialog-content :deep(.el-input-group__append .el-button) {
    border-radius: 8px;
    width: 100%;
  }

  .result-sources {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-right .el-button {
    width: 100%;
    justify-content: center;
  }

  .ai-dialog :deep(.el-dialog) {
    width: 95% !important;
  }

  .preview-dialog :deep(.el-dialog) {
    width: 95% !important;
  }
}

@media (max-width: 480px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .job-edit-page {
    background: #ffffff;
  }

  .hero-section {
    background: #f5f5f5;
    border-color: #000;
  }

  .form-section {
    background: #ffffff;
    border-color: #000;
  }

  .editor-toolbar {
    border-color: #000;
    background: #f0f0f0;
  }

  .jd-textarea :deep(.el-textarea__inner) {
    border-color: #000;
  }

  .info-grid {
    border-top-color: #000;
  }

  .form-actions {
    border-top-color: #000;
  }
}
</style>