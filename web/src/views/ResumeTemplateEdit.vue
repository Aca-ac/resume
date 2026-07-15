<template>
  <div class="template-edit-page">
    <section class="hero-section">
      <div class="hero-top">
        <el-button text @click="goBack">← 返回</el-button>
        <div class="hero-actions">
          <el-button
            v-if="templateId"
            type="success"
            plain
            @click="goExport"
          >
            去导出
          </el-button>
          <el-button type="primary" :loading="saving" @click="onSave">
            保存分段
          </el-button>
        </div>
      </div>
      <h1 class="hero-title">📋 模板简历编辑</h1>
      <p class="hero-subtitle">
        填写各分段内容，导出 Word/PDF 时将填入模板占位符（姓名/手机/邮箱来自个人中心，求职意向见下方）。
      </p>
    </section>

    <section class="content-section" v-loading="loading">
      <el-form label-position="top">
        <el-form-item label="简历标题">
          <el-input v-model="title" placeholder="导出文件名使用此标题" />
        </el-form-item>

        <el-form-item label="求职意向（模板 {{jobIntention}}）">
          <el-input
            v-model="jobIntention"
            placeholder="如：Java后端开发工程师 | 北京 | 15-20K"
            clearable
          />
        </el-form-item>

        <el-form-item label="一寸照（可选，模板 {{@photo}}）">
          <div class="photo-row">
            <div class="photo-preview">
              <img v-if="photoPreview" :src="photoPreview" alt="一寸照" />
              <span v-else class="photo-placeholder">暂无照片</span>
            </div>
            <div class="photo-actions">
              <el-upload
                :show-file-list="false"
                accept="image/jpeg,image/png,image/jpg"
                :before-upload="beforePhotoUpload"
              >
                <el-button type="primary" plain>上传照片</el-button>
              </el-upload>
              <el-button v-if="photoPreview" text type="danger" @click="onRemovePhoto">
                删除照片
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-divider />

        <el-form-item
          v-for="sec in RESUME_TEMPLATE_SECTIONS"
          :key="sec.type"
          :label="sec.name"
        >
          <el-input
            v-model="sections[sec.type]"
            type="textarea"
            :rows="5"
            :placeholder="sec.placeholder"
          />
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  deleteResumePhoto,
  fetchResume,
  fetchResumeDetails,
  saveResumeSection,
  updateResume,
  uploadResumePhoto
} from "@/api/resume";
import request from "@/utils/request";
import type { ResumeDetailItem } from "@/types/resume";
import { RESUME_TEMPLATE_SECTIONS, type ResumeSectionType } from "@/types/template";

const route = useRoute();
const router = useRouter();

const resumeId = Number(route.params.id);
const templateId = route.query.templateId ? Number(route.query.templateId) : null;

const loading = ref(false);
const saving = ref(false);
const title = ref("");
const jobIntention = ref("");
const sections = reactive<Record<ResumeSectionType, string>>({
  SUMMARY: "",
  EDUCATION: "",
  WORK_EXPERIENCE: "",
  PROJECT: "",
  SKILL: ""
});
const details = ref<ResumeDetailItem[]>([]);
const photoPreview = ref("");
let photoObjectUrl: string | null = null;

function revokePhotoUrl() {
  if (photoObjectUrl) {
    URL.revokeObjectURL(photoObjectUrl);
    photoObjectUrl = null;
  }
}

async function loadPhoto(resume: { photoUrl?: string }) {
  revokePhotoUrl();
  photoPreview.value = "";
  if (!resume.photoUrl) return;
  try {
    const blob = (await request.get(resume.photoUrl.replace(/^\/api/, ""), {
      responseType: "blob"
    })) as Blob;
    photoObjectUrl = URL.createObjectURL(blob);
    photoPreview.value = photoObjectUrl;
  } catch {
    photoPreview.value = "";
  }
}

async function loadData() {
  if (!resumeId) {
    ElMessage.error("无效的简历 ID");
    router.push("/resumes");
    return;
  }
  loading.value = true;
  try {
    const [resume, rows] = await Promise.all([
      fetchResume(resumeId),
      fetchResumeDetails(resumeId)
    ]);
    title.value = resume.title;
    jobIntention.value = resume.jobIntention ?? "";
    details.value = rows;
    for (const sec of RESUME_TEMPLATE_SECTIONS) {
      const row = rows.find((d) => d.sectionType === sec.type);
      sections[sec.type] = row?.content ?? "";
    }
    await loadPhoto(resume);
  } catch (e: any) {
    ElMessage.error(e.message || "加载失败");
    router.push("/resumes");
  } finally {
    loading.value = false;
  }
}

function beforePhotoUpload(file: File) {
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning("照片不能超过 2MB");
    return false;
  }
  uploadPhoto(file);
  return false;
}

async function uploadPhoto(file: File) {
  try {
    const updated = await uploadResumePhoto(resumeId, file);
    await loadPhoto(updated);
    ElMessage.success("照片已上传");
  } catch (e: any) {
    ElMessage.error(e.message || "上传失败");
  }
}

async function onRemovePhoto() {
  try {
    await deleteResumePhoto(resumeId);
    revokePhotoUrl();
    photoPreview.value = "";
    ElMessage.success("照片已删除");
  } catch (e: any) {
    ElMessage.error(e.message || "删除失败");
  }
}

async function onSave() {
  saving.value = true;
  try {
    await updateResume(resumeId, {
      title: title.value.trim() || "未命名简历",
      jobIntention: jobIntention.value.trim()
    });
    let rows = [...details.value];
    for (const sec of RESUME_TEMPLATE_SECTIONS) {
      await saveResumeSection(
        resumeId,
        rows,
        sec.type,
        sec.name,
        sections[sec.type],
        sec.sortOrder
      );
    }
    rows = await fetchResumeDetails(resumeId);
    details.value = rows;
    ElMessage.success("分段已保存，可前往模板预览导出");
  } catch (e: any) {
    ElMessage.error(e.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.back();
}

function goExport() {
  router.push({
    path: "/resume/preview",
    query: { templateId: String(templateId), resumeId: String(resumeId) }
  });
}

onMounted(loadData);
onBeforeUnmount(revokePhotoUrl);
</script>

<style scoped>
.template-edit-page {
  width: 100%;
  min-height: 100%;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #cde2e8 0%, #bcddbe 100%);
  border-radius: 12px;
}

.hero-section {
  margin-bottom: 24px;
  padding: 20px 28px;
  background: rgba(251, 252, 205, 0.9);
  border-radius: 16px;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 24px;
  color: #4a7a64;
}

.hero-subtitle {
  margin: 0;
  color: #5a7a6a;
  font-size: 14px;
  line-height: 1.6;
}

.content-section {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
}

.photo-row {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.photo-preview {
  width: 120px;
  height: 160px;
  border: 1px dashed #c8d8d2;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.photo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-placeholder {
  font-size: 12px;
  color: #999;
}

.photo-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
