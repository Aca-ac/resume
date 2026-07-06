<template>
  <div>
    <h2>Interview Report</h2>
    <el-button type="primary" :loading="loading" @click="onGenerate">Generate Report</el-button>
    <el-card v-if="report" class="report"><pre>{{ report }}</pre></el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { ElMessage } from "element-plus";

const route = useRoute();
const interview = useInterviewStore();
const sessionId = computed(() => Number(route.params.sessionId));
const loading = ref(false);
const report = ref("");

async function onGenerate() {
  loading.value = true;
  try {
    const session = await interview.loadReport(sessionId.value);
    report.value = session.report || "";
  } catch (e: any) {
    ElMessage.error(e.message || "Report failed");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.report { margin-top: 16px; }
pre { white-space: pre-wrap; }
</style>
