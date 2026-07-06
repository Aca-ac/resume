<template>
  <div class="match-page">
    <h2 class="page-title">Job Description Match</h2>
    <el-card class="form-card animate-fade-up" shadow="never">
      <el-form label-width="100px">
        <el-form-item label="Resume">
          <el-select v-model="resumeId" placeholder="Select resume" style="width: 100%">
            <el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="JD">
          <el-input v-model="jdText" type="textarea" :rows="10" placeholder="Paste job description here..." />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onMatch">Analyze Match</el-button>
      </el-form>
    </el-card>

    <transition name="scale-fade">
      <el-card v-if="matchStore.latest" class="result-card animate-scale-in" shadow="hover">
        <div class="score-wrap">
          <div class="score-ring" :style="{ '--score': matchStore.latest.matchScore }">
            <span class="score-value">{{ matchStore.latest.matchScore }}</span>
            <span class="score-label">Match</span>
          </div>
          <div class="analysis">
            <h3>Analysis</h3>
            <pre>{{ matchStore.latest.analysis }}</pre>
          </div>
        </div>
      </el-card>
    </transition>

    <h3 class="history-title">History</h3>
    <el-table :data="matchStore.history" class="history-table" stripe>
      <el-table-column prop="resumeId" label="Resume" />
      <el-table-column prop="matchScore" label="Score" width="100">
        <template #default="{ row }">
          <el-tag :type="scoreTagType(row.matchScore)" effect="plain">{{ row.matchScore }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="Time" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useMatchStore } from "@/stores/match";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";

const matchStore = useMatchStore();
const resumeStore = useResumeStore();
const resumes = computed(() => resumeStore.list);
const resumeId = ref<number | null>(null);
const jdText = ref("");
const loading = ref(false);

function scoreTagType(score: number) {
  if (score >= 80) return "success";
  if (score >= 60) return "warning";
  return "danger";
}

onMounted(async () => {
  await resumeStore.loadList();
  await matchStore.loadHistory();
  if (resumeStore.list.length) resumeId.value = resumeStore.list[0].id;
});

async function onMatch() {
  if (!resumeId.value || !jdText.value.trim()) {
    ElMessage.warning("Select resume and paste JD");
    return;
  }
  loading.value = true;
  try {
    await matchStore.runMatch(resumeId.value, jdText.value);
  } catch (e: any) {
    ElMessage.error(e.message || "Match failed");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.form-card {
  border-radius: var(--app-radius);
  margin-bottom: 20px;
}

.result-card {
  border-radius: var(--app-radius);
  margin-bottom: 24px;
  border: none;
}

.score-wrap {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.score-ring {
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: conic-gradient(
    #67c23a calc(var(--score) * 1%),
    #ebeef5 calc(var(--score) * 1%)
  );
  animation: scaleIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  position: relative;
}

.score-ring::before {
  content: "";
  position: absolute;
  inset: 8px;
  border-radius: 50%;
  background: #fff;
}

.score-value,
.score-label {
  position: relative;
  z-index: 1;
}

.score-value {
  font-size: 2rem;
  font-weight: 700;
  color: #67c23a;
  line-height: 1;
}

.score-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.analysis {
  flex: 1;
}

.analysis h3 {
  margin: 0 0 12px;
}

.analysis pre {
  white-space: pre-wrap;
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.history-title {
  margin: 0 0 12px;
  font-size: 1rem;
}

.scale-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.scale-fade-enter-from {
  opacity: 0;
  transform: scale(0.95) translateY(12px);
}
</style>