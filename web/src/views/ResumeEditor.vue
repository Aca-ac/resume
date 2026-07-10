<template>
  <div class="editor-page">
    <div class="page-header">
      <el-button @click="$router.push('/resumes')">← 返回列表</el-button>
      <h2 class="page-title">{{ isEdit ? "Edit Resume" : "New Resume" }}</h2>
    </div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="form-card animate-fade-up" shadow="never">
          <el-form :model="form" label-width="80px">
            <el-form-item label="Title"><el-input v-model="form.title" /></el-form-item>
            <el-form-item label="Content">
              <el-input v-model="form.content" type="textarea" :rows="16" placeholder="Write your resume content..." />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="onSave">Save</el-button>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <transition name="fade-slide">
          <ResumePreview
              v-if="form.content"
              :title="form.title || 'Preview'"
              :content="form.content"
              class="preview animate-fade-up stagger-1"
          />
        </transition>
      </el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import ResumePreview from "@/components/ResumePreview.vue";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";
const route = useRoute();
const router = useRouter();
const store = useResumeStore();
const saving = ref(false);
const form = reactive({ title: "", content: "" });
const isEdit = computed(() => Boolean(route.params.id));
onMounted(async () => {
  if (isEdit.value) {
    await store.loadOne(Number(route.params.id));
    if (store.current) {
      form.title = store.current.title;
      form.content = store.current.content;
    }
  }
});
async function onSave() {
  saving.value = true;
  try {
    await store.save({ id: isEdit.value ? Number(route.params.id) : undefined, title: form.title, content: form.content });
    ElMessage.success("Saved");
    router.push("/resumes");
  } finally {
    saving.value = false;
  }
}
</script>
<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.form-card {
  border-radius: var(--app-radius);
}
.preview {
  position: sticky;
  top: 24px;
}
</style>
