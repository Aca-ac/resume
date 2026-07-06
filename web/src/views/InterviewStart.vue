<template>
  <div>
    <h2>Start Mock Interview</h2>
    <el-form label-width="100px">
      <el-form-item label="Resume">
        <el-select v-model="resumeId" style="width: 100%">
          <el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Job title"><el-input v-model="jobTitle" /></el-form-item>
      <el-button type="primary" :loading="loading" @click="onStart">Start</el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
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

