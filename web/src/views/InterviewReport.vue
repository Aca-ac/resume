<template>
  <div class="report-page">
    <div class="page-content">
      <div class="toolbar">
        <h2 class="page-title">面试报告</h2>
        <div class="toolbar-actions">
          <el-button type="primary" :loading="loading" @click="onGenerate">
            重新生成报告
          </el-button>
          <el-button @click="$router.push('/interview/start')">开始新面试</el-button>
        </div>
      </div>

      <transition name="scale-fade">
        <el-card v-if="report" class="report-card animate-fade-up" shadow="hover">
          <div class="report-header">
            <el-tag type="success" size="large">面试已完成</el-tag>
          </div>
          <div class="report-content">
            <pre>{{ report }}</pre>
          </div>
        </el-card>
      </transition>

      <el-empty
          v-if="!report && !loading"
          description="暂未生成报告，请完成面试"
          class="animate-fade-up"
      />
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

async function loadReport() {
  loading.value = true;
  try {
    const session = await interview.loadReport(sessionId.value);
    report.value = session.report || "";
  } catch (e: any) {
    ElMessage.error(e.message || "获取报告失败");
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
.report-page {
  width: 100%;
  min-height: 100vh;
  padding: 20px 40px;
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
  max-width: 820px;
  margin: 0 auto;
  padding: 0 8px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #2c4d3d;
  margin: 0;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.report-card {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-left: 4px solid #64A386;
  overflow: hidden;
}

.report-header {
  padding: 16px 20px 0;
  border-bottom: 1px solid rgba(100, 163, 134, 0.1);
}

.report-content {
  padding: 20px;
}

.report-content pre {
  white-space: pre-wrap;
  margin: 0;
  line-height: 1.8;
  color: #2c4d3d;
  font-family: inherit;
}

.animate-fade-up {
  animation: fadeUp 0.6s ease-out;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.scale-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.scale-fade-enter-from {
  opacity: 0;
  transform: translateY(16px);

}
</style>
