<template>
  <div>
    <h2>Optimize Resume</h2>
    <el-form inline>
      <el-form-item label="Target role"><el-input v-model="targetRole" /></el-form-item>
      <el-button type="primary" :loading="loading" @click="onOptimize">Run AI Optimize</el-button>
    </el-form>
    <ResumePreview v-if="original" title="Original" :content="original.content" />
    <ResumePreview v-if="optimized" title="Optimized" :content="optimized.content" class="optimized" />
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
const original = ref<{ content: string } | null>(null);
const optimized = ref<{ content: string } | null>(null);

onMounted(async () => {
  await store.loadOne(Number(route.params.id));
  if (store.current) {
    original.value = { content: store.current.content };
  }
});

async function onOptimize() {
  loading.value = true;
  try {
    optimized.value = await optimizeResume(Number(route.params.id), targetRole.value);
    ElMessage.success("Optimized");
  } catch (e: any) {
    ElMessage.error(e.message || "Optimize failed");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.optimized { margin-top: 16px; }
</style>