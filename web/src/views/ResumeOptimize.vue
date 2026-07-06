<template>
  <div class="optimize-page">
    <h2 class="page-title">AI Resume Optimize</h2>
    <el-card class="form-card animate-fade-up" shadow="never">
      <el-form inline>
        <el-form-item label="Target role">
          <el-input v-model="targetRole" placeholder="e.g. Software Engineer" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onOptimize">
          <span v-if="!loading">✨ Run AI Optimize</span>
          <span v-else>Optimizing...</span>
        </el-button>
      </el-form>
    </el-card>

    <div class="previews">
      <ResumePreview v-if="original" title="Original" :content="original.content" class="preview-original" />
      <transition name="scale-fade">
        <ResumePreview
          v-if="optimized"
          title="Optimized"
          :content="optimized.content"
          class="preview-optimized"
          highlight
        />
      </transition>
    </div>
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
    ElMessage.success("Optimized successfully");
  } catch (e: any) {
    ElMessage.error(e.message || "Optimize failed");
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

.previews {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-original {
  animation: fadeUp 0.4s ease both;
}

.scale-fade-enter-active {
  transition: all 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.scale-fade-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.97);
}
</style>