<template>
  <div>
    <h2>Job Description Match</h2>
    <el-form label-width="100px">
      <el-form-item label="Resume">
        <el-select v-model="resumeId" placeholder="Select resume" style="width: 100%">
          <el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="JD">
        <el-input v-model="jdText" type="textarea" :rows="10" />
      </el-form-item>
      <el-button type="primary" :loading="loading" @click="onMatch">Analyze</el-button>
    </el-form>
    <el-card v-if="matchStore.latest" class="result">
      <h3>Score: {{ matchStore.latest.matchScore }}</h3>
      <pre>{{ matchStore.latest.analysis }}</pre>
    </el-card>
    <h3>History</h3>
    <el-table :data="matchStore.history">
      <el-table-column prop="resumeId" label="Resume" />
      <el-table-column prop="matchScore" label="Score" width="100" />
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
.result { margin-top: 16px; }
pre { white-space: pre-wrap; }
</style>

