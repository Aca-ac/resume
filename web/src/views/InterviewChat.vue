<template>
  <div class="chat-page">
    <h2 class="page-title">Interview Chat</h2>
    <div class="messages-panel">
      <transition-group name="msg" tag="div" class="messages">
        <div
          v-for="(m, idx) in interview.messages"
          :key="idx"
          class="msg-bubble"
          :class="m.role"
        >
          <div class="msg-avatar">{{ m.role === 'user' ? 'You' : 'AI' }}</div>
          <div class="msg-body">
            <span class="msg-role">{{ m.role === 'user' ? 'You' : 'Interviewer' }}</span>
            <p>{{ m.content }}</p>
          </div>
        </div>
        <div v-if="loading" key="typing" class="msg-bubble assistant typing">
          <div class="msg-avatar">AI</div>
          <div class="msg-body">
            <span class="typing-dots"><span></span><span></span><span></span></span>
          </div>
        </div>
      </transition-group>
    </div>
    <el-input v-model="answer" type="textarea" :rows="4" placeholder="Type your answer..." class="input-area" />
    <div class="actions">
      <el-button type="primary" :loading="loading" @click="onSend">Send</el-button>
      <el-button @click="$router.push(`/interview/${sessionId}/report`)">Finish & Report</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { fetchMessages } from "@/api/interview";
import { ElMessage } from "element-plus";

const route = useRoute();
const interview = useInterviewStore();
const sessionId = computed(() => Number(route.params.sessionId));
const answer = ref("");
const loading = ref(false);

onMounted(async () => {
  interview.sessionId = sessionId.value;
  interview.messages = await fetchMessages(sessionId.value);
});

async function onSend() {
  if (!answer.value.trim()) return;
  loading.value = true;
  try {
    await interview.answer(answer.value);
    answer.value = "";
  } catch (e: any) {
    ElMessage.error(e.message || "Send failed");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.chat-page {
  max-width: 800px;
}

.messages-panel {
  background: #fff;
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  padding: 20px;
  min-height: 320px;
  max-height: 480px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-bubble {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.msg-bubble.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.user .msg-avatar {
  background: linear-gradient(135deg, #409eff, #337ecc);
}

.assistant .msg-avatar {
  background: linear-gradient(135deg, #67c23a, #529b2e);
}

.msg-body {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 12px;
  border-top-left-radius: 4px;
}

.user .msg-body {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(51, 126, 204, 0.08));
  border-top-left-radius: 12px;
  border-top-right-radius: 4px;
}

.msg-role {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.msg-body p {
  margin: 6px 0 0;
  line-height: 1.6;
}

.msg-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.msg-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}

.typing-dots span {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  margin: 0 3px;
  animation: typingBounce 1.2s ease-in-out infinite;
}

.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.input-area {
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>