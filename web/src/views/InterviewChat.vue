<template>
  <div class="chat-page">
    <section class="hero-section">
      <h1 class="hero-title">🎤 模拟面试</h1>
      <p class="hero-subtitle">根据你的简历与目标职位进行多轮问答，结束后可生成面试报告。</p>
    </section>

    <section class="content-section">
      <div class="messages-panel">
        <div v-for="(m, idx) in interview.messages" :key="idx" class="msg-bubble" :class="m.role">
          <div class="msg-avatar">{{ m.role === "user" ? "我" : "AI" }}</div>
          <div class="msg-body">
            <span class="msg-role">{{ m.role === "user" ? "我" : "面试官" }}</span>
            <p>{{ m.content }}</p>
          </div>
        </div>
        <div v-if="loading" class="msg-bubble assistant">
          <div class="msg-avatar">AI</div>
          <div class="msg-body"><span class="msg-role">面试官</span><p>思考中…</p></div>
        </div>
      </div>

      <el-input
          v-model="answer"
          type="textarea"
          :rows="4"
          placeholder="输入你的回答…"
          class="input-area"
      />
      <div class="actions">
        <el-button type="primary" :loading="loading" @click="onSend">发送回答</el-button>
        <el-button @click="goReport">结束并查看报告</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
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

onMounted(async () => {
  interview.sessionId = sessionId.value;
  try {
    interview.messages = await fetchMessages(sessionId.value);
  } catch (e: any) {
    ElMessage.error(e.message || "加载对话失败");
  }
});

async function onSend() {
  if (!answer.value.trim()) return;
  loading.value = true;
  try {
    await interview.answer(answer.value);
    answer.value = "";
  } catch (e: any) {
    ElMessage.error(e.message || "发送失败");
  } finally {
    loading.value = false;
  }
}

function goReport() {
  router.push(`/interview/${sessionId.value}/report`);
}
</script>

<style scoped>
.chat-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #cde2e8 0%, #bcddbe 100%);
}

.hero-section {
  text-align: center;
  margin-bottom: 24px;
  padding: 24px 32px;
  background: #fbfccd;
  border-radius: 16px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 28px;
  color: #64a386;
}

.hero-subtitle {
  margin: 0;
  color: #4f6b5d;
}

.content-section {
  max-width: 860px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
}

.messages-panel {
  min-height: 320px;
  max-height: 480px;
  overflow-y: auto;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.msg-bubble {
  display: flex;
  gap: 12px;
  max-width: 88%;
}

.msg-bubble.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  background: #64a386;
}

.user .msg-avatar {
  background: #409eff;
}

.msg-body {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 10px 14px;
}

.user .msg-body {
  background: #ecf5ff;
}

.msg-role {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.msg-body p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
</style>
