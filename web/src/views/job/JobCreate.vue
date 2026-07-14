<!-- src/views/job/JobCreate.vue -->
<script setup lang="ts">
import { ref, computed, watch } from 'vue';
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
    return;
  }

  clearSearchResult();

  const result = await handleSearchJob({
    jobName: keyword.trim(),
    existingJd: form.value.jdContent || undefined
  });

  if (result) {
    ElMessage.success('AI搜索完成，请查看结果并点击"应用此JD"');
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
}

// 清除AI结果
function clearAiResult() {
  clearSearchResult();
  useAiResult.value = false;
}

// 提交表单 - 改为保存
async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
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
</script>

<template>
  <div class="job-create-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <div class="hero-left">
        <el-button :icon="ArrowLeft" text class="back-btn" @click="goBack">返回</el-button>
        <h1 class="hero-title">📝 创建岗位</h1>
      </div>
      <div class="hero-right">
        <el-button
            type="primary"
            :icon="DocumentAdd"
            :loading="submitting"
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
        <div class="ai-search-section">
          <div class="section-label">
            <span class="label-text">🤖 AI智能搜索</span>
            <el-tag size="small" type="warning" effect="plain">联网补全JD</el-tag>
          </div>

          <div class="ai-search-container">
            <el-input
                v-model="searchKeyword"
                size="large"
                placeholder="输入岗位名称，AI将帮你搜索补全JD..."
                clearable
                @keydown.enter="handleAiSearch(searchKeyword)"
                @clear="clearAiResult"
            >
              <template #append>
                <el-button
                    type="primary"
                    :loading="isSearching"
                    @click="handleAiSearch(searchKeyword)"
                    class="ai-search-btn"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI搜索补全
                </el-button>
              </template>
            </el-input>
          </div>

          <!-- AI搜索结果预览 -->
          <div v-if="aiResult && !isSearching" class="ai-result-preview">
            <div class="ai-result-header">
              <span class="result-title">
                <el-icon><Document /></el-icon>
                AI搜索补全结果
              </span>
              <div class="result-actions">
                <el-button size="small" type="primary" @click="applyAiResult">
                  <el-icon><Check /></el-icon>
                  应用此JD
                </el-button>
                <el-button size="small" text @click="clearAiResult">
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
                <el-tag
                    v-for="(source, index) in aiResult.sources"
                    :key="index"
                    size="small"
                    type="info"
                    effect="plain"
                >
                  {{ source.length > 50 ? source.substring(0, 50) + '...' : source }}
                </el-tag>
              </div>
              <div class="result-content-preview">
                <JdContent :content="aiResult.jdContent" :job-name="aiResult.jobName || searchKeyword" />
              </div>
            </div>
          </div>

          <!-- 搜索中加载状态 -->
          <div v-if="isSearching" class="ai-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>AI正在联网搜索「{{ searchKeyword || '岗位' }}」的JD信息...</span>
          </div>
        </div>

        <!-- 表单 -->
        <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            class="create-form"
        >
          <el-form-item label="岗位名称" prop="jobName">
            <el-input
                v-model="form.jobName"
                placeholder="请输入岗位名称，如：前端开发工程师"
                maxlength="100"
                show-word-limit
                clearable
            />
            <div class="form-tip">💡 可以手动输入，也可以使用AI搜索自动填充</div>
          </el-form-item>

          <el-form-item label="岗位描述" prop="jdContent">
            <el-input
                v-model="form.jdContent"
                type="textarea"
                placeholder="请详细描述岗位职责、任职要求、工作内容等..."
                :rows="12"
                maxlength="10000"
                show-word-limit
                resize="vertical"
            />
            <div class="form-tip">
              <span>💡 可以使用AI搜索补全JD，也可以手动填写</span>
              <span v-if="useAiResult" class="ai-badge">✓ 已应用AI生成内容</span>
            </div>
          </el-form-item>

          <el-form-item v-if="form.jdContent" label="预览">
            <div class="jd-preview">
              <JdContent :content="form.jdContent" :job-name="form.jobName || '未命名岗位'" />
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              <el-icon><Plus /></el-icon>
              保存
            </el-button>
            <el-button @click="goBack">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
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

.create-form :deep(.el-textarea__inner) {
  min-height: 200px;
  font-size: 14px;
  line-height: 1.8;
  border-color: rgba(200, 216, 210, 0.3);
}

.create-form :deep(.el-textarea__inner:focus) {
  border-color: #64A386;
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
</style>