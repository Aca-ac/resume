<!-- src/views/InterviewChat.vue -->
<template>
  <div class="chat-page">
    <!-- 页面标题区域 -->
    <section class="hero-section" aria-labelledby="hero-title">
      <h1 id="hero-title" class="hero-title">
        <span aria-hidden="true">🎤</span>
        模拟面试
      </h1>
      <p class="hero-subtitle">根据你的简历与目标职位进行多轮问答，结束后可生成面试报告。</p>
    </section>

    <!-- 主要内容区域 -->
    <section class="content-section" aria-label="面试对话区域">
      <!-- 消息列表 -->
      <div
          class="messages-panel"
          role="log"
          aria-live="polite"
          aria-atomic="false"
          aria-label="面试对话消息列表"
          ref="messagesPanelRef"
      >
        <div
            v-for="(m, idx) in interview.messages"
            :key="idx"
            class="msg-bubble"
            :class="m.role"
            :role="m.role === 'user' ? 'article' : 'article'"
            :aria-label="`${m.role === 'user' ? '我' : '面试官'}的消息`"
        >
          <div class="msg-avatar" :aria-hidden="true">
            {{ m.role === "user" ? "我" : "AI" }}
          </div>
          <div class="msg-body">
            <span class="msg-role" :id="`msg-role-${idx}`">
              {{ m.role === "user" ? "我" : "面试官" }}
            </span>
            <p :aria-labelledby="`msg-role-${idx}`">{{ m.content }}</p>
          </div>
        </div>

        <!-- 加载中状态 -->
        <div v-if="loading" class="msg-bubble assistant" role="status" aria-live="polite">
          <div class="msg-avatar" aria-hidden="true">AI</div>
          <div class="msg-body">
            <span class="msg-role">面试官</span>
            <p>思考中…</p>
          </div>
        </div>

        <!-- 空状态提示 -->
        <div v-if="!interview.messages.length && !loading" class="empty-state" role="status">
          <p>暂无消息，请开始面试对话</p>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area-wrapper">
        <label for="answer-input" class="sr-only">输入你的回答</label>
        <el-input
            id="answer-input"
            v-model="answer"
            type="textarea"
            :rows="4"
            placeholder="输入你的回答…"
            class="input-area"
            resize="vertical"
            @keydown.ctrl.enter="onSend"
            @keydown.meta.enter="onSend"
            aria-describedby="input-hint"
            :aria-invalid="!!inputError"
        />
        <span id="input-hint" class="sr-only">
          按 Ctrl+Enter 或 Command+Enter 快速发送
        </span>

        <!-- 输入错误提示 -->
        <div v-if="inputError" class="input-error" role="alert" aria-live="polite">
          <span aria-hidden="true">⚠️</span>
          {{ inputError }}
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <el-button
            type="primary"
            :loading="loading"
            @click="onSend"
            :disabled="loading || !answer.trim()"
            :aria-label="loading ? '正在发送回答...' : '发送回答'"
        >
          <span v-if="!loading">发送回答</span>
          <span v-else>发送中...</span>
          <span class="sr-only">快捷键 Ctrl+Enter</span>
        </el-button>
        <el-button
            @click="goReport"
            :disabled="loading"
            :aria-label="'结束面试并查看报告'"
        >
          结束并查看报告
        </el-button>
      </div>

      <!-- 键盘快捷键提示 -->
      <div class="keyboard-hint" aria-hidden="true">
        <kbd>Ctrl</kbd> + <kbd>Enter</kbd> 快速发送
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { fetchMessages } from "@/api/interview";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();
const interview = useInterviewStore();
const sessionId = computed(() => Number(route.params.sessionId));
const answer = ref("");
const loading = ref(false);
const inputError = ref("");
const messagesPanelRef = ref<HTMLElement | null>(null);

// 自动滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesPanelRef.value) {
      messagesPanelRef.value.scrollTop = messagesPanelRef.value.scrollHeight;
    }
  });
};

// 监听消息变化，自动滚动
watch(
    () => interview.messages.length,
    () => {
      scrollToBottom();
    },
    { immediate: true }
);

onMounted(async () => {
  interview.sessionId = sessionId.value;
  try {
    interview.messages = await fetchMessages(sessionId.value);
    scrollToBottom();
  } catch (e: any) {
    ElMessage.error({
      message: e.message || "加载对话失败",
      duration: 5000,
    });
  }
});

async function onSend() {
  // 清除之前的错误
  inputError.value = "";

  // 验证输入
  const trimmedAnswer = answer.value.trim();
  if (!trimmedAnswer) {
    inputError.value = "请输入您的回答内容";
    // 聚焦到输入框
    const inputElement = document.getElementById("answer-input");
    if (inputElement) {
      inputElement.focus();
    }
    return;
  }

  // 检查字数限制（可选）
  if (trimmedAnswer.length < 2) {
    inputError.value = "回答内容至少需要2个字符";
    const inputElement = document.getElementById("answer-input");
    if (inputElement) {
      inputElement.focus();
    }
    return;
  }

  loading.value = true;
  try {
    await interview.answer(trimmedAnswer);
    answer.value = "";
    scrollToBottom();
  } catch (e: any) {
    const errorMsg = e.message || "发送失败，请重试";
    ElMessage.error({
      message: errorMsg,
      duration: 5000,
    });
    // 聚焦回输入框以便重试
    const inputElement = document.getElementById("answer-input");
    if (inputElement) {
      inputElement.focus();
    }
  } finally {
    loading.value = false;
  }
}

function goReport() {
  if (loading.value) {
    ElMessage.warning({
      message: "请等待当前回答发送完成",
      duration: 3000,
    });
    return;
  }

  // 检查是否有消息
  if (interview.messages.length === 0) {
    ElMessage.warning({
      message: "暂无对话记录，无法生成报告",
      duration: 3000,
    });
    return;
  }

  router.push(`/interview/${sessionId.value}/report`);
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

.chat-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #cde2e8 0%, #bcddbe 100%);
}

/* ===== 标题区域 - 高对比度 ===== */
.hero-section {
  text-align: center;
  margin-bottom: 24px;
  padding: 24px 32px;
  background: #fbfccd;
  border-radius: 16px;
  border: 2px solid #e8e9b8;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 28px;
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
  line-height: 1.6;
}

/* ===== 内容区域 ===== */
.content-section {
  max-width: 860px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 16px;
  padding: 24px 24px 20px;
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.15);
}

/* ===== 消息面板 ===== */
.messages-panel {
  min-height: 320px;
  max-height: 480px;
  overflow-y: auto;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px;
  /* 滚动条样式优化 */
  scroll-behavior: smooth;
}

/* 自定义滚动条 - 提高可见性 */
.messages-panel::-webkit-scrollbar {
  width: 8px;
}

.messages-panel::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.messages-panel::-webkit-scrollbar-thumb {
  background: #b0c4b8;
  border-radius: 4px;
}

.messages-panel::-webkit-scrollbar-thumb:hover {
  background: #8aa89a;
}

/* ===== 消息气泡 ===== */
.msg-bubble {
  display: flex;
  gap: 12px;
  max-width: 88%;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-bubble.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  background: #2c6b4f;
  /* 确保文字与背景有足够对比度 */
}

.user .msg-avatar {
  background: #1a5a8c;
}

.msg-body {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid #e8ecf0;
}

.user .msg-body {
  background: #ecf5ff;
  border-color: #d4e3f0;
}

.msg-role {
  display: block;
  font-size: 12px;
  color: #4a5e51;
  margin-bottom: 4px;
  font-weight: 600;
}

.msg-body p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #1f2e26;
  font-size: 15px;
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6b8a7a;
  font-size: 16px;
}

/* ===== 输入区域 ===== */
.input-area-wrapper {
  margin-bottom: 12px;
}

.input-area {
  width: 100%;
}

:deep(.el-textarea__inner) {
  border-radius: 10px;
  border-color: #b8ccbf;
  font-size: 15px;
  line-height: 1.6;
  padding: 12px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

:deep(.el-textarea__inner:focus) {
  border-color: #4c8a6e;
  box-shadow: 0 0 0 3px rgba(76, 138, 110, 0.15);
  outline: none;
}

:deep(.el-textarea__inner:disabled) {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

/* ===== 输入错误 ===== */
.input-error {
  color: #b91c1c;
  font-size: 14px;
  margin-top: 6px;
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 6px;
  border-left: 4px solid #dc2626;
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-error span[aria-hidden="true"] {
  font-size: 16px;
}

/* ===== 操作按钮 ===== */
.actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.actions .el-button {
  min-height: 44px;
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 10px;
}

.actions .el-button--primary {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #ffffff;
}

.actions .el-button--primary:hover:not(:disabled) {
  opacity: 0.9;
}

.actions .el-button--primary:focus-visible {
  outline: 3px solid #2c4d3d;
  outline-offset: 2px;
}

.actions .el-button--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.actions .el-button:not(.el-button--primary) {
  border-color: #b8ccbf;
  color: #2c4d3d;
}

.actions .el-button:not(.el-button--primary):hover:not(:disabled) {
  border-color: #4c8a6e;
  color: #2c6b4f;
}

.actions .el-button:not(.el-button--primary):focus-visible {
  outline: 3px solid #4c8a6e;
  outline-offset: 2px;
}

.actions .el-button:not(.el-button--primary):disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* ===== 响应式适配 ===== */
@media (max-width: 768px) {
  .chat-page {
    padding: 16px;
  }

  .hero-section {
    padding: 16px 20px;
  }

  .hero-title {
    font-size: 22px;
  }

  .content-section {
    padding: 16px;
  }

  .messages-panel {
    min-height: 240px;
    max-height: 360px;
  }

  .msg-bubble {
    max-width: 94%;
  }

  .actions {
    flex-direction: column;
  }

  .actions .el-button {
    width: 100%;
  }
}

/* ===== 高对比度模式 ===== */
@media (prefers-contrast: high) {
  .hero-section {
    border: 3px solid #1a4d36;
  }

  .msg-body {
    border: 2px solid #2c4d3d;
  }

  .user .msg-body {
    border-color: #1a5a8c;
  }

  :deep(.el-textarea__inner) {
    border-width: 2px;
    border-color: #2c4d3d;
  }

  :deep(.el-textarea__inner:focus) {
    border-color: #1a4d36;
    box-shadow: 0 0 0 4px rgba(76, 138, 110, 0.3);
  }

  .actions .el-button--primary {
    background: #2c6b4f;
    border: 2px solid #1a4d36;
  }
}

/* ===== 减少动画 ===== */
@media (prefers-reduced-motion: reduce) {
  .msg-bubble {
    animation: none;
  }

  .messages-panel {
    scroll-behavior: auto;
  }
}

/* ===== 打印样式 ===== */
@media print {
  .chat-page {
    background: white;
    padding: 20px;
  }

  .actions,
  .input-area-wrapper,
  .keyboard-hint {
    display: none;
  }

  .messages-panel {
    max-height: none;
    overflow: visible;
  }
}
</style>