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

        <div class="export-box">
          <el-select v-model="resumeId" placeholder="选择要导出的简历" filterable style="width: 100%">
            <el-option
              v-for="r in resumes"


              :key="r.id"
              :label="r.title"
              :value="r.id"
            />
          </el-select>
          <div class="export-actions">
            <el-button type="primary" :loading="exporting === 'word'" @click="doExport('word')">
              导出 Word
            </el-button>
            <el-button type="success" :loading="exporting === 'pdf'" @click="doExport('pdf')">
              导出 PDF
            </el-button>
          </div>
        </div>
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
import { exportResumeByTemplate, fetchResumes } from "@/api/resume";
import type { ResumeItem } from "@/stores/resume";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const exporting = ref<"word" | "pdf" | "">("");
const template = ref<TemplateVO | null>(null);
const resumes = ref<ResumeItem[]>([]);
const resumeId = ref<number | undefined>();

async function load() {
  const id = Number(route.query.templateId || route.query.id);
  if (!id) return;
  loading.value = true;
  try {
    const [t, list] = await Promise.all([fetchTemplate(id), fetchResumes()]);
    template.value = t;
    resumes.value = list;
    if (list.length && resumeId.value == null) {
      resumeId.value = list[0].id;
    }
  } catch (e: any) {
    ElMessage.error(e.message || "加载详情失败");
  } finally {
    loading.value = false;
  }
}

async function doExport(format: "word" | "pdf") {
  if (!template.value) return;
  if (!resumeId.value) {
    ElMessage.warning("请先选择简历");
    return;
  }
  exporting.value = format;
  try {
    await exportResumeByTemplate(resumeId.value, template.value.id, format);
    ElMessage.success(format === "pdf" ? "PDF 导出成功" : "Word 导出成功");
  } catch (e: any) {
    ElMessage.error(e.message || "导出失败");
  } finally {
    exporting.value = "";
  }
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
.export-box {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.export-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
@media (max-width: 800px) {
  .body {
    grid-template-columns: 1fr;
  }
}
</style>
