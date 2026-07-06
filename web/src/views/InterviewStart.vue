<template>
  <div class="start-page">
    <h2 class="page-title">Start Mock Interview</h2>
    <el-card class="form-card animate-fade-up hover-lift" shadow="hover">
      <div class="hero-icon animate-scale-in">
        <el-icon :size="48"><ChatDotRound /></el-icon>
      </div>
      <p class="hero-desc">Select a resume and job title to begin your AI-powered mock interview.</p>
      <el-form label-width="100px">
        <el-form-item label="Resume">
          <el-select v-model="resumeId" style="width: 100%">
            <el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Job title">
          <el-input v-model="jobTitle" placeholder="e.g. Backend Engineer" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="onStart">
          Start Interview →
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ChatDotRound } from "@element-plus/icons-vue";
import { useInterviewStore } from "@/stores/interview";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";

const router = useRouter();
const interview = useInterviewStore();
const resumeStore = useResumeStore();
const resumes = computed(() => resumeStore.list);
const resumeId = ref<number | null>(null);
const jobTitle = ref("Backend Engineer");
const loading = ref(false);

onMounted(async () => {
  await resumeStore.loadList();
  if (resumeStore.list.length) resumeId.value = resumeStore.list[0].id;
});

async function onStart() {
  if (!resumeId.value) return;
  loading.value = true;
  try {
    const session = await interview.start(resumeId.value, jobTitle.value);
    router.push(`/interview/${session.id}/chat`);
  } catch (e: any) {
    ElMessage.error(e.message || "Failed to start");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.start-page {
  max-width: 560px;
}

.form-card {
  border-radius: var(--app-radius);
  text-align: center;
  padding: 8px;
}

.hero-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  color: #fff;
}

.hero-desc {
  color: #909399;
  margin: 0 0 24px;
  line-height: 1.6;
}

.form-card :deep(.el-form) {
  text-align: left;
}
</style>