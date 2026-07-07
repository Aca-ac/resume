<template>
  <div class="report-page">
    <div class="toolbar">
      <h2 class="page-title">Interview Report</h2>
      <el-button type="primary" :loading="loading" @click="onGenerate">Regenerate Report</el-button>
    </div>
    <transition name="scale-fade">
      <el-card v-if="report" class="report-card animate-fade-up" shadow="hover">
        <pre>{{ report }}</pre>
      </el-card>
    </transition>
    <el-empty v-if="!report && !loading" description="No report yet" class="animate-fade-up" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { ElMessage } from "element-plus";

const route = useRoute();
const interview = useInterviewStore();
const sessionId = computed(() => Number(route.params.sessionId));
const loading = ref(false);
const report = ref("");

async function loadReport() {
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

onMounted(() => {
  loadReport();
});

async function onGenerate() {
  await loadReport();
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar .page-title {
  margin: 0;
}

.report-card {
  border-radius: var(--app-radius);
  border-left: 4px solid #667eea;
}

.report-card pre {
  white-space: pre-wrap;
  margin: 0;
  line-height: 1.7;
  color: #606266;
}

.scale-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.scale-fade-enter-from {
  opacity: 0;
  transform: translateY(16px);
}
</style>