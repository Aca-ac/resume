<!-- src/views/InterviewReport.vue -->
<template>
  <div class="report-page">
    <div class="page-content">
      <!-- 页面标题和工具栏 -->
      <header class="toolbar" aria-labelledby="page-title">
        <h1 id="page-title" class="page-title">面试报告</h1>
        <div class="toolbar-actions" role="group" aria-label="报告操作按钮">
          <el-button
              type="primary"
              :loading="loading"
              @click="onGenerate"
              :aria-label="loading ? '正在重新生成报告...' : '重新生成报告'"
              :disabled="loading"
          >
            <span v-if="!loading">重新生成报告</span>
            <span v-else>生成中...</span>
          </el-button>
          <el-button
              @click="goStartNew"
              :disabled="loading"
              aria-label="开始新的面试"
          >
            开始新面试
          </el-button>
        </div>
      </header>

      <!-- 报告卡片 -->
      <transition name="scale-fade">
        <div
            v-if="report"
            class="report-card animate-fade-up"
            role="article"
            aria-label="面试报告内容"
        >
          <div class="report-header">
            <span class="status-tag" aria-hidden="true">
              <span class="status-dot"></span>
              面试已完成
            </span>
            <span class="sr-only">面试状态：已完成</span>
          </div>

          <div class="report-content" role="document" aria-label="报告详情">
            <pre>{{ report }}</pre>
          </div>

          <!-- 报告操作提示 -->
          <div class="report-actions">
            <el-button
                size="small"
                @click="copyReport"
                :aria-label="copySuccess ? '报告已复制' : '复制报告内容'"
            >
              {{ copySuccess ? '✓ 已复制' : '📋 复制报告' }}
            </el-button>
          </div>
        </div>
      </transition>

      <!-- 空状态 -->
      <div
          v-if="!report && !loading"
          class="empty-state animate-fade-up"
          role="status"
          aria-live="polite"
      >
        <div class="empty-icon" aria-hidden="true">📄</div>
        <p class="empty-description">暂未生成报告，请完成面试</p>
        <el-button
            type="primary"
            @click="goStartNew"
            aria-label="前往开始新的面试"
        >
          开始新面试
        </el-button>
      </div>

      <!-- 加载状态 -->
      <div
          v-if="loading"
          class="loading-state"
          role="status"
          aria-live="polite"
          aria-label="正在加载报告"
      >
        <div class="loading-spinner" aria-hidden="true"></div>
        <p>正在生成面试报告...</p>
      </div>

      <!-- 错误提示区域 -->
      <div
          v-if="errorMessage"
          class="error-state"
          role="alert"
          aria-live="assertive"
      >
        <span aria-hidden="true">⚠️</span>
        <span>{{ errorMessage }}</span>
        <el-button
            size="small"
            @click="retryLoad"
            aria-label="重试加载报告"
        >
          重试
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();
const interview = useInterviewStore();

const sessionId = computed(() => Number(route.params.sessionId));
const loading = ref(false);
const report = ref("");
const errorMessage = ref("");
const copySuccess = ref(false);

// 清理可能存在的旧状态
const clearState = () => {
  errorMessage.value = "";
  copySuccess.value = false;
};

async function loadReport() {
  clearState();

  // 验证sessionId
  if (!sessionId.value || isNaN(sessionId.value)) {
    errorMessage.value = "无效的面试会话ID";
    return;
  }

  loading.value = true;
  try {
    const session = await interview.loadReport(sessionId.value);
    if (session?.report) {
      report.value = session.report;
    } else {
      errorMessage.value = "该面试暂无报告，请先完成面试";
    }
  } catch (e: any) {
    const msg = e.message || "获取报告失败，请稍后重试";
    errorMessage.value = msg;
    ElMessage.error({
      message: msg,
      duration: 5000,
    });
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadReport();
});

async function onGenerate() {
  if (loading.value) return;
  await loadReport();
  if (report.value) {
    ElMessage.success({
      message: "报告已重新生成",
      duration: 3000,
    });
  }
}

function goStartNew() {
  if (loading.value) {
    ElMessage.warning({
      message: "请等待报告生成完成",
      duration: 3000,
    });
    return;
  }
  router.push("/interview/start");
}

function retryLoad() {
  loadReport();
}

// 复制报告功能
async function copyReport() {
  if (!report.value) {
    ElMessage.warning("没有可复制的内容");
    return;
  }

  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(report.value);
    } else {
      // 降级方案
      const textarea = document.createElement('textarea');
      textarea.value = report.value;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }

    copySuccess.value = true;
    ElMessage.success({
      message: "报告已复制到剪贴板",
      duration: 3000,
    });

    // 重置复制状态
    setTimeout(() => {
      copySuccess.value = false;
    }, 3000);
  } catch (e) {
    ElMessage.error({
      message: "复制失败，请手动选择内容复制",
      duration: 4000,
    });
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
.report-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 40%, #FBFCCD 70%, #CDE2E8 100%);
  background-size: 400% 400%;
  animation: gradientMove 12s ease-in-out infinite;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.page-content {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 8px;
}

/* ===== 工具栏 - 提升可访问性 ===== */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #2c4d3d;
  margin: 0;
  line-height: 1.3;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-actions .el-button {
  min-height: 44px;
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 10px;
}

.toolbar-actions .el-button--primary {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #ffffff;
}

.toolbar-actions .el-button--primary:hover:not(:disabled) {
  opacity: 0.9;
}

.toolbar-actions .el-button--primary:focus-visible {
  outline: 3px solid #2c4d3d;
  outline-offset: 2px;
}

.toolbar-actions .el-button--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-actions .el-button:not(.el-button--primary) {
  border-color: #b8ccbf;
  color: #2c4d3d;
}

.toolbar-actions .el-button:not(.el-button--primary):hover:not(:disabled) {
  border-color: #4c8a6e;
  color: #2c6b4f;
}

.toolbar-actions .el-button:not(.el-button--primary):focus-visible {
  outline: 3px solid #4c8a6e;
  outline-offset: 2px;
}

.toolbar-actions .el-button:not(.el-button--primary):disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== 报告卡片 ===== */
.report-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-left: 5px solid #4c8a6e;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(76, 138, 110, 0.12);
  animation: fadeUp 0.6s ease-out;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 报告头部 ===== */
.report-header {
  padding: 18px 24px 0;
  border-bottom: 1px solid rgba(100, 163, 134, 0.12);
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: #ecfdf5;
  color: #065f46;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid #a7d7c5;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #059669;
  display: inline-block;
}

/* ===== 报告内容 ===== */
.report-content {
  padding: 24px;
}

.report-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  line-height: 1.8;
  color: #1f2e26;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  font-size: 15px;
  background: #f8faf9;
  padding: 20px 24px;
  border-radius: 12px;
  border: 1px solid #e8ecf0;
  max-height: 600px;
  overflow-y: auto;
}

/* ===== 报告操作按钮 ===== */
.report-actions {
  padding: 0 24px 20px;
  display: flex;
  justify-content: flex-end;
}

.report-actions .el-button {
  min-height: 36px;
  font-size: 14px;
  border-color: #b8ccbf;
  color: #2c4d3d;
}

.report-actions .el-button:hover:not(:disabled) {
  border-color: #4c8a6e;
  color: #2c6b4f;
}

.report-actions .el-button:focus-visible {
  outline: 3px solid #4c8a6e;
  outline-offset: 2px;
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation: fadeUp 0.6s ease-out;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  line-height: 1;
}

.empty-description {
  font-size: 18px;
  color: #4a5e51;
  margin: 0 0 20px;
}

.empty-state .el-button {
  min-height: 44px;
  padding: 0 32px;
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #ffffff;
  font-size: 15px;
  border-radius: 10px;
}

.empty-state .el-button:hover:not(:disabled) {
  opacity: 0.9;
}

.empty-state .el-button:focus-visible {
  outline: 3px solid #2c4d3d;
  outline-offset: 2px;
}

/* ===== 加载状态 ===== */
.loading-state {
  text-align: center;
  padding: 80px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  border: 4px solid #e8ecf0;
  border-top: 4px solid #4c8a6e;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-state p {
  color: #4a5e51;
  font-size: 16px;
  margin: 0;
}

/* ===== 错误状态 ===== */
.error-state {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fef2f2;
  border-radius: 12px;
  border-left: 4px solid #dc2626;
  margin-top: 16px;
  color: #991b1b;
  font-size: 15px;
  flex-wrap: wrap;
}

.error-state .el-button {
  margin-left: auto;
  min-height: 36px;
  padding: 0 16px;
}

/* ===== 过渡动画 ===== */
.scale-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.scale-fade-enter-from {
  opacity: 0;
  transform: scale(0.96) translateY(16px);
}
.scale-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.scale-fade-leave-to {
  opacity: 0;
  transform: scale(0.96) translateY(-16px);
}

/* ===== 响应式适配 ===== */
@media (max-width: 768px) {
  .report-page {
    padding: 16px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .page-title {
    font-size: 22px;
    text-align: center;
  }

  .toolbar-actions {
    justify-content: center;
  }

  .toolbar-actions .el-button {
    flex: 1;
    min-width: 120px;
  }

  .report-content pre {
    font-size: 14px;
    padding: 16px;
    max-height: 400px;
  }

  .report-header {
    padding: 14px 16px 0;
  }

  .report-content {
    padding: 16px;
  }

  .report-actions {
    padding: 0 16px 16px;
  }

  .empty-state {
    padding: 60px 16px;
  }

  .empty-icon {
    font-size: 48px;
  }
}

/* ===== 高对比度模式 ===== */
@media (prefers-contrast: high) {
  .report-card {
    border: 2px solid #1a4d36;
    border-left-width: 6px;
  }

  .status-tag {
    border: 2px solid #065f46;
  }

  .report-content pre {
    border: 2px solid #2c4d3d;
  }

  .toolbar {
    border: 2px solid #2c4d3d;
  }

  .empty-state {
    border: 2px solid #2c4d3d;
  }

  .loading-state {
    border: 2px solid #2c4d3d;
  }
}

/* ===== 减少动画 ===== */
@media (prefers-reduced-motion: reduce) {
  .report-page {
    animation: none;
  }

  .report-card {
    animation: none;
  }

  .empty-state {
    animation: none;
  }

  .loading-spinner {
    animation-duration: 2s;
  }
}

/* ===== 打印样式 ===== */
@media print {
  .report-page {
    background: white !important;
    animation: none !important;
    padding: 20px;
  }

  .toolbar-actions,
  .report-actions {
    display: none !important;
  }

  .report-card {
    border: 1px solid #ccc !important;
    box-shadow: none !important;
  }

  .report-content pre {
    max-height: none !important;
    overflow: visible !important;
    background: #f9f9f9 !important;
  }

  .empty-state,
  .loading-state,
  .error-state {
    display: none !important;
  }
}
</style>