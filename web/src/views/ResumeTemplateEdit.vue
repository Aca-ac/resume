<template>
  <div class="template-edit-page">
    <section class="hero-section">
      <div class="hero-top">
        <el-button text @click="goBack">← 返回</el-button>
        <div class="hero-actions">
          <el-button
              v-if="templateId && resumeId"
              type="success"
              plain
              @click="goExport"
          >
            去导出
          </el-button>
          <el-button type="primary" :loading="saving" @click="onSave">
            {{ isCreateMode ? '创建简历' : '保存分段' }}
          </el-button>
        </div>
      </div>
      <h1 class="hero-title">📋 模板简历编辑</h1>
      <p class="hero-subtitle">
        {{ isCreateMode ? '填写各分段内容，保存后将自动创建简历。' : '填写各分段内容，导出 Word/PDF 时将填入模板占位符（姓名/手机/邮箱来自个人中心，求职意向见下方）。' }}
      </p>
    </section>

    <section class="content-section" v-loading="loading">
      <el-form label-position="top">
        <el-form-item label="简历标题" required>
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
import { onBeforeUnmount, onMounted, reactive, ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  createResume,
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

// 从路由参数获取 resumeId，支持创建模式（无 id）
const resumeId = ref<number | null>(null);
const templateId = route.query.templateId ? Number(route.query.templateId) : null;

// 判断是否为创建模式
const isCreateMode = computed(() => !resumeId.value);

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
  const idParam = route.params.id;
  if (!idParam) {
    // 创建模式：只初始化空表单，不加载数据
    title.value = "新简历";
    jobIntention.value = "";
    return;
  }

  const id = Number(idParam);
  resumeId.value = id;
  loading.value = true;
  try {
    const [resume, rows] = await Promise.all([
      fetchResume(id),
      fetchResumeDetails(id)
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
  if (!resumeId.value) {
    ElMessage.warning("请先保存简历再上传照片");
    return;
  }
  try {
    const updated = await uploadResumePhoto(resumeId.value, file);
    await loadPhoto(updated);
    ElMessage.success("照片已上传");
  } catch (e: any) {
    ElMessage.error(e.message || "上传失败");
  }
}

async function onRemovePhoto() {
  if (!resumeId.value) {
    ElMessage.warning("请先保存简历再删除照片");
    return;
  }
  try {
    await deleteResumePhoto(resumeId.value);
    revokePhotoUrl();
    photoPreview.value = "";
    ElMessage.success("照片已删除");
  } catch (e: any) {
    ElMessage.error(e.message || "删除失败");
  }
}

async function onSave() {
  // 校验标题
  const trimmedTitle = title.value.trim();
  if (!trimmedTitle) {
    ElMessage.warning("请填写简历标题");
    return;
  }

  saving.value = true;
  try {
    // 如果是创建模式，先创建简历
    if (!resumeId.value) {
      const newResume = await createResume({
        title: trimmedTitle,
        content: "", // 模板编辑模式使用分段，主 content 留空
        jobIntention: jobIntention.value.trim() || undefined
      });
      resumeId.value = newResume.id;
      details.value = [];
      ElMessage.success("简历已创建，正在保存分段...");
    } else {
      // 更新已有简历
      await updateResume(resumeId.value, {
        title: trimmedTitle,
        jobIntention: jobIntention.value.trim()
      });
    }

    // 保存各分段
    let rows = [...details.value];
    for (const sec of RESUME_TEMPLATE_SECTIONS) {
      await saveResumeSection(
          resumeId.value!,
          rows,
          sec.type,
          sec.name,
          sections[sec.type],
          sec.sortOrder
      );
    }
    rows = await fetchResumeDetails(resumeId.value!);
    details.value = rows;
    ElMessage.success(isCreateMode.value ? "简历创建成功！" : "分段已保存");

    // 创建模式完成后，跳转到编辑页（带上 templateId 保持上下文）
    if (isCreateMode.value) {
      router.replace({
        path: `/resumes/${resumeId.value}/template-edit`,
        query: { templateId: String(templateId) }
      });
      // 重新加载数据以获取最新状态
      await loadData();
    }
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
  if (!resumeId.value) {
    ElMessage.warning("请先保存简历");
    return;
  }
  router.push({
    path: "/resume/preview",
    query: { templateId: String(templateId), resumeId: String(resumeId.value) }
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