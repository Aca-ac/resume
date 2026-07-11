<template>
  <div class="import-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">📥 导入简历</h1>
      <p class="hero-subtitle">
        支持上传 Word、PDF 或图片格式的简历文件，系统将自动提取内容。
      </p>
    </section>

    <!-- 导入区域 -->
    <section class="content-section">
      <div class="import-card">
        <div class="upload-icon-wrapper">
          <el-icon :size="64" class="upload-icon"><UploadFilled /></el-icon>
        </div>
        <h3 class="upload-title">上传简历文件</h3>
        <p class="upload-desc">拖拽或点击上传，支持 doc、docx、pdf、jpg、jpeg、png</p>

        <el-upload
            :show-file-list="false"
            :http-request="onImport"
            accept=".doc,.docx,.pdf,.jpg,.jpeg,.png"
            class="upload-area"
        >
          <el-button size="large" :loading="importing" class="btn-upload">
            <el-icon v-if="!importing"><Upload /></el-icon>
            {{ importing ? '正在导入...' : '选择文件上传' }}
          </el-button>
        </el-upload>

        <div class="format-tips">
          <span class="tip-item">📄 Word 文档</span>
          <span class="tip-item">📑 PDF 文件</span>
          <span class="tip-item">🖼️ 图片 (OCR识别)</span>
        </div>
      </div>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useResumeStore } from "@/stores/resume";
import { importResume, runResumeOcr } from "@/api/resume";
import { ElMessage } from "element-plus";
import { Upload, UploadFilled } from "@element-plus/icons-vue";

const router = useRouter();
const store = useResumeStore();
const importing = ref(false);

// ===== 导入逻辑 =====
async function onImport(options: { file: File }) {
  importing.value = true;
  try {
    const result = await importResume(options.file);
    await store.loadList();
    ElMessage.success(`导入成功: ${result.title}`);
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
    router.push("/resumes");
  } catch {
    ElMessage.error("导入失败，请检查文件格式");
  } finally {
    importing.value = false;
  }
}
</script>

<style scoped>
.import-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
}

.hero-section {
  text-align: center;
  margin-bottom: 36px;
  padding: 28px 40px;
  background: #FBFCCD;
  border-radius: 16px;
}

.hero-title {
  margin: 0 0 12px;
  font-size: 32px;
  color: #64A386;
}

.hero-subtitle {
  margin: 0;
  color: #4f6b5d;
  font-size: 16px;
  line-height: 1.7;
}

.content-section {
  max-width: 600px;
  margin: 0 auto;
}

.import-card {
  background-color: #fff;
  padding: 48px 40px;
  border-radius: 16px;
  text-align: center;
}

.upload-icon-wrapper {
  width: 96px;
  height: 96px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: #f0f7f4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  color: #64A386;
}

.upload-title {
  font-size: 20px;
  font-weight: 600;
  color: #3d6b57;
  margin: 0 0 8px;
}

.upload-desc {
  color: #8aa89a;
  font-size: 14px;
  margin: 0 0 28px;
}

.upload-area {
  display: block;
}

.btn-upload {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 10px;
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 500;
  min-width: 200px;
}
.btn-upload:hover {
  opacity: 0.92;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(100, 163, 134, 0.25);
}

.format-tips {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.tip-item {
  font-size: 13px;
  color: #a0bcae;
  background: #f8fbf9;
  padding: 4px 16px;
  border-radius: 20px;

}


.blank-area {
  width: 100%;
  min-height: 40px;
}
</style>