<template>
  <div>
    <h2>Optimize Resume</h2>
    <el-form inline>
      <el-form-item label="Target role"><el-input v-model="targetRole" /></el-form-item>
      <el-button type="primary" :loading="loading" @click="onOptimize">Run AI Optimize</el-button>
    </el-form>
    <ResumePreview v-if="result" title="Optimized" :content="result.content" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { optimizeResume } from "@/api/resume";
import ResumePreview from "@/components/ResumePreview.vue";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";

const route = useRoute();
const store = useResumeStore();
const targetRole = ref("Software Engineer");
const loading = ref(false);
const result = ref<{ content: string } | null>(null);

onMounted(async () => {
  await store.loadOne(Number(route.params.id));
  if (store.current) result.value = store.current;
});

async function onOptimize() {
  loading.value = true;
  try {
    result.value = await optimizeResume(Number(route.params.id), targetRole.value);
    ElMessage.success("Optimized");
  } catch (e: any) {
    ElMessage.error(e.message || "Optimize failed");
  } finally {
    loading.value = false;
  }
}
</script>
