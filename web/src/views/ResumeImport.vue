<template>
  <div class="import-page">
    <div class="page-header">
      <el-button @click="$router.push('/resumes')">← 返回列表</el-button>
      <h2 class="page-title">Import Resume</h2>
    </div>
    <el-card class="import-card animate-fade-up" shadow="never">
      <el-upload
          :show-file-list="false"
          :http-request="onImport"
          accept=".doc,.docx,.pdf,.jpg,.jpeg,.png"
      >
        <el-button size="large" :loading="importing">点击上传文件导入简历</el-button>
      </el-upload>
      <div class="tip-text">支持格式：doc、docx、pdf、jpg、jpeg、png</div>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useResumeStore } from "@/stores/resume";
import { importResume, runResumeOcr } from "@/api/resume";
import { ElMessage } from "element-plus";
const router = useRouter();
const store = useResumeStore();
const importing = ref(false);

async function onImport(options: { file: File }) {
  importing.value = true;
  try {
    const result = await importResume(options.file);
    await store.loadList();
    ElMessage.success(`Imported: ${result.title}`);
    const isImage = ["JPG", "JPEG", "PNG"].includes(result.fileType);
    if (isImage && result.content.startsWith("【")) {
      ElMessage.warning("图片 OCR 未成功，可稍后重试识别");
    } else if (isImage) {
      ElMessage.success("图片 OCR 识别完成");
    }
    if (isImage && result.fileId) {
      try {
        await runResumeOcr(result.fileId);
        await store.loadList();
      } catch {
        // import 已尝试 OCR，此处静默
      }
    }
    // 导入成功自动返回列表
    router.push("/resumes");
  } catch {
    ElMessage.error("Import failed");
  } finally {
    importing.value = false;
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
.import-card {
  border-radius: var(--app-radius);
  padding: 40px;
  text-align: center;
}
.tip-text {
  margin-top: 12px;
  color: #999;
  font-size: 14px;
}
</style>
