<template>
  <div>
    <h2>{{ isEdit ? "Edit Resume" : "New Resume" }}</h2>
    <el-form :model="form" label-width="80px">
      <el-form-item label="Title"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="Content"><el-input v-model="form.content" type="textarea" :rows="16" /></el-form-item>
      <el-button type="primary" :loading="saving" @click="onSave">Save</el-button>
    </el-form>
    <ResumePreview v-if="form.content" class="preview" :title="form.title || 'Preview'" :content="form.content" />
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
.preview { margin-top: 16px; }
</style>
