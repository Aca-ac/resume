<template>
  <div class="template-preview" v-loading="loading">
    <section class="hero">
      <el-button text @click="router.push('/templates')">← 返回模板广场</el-button>
      <h1>{{ template?.name || "模板预览" }}</h1>
      <p>{{ template?.category }} · {{ template?.applicableScene || "通用场景" }}</p>
    </section>

    <section v-if="template" class="body">
      <img class="preview" :src="previewSrc(template.previewUrl)" :alt="template.name" />
      <div class="info">
        <p><strong>模板 ID：</strong>{{ template.id }}</p>
        <p><strong>预览路径：</strong>{{ template.previewUrl }}</p>
        <p><strong>模板文件：</strong>{{ template.templatePath }}</p>
        <el-button type="primary" @click="copyId">复制 templateId</el-button>
      </div>
    </section>
    <el-empty v-else-if="!loading" description="未找到模板，请从模板广场进入" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { fetchTemplate, previewSrc, type TemplateVO } from "@/api/template";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const template = ref<TemplateVO | null>(null);

async function load() {
  const id = Number(route.query.templateId || route.query.id);
  if (!id) return;
  loading.value = true;
  try {
    template.value = await fetchTemplate(id);
  } catch (e: any) {
    ElMessage.error(e.message || "加载详情失败");
  } finally {
    loading.value = false;
  }
}

async function copyId() {
  if (!template.value) return;
  await navigator.clipboard.writeText(String(template.value.id));
  ElMessage.success("已复制 templateId=" + template.value.id);
}

onMounted(load);
</script>

<style scoped>
.template-preview {
  padding: 24px 32px 48px;
}
.hero h1 {
  margin: 8px 0;
  font-size: 26px;
}
.hero p {
  margin: 0 0 20px;
  color: #666;
}
.body {
  display: grid;
  grid-template-columns: minmax(280px, 420px) 1fr;
  gap: 28px;
  align-items: start;
}
.preview {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
}
.info p {
  margin: 0 0 10px;
  color: #444;
}
@media (max-width: 800px) {
  .body {
    grid-template-columns: 1fr;
  }
}
</style>
