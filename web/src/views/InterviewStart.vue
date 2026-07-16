<!-- src/views/InterviewStart.vue -->
<template>
  <div class="start-page">
    <!-- 主视觉标语 -->
    <header class="hero-section" aria-labelledby="hero-title">
      <h1 id="hero-title" class="hero-title">
        <span aria-hidden="true">🎯</span>
        模拟面试练习
      </h1>
      <p class="hero-subtitle">
        选择一份简历和目标职位，AI 面试官将根据你的经历进行针对性提问，
        帮助你提前适应真实面试场景，提升应答能力。
      </p>
      <el-button text type="primary" @click="router.push('/interview/history')">查看面试历史 →</el-button>
    </header>

    <!-- 面试设置卡片 -->
    <main class="content-section" aria-label="面试设置表单">
      <div class="form-card">
        <div class="card-header">
          <span class="card-title" id="form-title">面试设置</span>
          <span class="card-subtitle">填写信息，开始模拟面试</span>
        </div>

        <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="100px"
            label-position="top"
            novalidate
            @submit.prevent="onStart"
        >
          <!-- 简历选择 -->
          <el-form-item
              label="选择简历"
              prop="resumeId"
              :error="formErrors.resumeId"
          >
            <el-select
                v-model="formData.resumeId"
                style="width: 100%"
                placeholder="请选择一份简历"
                id="resume-select"
                name="resumeId"
                :aria-describedby="'resume-hint'"
                :aria-invalid="!!formErrors.resumeId"
                @change="clearFieldError('resumeId')"
            >
              <el-option
                  v-for="r in resumes"
                  :key="r.id"
                  :label="r.title"
                  :value="r.id"
              />
            </el-select>
            <div id="resume-hint" class="form-hint">选择已创建的简历，AI 将基于你的经历进行提问</div>
            <div v-if="formErrors.resumeId" class="field-error" role="alert">
              <span aria-hidden="true">⚠️</span>
              {{ formErrors.resumeId }}
            </div>
          </el-form-item>

          <!-- 目标职位 -->
          <el-form-item
              label="目标职位"
              prop="jobTitle"
              :error="formErrors.jobTitle"
          >
            <el-input
                v-model="formData.jobTitle"
                placeholder="例如：后端开发工程师"
                id="job-title-input"
                name="jobTitle"
                :aria-describedby="'job-title-hint'"
                :aria-invalid="!!formErrors.jobTitle"
                @input="clearFieldError('jobTitle')"
            />
            <div id="job-title-hint" class="form-hint">也可从下方已保存岗位中选择，自动带出 JD</div>
            <div v-if="formErrors.jobTitle" class="field-error" role="alert">
              <span aria-hidden="true">⚠️</span>
              {{ formErrors.jobTitle }}
            </div>
          </el-form-item>

          <el-form-item label="关联目标岗位（可选）">
            <el-select
                v-model="formData.jobId"
                clearable
                filterable
                style="width: 100%"
                placeholder="选择已保存的目标岗位（可选）"
                @change="onJobPicked"
            >
              <el-option
                  v-for="j in myJobs"
                  :key="j.id"
                  :label="j.jobName"
                  :value="j.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="问题数量">
            <el-input-number v-model="formData.maxQuestions" :min="3" :max="10" />
          </el-form-item>

          <!-- 提交按钮 -->
          <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="onStart"
              class="start-btn"
              :aria-label="loading ? '正在启动面试...' : '开始面试'"
              :disabled="loading"
          >
            <el-icon v-if="!loading" aria-hidden="true"><ChatDotRound /></el-icon>
            <span v-if="!loading">开始面试 →</span>
            <span v-else>正在启动面试...</span>
          </el-button>

          <!-- 快捷键提示 -->
          <div class="keyboard-hint" aria-hidden="true">
            按 <kbd>Enter</kbd> 快速开始
          </div>
        </el-form>

        <!-- 全局错误提示 -->
        <div
            v-if="globalError"
            class="global-error"
            role="alert"
            aria-live="assertive"
        >
          <span aria-hidden="true">❌</span>
          {{ globalError }}
          <el-button
              size="small"
              @click="clearGlobalError"
              aria-label="关闭错误提示"
          >
            关闭
          </el-button>
        </div>

        <!-- 加载状态提示（屏幕阅读器） -->
        <div v-if="loading" class="sr-only" role="status" aria-live="polite">
          正在启动面试，请稍候...
        </div>
      </div>
    </main>

    <!-- 底部留白 -->
    <div class="blank-area" aria-hidden="true"></div>

    <!-- 动态气泡背景 - 完全装饰性 -->
    <div class="bubbles" aria-hidden="true">
      <div
          class="bubble"
          v-for="i in 12"
          :key="i"
          :style="getBubbleStyle(i)"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ChatDotRound } from "@element-plus/icons-vue";
import { useInterviewStore } from "@/stores/interview";
import { useResumeStore } from "@/stores/resume";
import { jobApi } from "@/api/job";
import type { JobSimple } from "@/types/job";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

const router = useRouter();
const interview = useInterviewStore();
const resumeStore = useResumeStore();

// ===== 表单引用 =====
const formRef = ref<FormInstance>();

// ===== 表单数据 =====
const formData = reactive({
  resumeId: null as number | null,
  jobTitle: "后端开发工程师",
  jobId: null as number | null,
  maxQuestions: 5
});
const myJobs = ref<JobSimple[]>([]);

function onJobPicked(id: number | null) {
  if (!id) return;
  const job = myJobs.value.find((j) => j.id === id);
  if (job?.jobName) {
    formData.jobTitle = job.jobName;
    clearFieldError("jobTitle");
  }
}

// ===== 表单验证规则 =====
const formRules: FormRules = {
  resumeId: [
    {
      required: true,
      message: '请选择一份简历',
      trigger: 'change'
    },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('请选择一份简历'));
        } else {
          callback();
        }
      },
      trigger: 'change'
    }
  ],
  jobTitle: [
    {
      required: true,
      message: '请输入目标职位',
      trigger: 'blur'
    },
    {
      min: 2,
      max: 50,
      message: '职位名称长度应在2-50个字符之间',
      trigger: 'blur'
    },
    {
      pattern: /^[\u4e00-\u9fa5a-zA-Z0-9\s\-\(\)（）]+$/,
      message: '职位名称包含无效字符',
      trigger: 'blur'
    }
  ]
};

// ===== 状态管理 =====
const resumes = computed(() => resumeStore.list);
const loading = ref(false);
const globalError = ref("");
const formErrors = reactive({
  resumeId: "",
  jobTitle: ""
});

// ===== 生成气泡样式（仅在客户端执行） =====
const getBubbleStyle = (i: number) => {
  // 使用固定种子值确保服务端渲染一致性
  const seed = i * 7.3;
  const size = 20 + ((seed * 1.3) % 60);
  const left = ((seed * 2.7) % 100);
  const duration = 15 + ((seed * 1.1) % 25);
  const delay = ((seed * 0.7) % 20);
  const opacity = 0.08 + ((seed * 0.4) % 0.22);
  return {
    width: size + 'px',
    height: size + 'px',
    left: left + '%',
    animationDuration: duration + 's',
    animationDelay: delay + 's',
    opacity: opacity,
    background: `radial-gradient(circle at 30% 30%, rgba(100, 163, 134, ${0.15 + ((seed * 0.3) % 0.35)}), rgba(79, 172, 254, ${0.1 + ((seed * 0.2) % 0.25)}))`
  };
};

// ===== 清除字段错误 =====
const clearFieldError = (field: keyof typeof formErrors) => {
  formErrors[field] = "";
};

// ===== 清除全局错误 =====
const clearGlobalError = () => {
  globalError.value = "";
};

// ===== 加载简历列表 =====
onMounted(async () => {
  try {
    await resumeStore.loadList();
    if (resumeStore.list.length > 0) {
      formData.resumeId = resumeStore.list[0].id;
    }
  } catch (e: any) {
    ElMessage.error({
      message: e.message || "加载简历列表失败",
      duration: 5000,
    });
  }
  try {
    const page = await jobApi.getJobList({ page: 1, size: 50 });
    myJobs.value = page.records || [];
  } catch {
    myJobs.value = [];
  }
});

// ===== 提交表单 =====
async function onStart() {
  // 清除旧错误
  clearGlobalError();
  Object.keys(formErrors).forEach(key => {
    formErrors[key as keyof typeof formErrors] = "";
  });

  // 验证表单
  if (!formRef.value) return;

  try {
    await formRef.value.validate();
  } catch (e: any) {
    // 提取并设置字段错误
    if (e && typeof e === 'object') {
      const errors = e as Record<string, { message: string }[]>;
      for (const [field, fieldErrors] of Object.entries(errors)) {
        if (fieldErrors && fieldErrors.length > 0) {
          const errorKey = field as keyof typeof formErrors;
          if (errorKey in formErrors) {
            formErrors[errorKey] = fieldErrors[0].message;
          }
        }
      }
    }
    // 聚焦到第一个错误字段
    const firstError = document.querySelector('.el-form-item.is-error input, .el-form-item.is-error .el-select');
    if (firstError) {
      (firstError as HTMLElement).focus();
    }
    return;
  }

  // 再次检查简历ID
  if (!formData.resumeId) {
    formErrors.resumeId = "请选择一份简历";
    const resumeSelect = document.getElementById('resume-select');
    if (resumeSelect) resumeSelect.focus();
    return;
  }

  loading.value = true;
  try {
    const session = await interview.start(
      formData.resumeId,
      formData.jobTitle,
      formData.jobId || undefined,
      formData.maxQuestions
    );
    // 跳转到聊天页面
    await router.push(`/interview/${session.id}/chat`);
  } catch (e: any) {
    const msg = e.message || "启动面试失败，请稍后重试";
    globalError.value = msg;
    ElMessage.error({
      message: msg,
      duration: 5000,
    });
    // 聚焦到错误区域
    const errorElement = document.querySelector('.global-error');
    if (errorElement) {
      (errorElement as HTMLElement).focus({ preventScroll: true });
    }
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
/* ===== 屏幕阅读器专用类 ===== */
.sr-only {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  border: 0 !important;
  white-space: nowrap !important;
}

/* ===== 页面基础样式 ===== */
.start-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 40%, #FBFCCD 70%, #CDE2E8 100%);
  background-size: 400% 400%;
  animation: gradientMove 12s ease-in-out infinite;
  position: relative;
  overflow: hidden;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* ===== 背景气泡 - 完全装饰性 ===== */
.bubbles {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bubble {
  position: absolute;
  bottom: -80px;
  border-radius: 50%;
  animation: bubbleFloat linear infinite;
  filter: blur(2px);
  will-change: transform;
}

@keyframes bubbleFloat {
  0% {
    transform: translateY(0) scale(0.8) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-110vh) scale(1.2) rotate(720deg);
    opacity: 0;
  }
}

/* ===== 标题区域 ===== */
.hero-section {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 36px;
  padding: 28px 40px;
  background: #FBFCCD;
  border-radius: 16px;
  border: 2px solid #e8e9b8;
}

.hero-title {
  margin: 0 0 12px;
  font-size: 32px;
  color: #2c4d3d;
  line-height: 1.3;
}

.hero-title span[aria-hidden="true"] {
  margin-right: 4px;
}

.hero-subtitle {
  margin: 0;
  color: #3d5a4b;
  font-size: 16px;
  line-height: 1.7;
  max-width: 640px;
  margin-left: auto;
  margin-right: auto;
}

/* ===== 内容区域 ===== */
.content-section {
  position: relative;
  z-index: 1;
  background-color: #ffffff;
  padding: 32px 40px;
  border-radius: 16px;
  max-width: 600px;
  margin: 0 auto;
  box-shadow: 0 4px 24px rgba(100, 163, 134, 0.10);
  border: 1px solid rgba(100, 163, 134, 0.08);
}

/* ===== 表单卡片 ===== */
.form-card {
  width: 100%;
}

.card-header {
  margin-bottom: 28px;
  text-align: center;
}

.card-title {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #2c4d3d;
  margin-bottom: 6px;
}

.card-subtitle {
  display: block;
  font-size: 14px;
  color: #6b8a7a;
}

/* ===== 表单样式 ===== */
.el-form {
  width: 100%;
}

.el-form-item {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c4d3d;
  padding-bottom: 6px;
  font-size: 14px;
}

/* ===== 选择器样式 ===== */
.el-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  min-height: 44px;
  box-shadow: 0 0 0 1px #b8ccbf;
  background: #ffffff;
  transition: box-shadow 0.2s ease;
}

.el-select :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4c8a6e, 0 0 0 4px rgba(76, 138, 110, 0.12);
}

.el-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #4c8a6e;
}

/* ===== 输入框样式 ===== */
.el-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  min-height: 44px;
  box-shadow: 0 0 0 1px #b8ccbf;
  background: #ffffff;
  transition: box-shadow 0.2s ease;
}

.el-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4c8a6e, 0 0 0 4px rgba(76, 138, 110, 0.12);
}

.el-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #4c8a6e;
}

/* ===== 错误状态 ===== */
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.1);
}

:deep(.el-form-item.is-error .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.1);
}

:deep(.el-form-item.is-error .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.2);
}

:deep(.el-form-item.is-error .el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.2);
}

/* ===== 提示文字 ===== */
.form-hint {
  font-size: 13px;
  color: #6b8a7a;
  margin-top: 6px;
  padding-left: 2px;
  line-height: 1.5;
}

/* ===== 字段错误 ===== */
.field-error {
  color: #b91c1c;
  font-size: 14px;
  margin-top: 4px;
  padding: 4px 8px;
  background: #fef2f2;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-error span[aria-hidden="true"] {
  font-size: 14px;
}

/* ===== 全局错误 ===== */
.global-error {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fef2f2;
  border-radius: 10px;
  border-left: 4px solid #dc2626;
  margin-top: 16px;
  color: #991b1b;
  font-size: 15px;
  flex-wrap: wrap;
}

.global-error:focus-visible {
  outline: 2px solid #dc2626;
  outline-offset: 2px;
}

.global-error .el-button {
  margin-left: auto;
  min-height: 32px;
  padding: 0 12px;
}

/* ===== 按钮样式 ===== */
.start-btn {
  width: 100%;
  margin-top: 8px;
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  font-size: 16px;
  font-weight: 500;
  height: 50px;
  border-radius: 10px;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: opacity 0.2s, transform 0.1s, box-shadow 0.2s;
  cursor: pointer;
}

.start-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(76, 138, 110, 0.25);
}

.start-btn:focus-visible {
  outline: 3px solid #2c4d3d;
  outline-offset: 2px;
  box-shadow: 0 0 0 4px rgba(76, 138, 110, 0.15);
}

.start-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.start-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.start-btn .el-icon {
  font-size: 18px;
}

/* ===== 键盘提示 ===== */
.keyboard-hint {
  text-align: center;
  margin-top: 12px;
  font-size: 12px;
  color: #6b8a7a;
}

kbd {
  display: inline-block;
  padding: 2px 8px;
  background: #f0f2f5;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  color: #2c4d3d;
  box-shadow: 0 1px 0 #d0d7de;
}

/* ===== 底部留白 ===== */
.blank-area {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 60px;
}

/* ===== 响应式适配 ===== */
@media (max-width: 768px) {
  .start-page {
    padding: 16px;
  }

  .hero-section {
    padding: 20px 16px;
    margin-bottom: 24px;
  }

  .hero-title {
    font-size: 26px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .content-section {
    padding: 24px 20px;
  }

  .card-title {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .start-page {
    padding: 12px;
  }

  .hero-section {
    padding: 16px 12px;
  }

  .hero-title {
    font-size: 22px;
  }

  .content-section {
    padding: 20px 16px;
  }

  .el-form-item {
    margin-bottom: 18px;
  }
}

/* ===== 高对比度模式 ===== */
@media (prefers-contrast: high) {
  .hero-section {
    border: 3px solid #1a4d36;
  }

  .content-section {
    border: 2px solid #2c4d3d;
  }

  .el-select :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 2px #2c4d3d !important;
  }

  .el-input :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 2px #2c4d3d !important;
  }

  .start-btn {
    background: #2c6b4f;
    border: 2px solid #1a4d36;
  }

  .start-btn:hover:not(:disabled) {
    background: #1a4d36;
  }

  :deep(.el-form-item.is-error .el-input__wrapper) {
    box-shadow: 0 0 0 2px #b91c1c !important;
  }

  :deep(.el-form-item.is-error .el-select .el-input__wrapper) {
    box-shadow: 0 0 0 2px #b91c1c !important;
  }

  .global-error {
    border: 2px solid #dc2626;
  }

  .field-error {
    border: 1px solid #dc2626;
  }
}

/* ===== 减少动画 ===== */
@media (prefers-reduced-motion: reduce) {
  .start-page {
    animation: none;
  }

  .bubble {
    animation: none !important;
    display: none !important;
  }

  .bubbles {
    display: none !important;
  }

  .start-btn {
    transition: none;
  }

  .start-btn:hover:not(:disabled) {
    transform: none;
  }
}

/* ===== 打印样式 ===== */
@media print {
  .start-page {
    background: white !important;
    animation: none !important;
    padding: 20px;
  }

  .bubbles {
    display: none !important;
  }

  .hero-section {
    border: 1px solid #ccc !important;
    background: #f9f9f9 !important;
  }

  .content-section {
    border: 1px solid #ccc !important;
    box-shadow: none !important;
  }

  .start-btn {
    background: #2c6b4f !important;
    color: white !important;
  }

  .blank-area {
    display: none !important;
  }
}
</style>