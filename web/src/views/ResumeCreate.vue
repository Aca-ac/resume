<template>
  <div class="editor-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">✏️ 新建简历</h1>
      <p class="hero-subtitle">填写简历标题和内容，创建一份全新的简历文档。</p>
    </section>

    <!-- 编辑区域 -->
    <section class="content-section">
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="form-card">
            <div class="card-header">
              <span class="card-title">📝 编辑内容</span>
              <span class="card-subtitle">填写简历信息</span>
            </div>
            <el-form :model="form" label-width="80px" label-position="top">
              <el-form-item label="简历标题">
                <el-input v-model="form.title" placeholder="请输入简历标题" />
              </el-form-item>
              <el-form-item label="简历内容">
                <el-input
                    v-model="form.content"
                    type="textarea"
                    :rows="16"
                    placeholder="请填写你的简历内容..."
                />
              </el-form-item>
              <div class="form-actions">
                <el-button @click="handleBack" class="btn-cancel">取消</el-button>
                <el-button type="primary" :loading="saving" @click="onSave" class="btn-save">
                  <el-icon><Check /></el-icon> 保存简历
                </el-button>
              </div>
            </el-form>
          </div>
        </el-col>
        <el-col :span="12">
          <transition name="fade-slide">
            <div v-if="form.content" class="preview-card">
              <div class="card-header">
                <span class="card-title">👁️ 实时预览</span>
                <span class="card-subtitle">简历效果</span>
              </div>
              <ResumePreview
                  :title="form.title || '预览'"
                  :content="form.content"
                  class="preview"
              />
            </div>
            <div v-else class="preview-placeholder">
              <el-icon :size="48"><Document /></el-icon>
              <p>输入内容后，此处将显示实时预览</p>
            </div>
          </transition>
        </el-col>
      </el-row>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import ResumePreview from "@/components/ResumePreview.vue";
import { useResumeStore } from "@/stores/resume";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Document,
  Check
} from "@element-plus/icons-vue";

const router = useRouter();
const store = useResumeStore();
const authStore = useAuthStore();
const saving = ref(false);

const originForm = reactive({ title: "", content: "" });
const form = reactive({ title: "", content: "" });

const isModified = ref(false);
watch([() => form.title, () => form.content], () => {
  isModified.value = form.title !== originForm.title || form.content !== originForm.content;
});

// ===== 页面逻辑 =====
async function onSave() {
  if (!form.title?.trim()) {
    ElMessage.warning("请先填写简历标题");
    return;
  }
  saving.value = true;
  try {
    await store.save({ id: undefined, title: form.title, content: form.content });
    ElMessage.success("保存成功");
    originForm.title = form.title;
    originForm.content = form.content;
    isModified.value = false;
    router.push("/resumes");
  } finally {
    saving.value = false;
  }
}

const handleBack = async () => {
  if (!isModified.value) {
    router.push("/resumes");
    return;
  }

  ElMessageBox.confirm(
      "当前简历内容未保存，是否保存更改？",
      "提示",
      {
        confirmButtonText: "保存",
        cancelButtonText: "放弃",
        type: "warning",
        closeOnClickModal: false
      }
  )
      .then(async () => {
        await onSave();
      })
      .catch(() => {
        router.push("/resumes");
      });
};
</script>

<style scoped>
.editor-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
}

/* ========== 标语 ========== */
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

/* ========== 内容区域 ========== */
.content-section {
  background-color: #fff;
  padding: 28px 32px;
  border-radius: 16px;
}

.form-card {
  width: 100%;
}

.preview-card {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: #f8fbf9;
  border-radius: 12px;
  border: 2px dashed #dce8e2;
  color: #a0bcae;
}
.preview-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 12px;
}
.preview-placeholder p {
  margin: 0;
  font-size: 14px;
}

.card-header {
  margin-bottom: 20px;
}

.card-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #3d6b57;
}

.card-subtitle {
  display: block;
  font-size: 13px;
  color: #8aa89a;
  margin-top: 2px;
}

.el-form-item :deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c4d3d;
  padding-bottom: 4px;
}

.el-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  transition: all 0.3s ease;
}
.el-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
.el-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.30);
}

.el-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  font-family: inherit;
  line-height: 1.7;
  transition: all 0.3s ease;
}
.el-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn-cancel {
  border: 1px solid rgba(100, 163, 134, 0.3);
  color: #64A386;
  background: transparent;
  border-radius: 8px;
  padding: 10px 28px;
}
.btn-cancel:hover {
  background: rgba(100, 163, 134, 0.08);
  border-color: #64A386;
}

.btn-save {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 8px;
  padding: 10px 28px;
}
.btn-save:hover {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}

.preview {
  background: #fafdfb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8f0ec;
}

.blank-area {
  width: 100%;
  min-height: 40px;
}

.fade-slide-enter-active {
  transition: all 0.4s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
</style>