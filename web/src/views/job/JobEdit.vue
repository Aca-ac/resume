<!-- src/views/job/JobEdit.vue -->
<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
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

// AI 搜索对话框
const aiDialogVisible = ref(false);
const aiKeyword = ref('');
const aiSearching = ref(false);
const aiSearchResult = ref<JobSearchResult | null>(null);

function handleAISearch() {
  aiKeyword.value = formData.jobName || '';
  aiSearchResult.value = null;
  aiDialogVisible.value = true;
}

async function handleAISearchConfirm() {
  const keyword = aiKeyword.value.trim();
  if (!keyword) {
    ElMessage.warning('请输入岗位名称');
    return;
  }

  aiSearching.value = true;
  try {
    const result = await handleSearchJob({
      jobName: keyword,
      existingJd: formData.jdContent.trim() || undefined,
    });
    if (result) {
      aiSearchResult.value = result;
      ElMessage.success('AI 搜索完成，请查看结果');
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
}

// 预览
const previewDialogVisible = ref(false);

function handlePreview() {
  previewDialogVisible.value = true;
}

function goToOriginalJob() {
  const originalId = jobDetail.value?.originalJobId;
  if (!originalId) return;
  router.push(`/jobs/${originalId}`);
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
});
</script>

<template>
  <div class="job-edit-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <div class="hero-left">
        <el-button :icon="ArrowLeft" text class="back-btn" @click="goBack">返回</el-button>
        <h1 class="hero-title">✏️ 编辑岗位</h1>
        <el-tag
            v-if="jobDetail"
            :type="sourceInfo.type"
            size="small"
            effect="plain"
        >
          {{ sourceInfo.label }}
        </el-tag>
      </div>
      <div class="hero-right">
        <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
        >
          <el-icon><Check /></el-icon>
          保存更新
        </el-button>
      </div>
    </section>

    <!-- 表单区域 -->
    <section class="form-section">
      <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
          label-position="right"
          class="edit-form"
      >
        <!-- 岗位名称 -->
        <el-form-item label="岗位名称" prop="jobName" required>
          <el-input
              v-model="formData.jobName"
              placeholder="请输入岗位名称，如：Java后端开发工程师"
              size="large"
              maxlength="100"
              show-word-limit
          />
        </el-form-item>

        <!-- JD 内容 -->
        <el-form-item label="JD 内容" prop="jdContent">
          <div class="jd-editor-wrapper">
            <div class="editor-toolbar">
              <span class="toolbar-title">📋 岗位描述</span>
              <div class="toolbar-actions">
                <el-button size="small" text @click="handleAISearch">
                  <el-icon><MagicStick /></el-icon>
                  AI 重新生成
                </el-button>
                <el-button size="small" text @click="handlePreview">
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
              </div>
            </div>
            <el-input
                v-model="formData.jdContent"
                type="textarea"
                :rows="12"
                placeholder="请输入岗位 JD 内容，包含公司名称、薪资范围、工作地点、岗位职责、任职要求等"
                maxlength="10000"
                show-word-limit
                class="jd-textarea"
            />
            <div class="editor-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>建议结构：公司名称 | 薪资范围 | 工作地点 | 岗位职责 | 任职要求</span>
            </div>
          </div>
        </el-form-item>

        <!-- 岗位信息 -->
        <el-divider content-position="left">
          <span class="divider-title">📊 岗位信息</span>
        </el-divider>

        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">来源</span>
            <el-tag :type="sourceInfo.type" size="default">
              {{ sourceInfo.label }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(jobDetail?.createdAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">更新时间</span>
            <span class="info-value">{{ formatDate(jobDetail?.updatedAt) }}</span>
          </div>
          <div v-if="jobDetail?.originalJobId" class="info-item">
            <span class="info-label">原始岗位</span>
            <el-link type="primary" @click="goToOriginalJob">
              #{{ jobDetail.originalJobId }}
            </el-link>
          </div>
        </div>

        <!-- 操作按钮 -->
        <el-form-item class="form-actions">
          <el-button size="large" @click="handleCancel">取消</el-button>
          <el-button
              type="primary"
              size="large"
              :loading="submitting"
              @click="handleSubmit"
          >
            <el-icon><Check /></el-icon>
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
    >
      <div class="ai-dialog-content">
        <p class="ai-dialog-desc">
          输入岗位名称，AI 将联网搜索最新的 JD 信息，帮助你完善或更新岗位描述。
          <span class="ai-limit-tip">（每分钟限 5 次）</span>
        </p>
        <el-input
            v-model="aiKeyword"
            placeholder="请输入岗位名称，如：Java后端开发工程师"
            size="large"
            clearable
            @keydown.enter="handleAISearchConfirm"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button
                type="primary"
                :loading="aiSearching"
                @click="handleAISearchConfirm"
            >
              <el-icon><MagicStick /></el-icon>
              AI 搜索
            </el-button>
          </template>
        </el-input>

        <div v-if="aiSearchResult" class="ai-result-preview">
          <el-divider content-position="left">
            <span class="result-title">📎 搜索结果</span>
          </el-divider>
          <div class="result-content">
            <JdContent
                :content="aiSearchResult.jdContent"
                :job-name="aiSearchResult.jobName"
                show-header
            />
          </div>
          <div v-if="aiSearchResult.sources && aiSearchResult.sources.length" class="result-sources">
            <span class="sources-label">🔗 来源：</span>
            <el-link
                v-for="(source, index) in aiSearchResult.sources"
                :key="index"
                :href="source"
                target="_blank"
                type="primary"
                :underline="false"
                class="source-link"
            >
              {{ source.length > 40 ? source.substring(0, 40) + '...' : source }}
            </el-link>
          </div>
          <div class="result-actions">
            <el-button @click="aiSearchResult = null">取消</el-button>
            <el-button type="primary" @click="handleApplyAIResult">
              <el-icon><Check /></el-icon>
              应用此 JD
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
        v-model="previewDialogVisible"
        title="👁️ JD 预览"
        width="700px"
        destroy-on-close
    >
      <div class="preview-content">
        <JdContent
            :content="formData.jdContent || '暂无 JD 内容'"
            :job-name="formData.jobName || '未命名岗位'"
            show-header
        />
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
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
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-size: 14px;
  color: #8aab9a;
  min-width: 56px;
}

.info-value {
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

.form-actions .el-button--primary {
  background: #64A386;
  border-color: #64A386;
}

.form-actions .el-button--primary:hover {
  background: #558f73;
  border-color: #558f73;
}

/* ========== AI 对话框 ========== */
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

.ai-result-preview {
  margin-top: 20px;
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

.source-link {
  font-size: 12px;
  color: #64A386;
  word-break: break-all;
}

.source-link:hover {
  color: #4a7a64;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
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
}

@media (max-width: 480px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>