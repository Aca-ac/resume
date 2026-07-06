<template>
  <div>
    <h2>Interview Chat</h2>
    <div class="messages">
      <div v-for="(m, idx) in interview.messages" :key="idx" class="msg" :class="m.role">
        <strong>{{ m.role }}:</strong> {{ m.content }}
      </div>
    </div>
    <el-input v-model="answer" type="textarea" :rows="4" placeholder="Your answer" />
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
.messages { border: 1px solid #eee; padding: 12px; min-height: 240px; margin-bottom: 12px; }
.msg { margin-bottom: 8px; }
.user { color: #409eff; }
.assistant { color: #67c23a; }
.actions { margin-top: 8px; display: flex; gap: 8px; }
</style>
