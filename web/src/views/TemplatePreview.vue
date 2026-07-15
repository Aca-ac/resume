<template>
  <div class="template-preview" v-loading="loading">
    <section class="hero">
      <el-button text @click="router.push('/templates')">← 返回模板广场</el-button>
      <h1>{{ template?.name || "模板预览与导出" }}</h1>
      <p v-if="template">
        {{ template.category }} · {{ template.applicableScene || "通用场景" }}
      </p>
    </section>

    <el-row v-if="template" :gutter="24" class="body">
      <el-col :xs="24" :md="10">
        <div class="preview-panel">
          <img class="preview" :src="previewSrc(template.previewUrl)" :alt="template.name" />
        </div>
        <div class="skin-panel">
          <div class="panel-label">一键换肤</div>
          <div class="skin-list">
            <div
              v-for="t in allTemplates"
              :key="t.id"
              class="skin-item"
              :class="{ active: t.id === template.id }"
              @click="switchTemplate(t.id)"
            >
              <img :src="previewSrc(t.previewUrl)" :alt="t.name" />
              <span>{{ t.name }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :md="14">
        <div class="export-panel">
          <h3>选择简历并导出</h3>
          <p class="hint">
            导出时将使用模板占位符填充内容。请先
            <el-link type="primary" @click="goEditSections">完善简历分段</el-link>
            ，姓名/手机/邮箱来自个人中心。
          </p>

          <el-form label-position="top">
            <el-form-item label="选择简历">
              <el-select
                v-model="resumeId"
                placeholder="请选择要导出的简历"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="r in resumes"
                  :key="r.id"
                  :label="r.title"
                  :value="r.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-if="!resumes.length">
              <el-alert
                type="warning"
                :closable="false"
                title="还没有简历，请先创建或导入后再导出。"
                show-icon
              />
              <el-button type="primary" plain class="mt-8" @click="router.push('/resumes/new')">
                新建简历
              </el-button>
            </el-form-item>
          </el-form>

          <div class="export-actions">
            <el-button
              type="primary"
              :loading="exporting === 'word'"
              :disabled="!resumeId"
              @click="doExport('word')"
            >
              导出 Word
            </el-button>
            <el-button
              type="success"
              :loading="exporting === 'pdf'"
              :disabled="!resumeId"
              @click="doExport('pdf')"
            >
              导出 PDF
            </el-button>
            <el-button
              v-if="resumeId"
              plain
              @click="goEditSections"
            >
              编辑分段内容
            </el-button>
          </div>

          <el-alert
            v-if="lastExport"
            class="export-result"
            type="success"
            :closable="false"
            show-icon
          >
            <template #title>
              已生成 {{ lastExport.filename }}
            </template>
            <p v-if="lastExport.expiresAt" class="expire-tip">
              下载链接约 24 小时内有效（过期时间：{{ lastExport.expiresAt }}）
            </p>
          </el-alert>

          <el-alert
            class="pdf-tip"
            type="info"
            :closable="false"
            show-icon
            title="PDF 导出可能需 10~60 秒，请耐心等待；失败时可改导 Word。"
          />
        </div>
      </el-col>
    </el-row>

    <el-empty v-else-if="!loading" description="未找到模板，请从模板广场进入" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { fetchTemplate, fetchTemplates, previewSrc, type TemplateVO } from "@/api/template";
import { exportResumeByTemplate, fetchResumes } from "@/api/resume";
import type { ExportResultVO } from "@/types/template";
import type { ResumeItem } from "@/stores/resume";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const exporting = ref<"word" | "pdf" | "">("");
const template = ref<TemplateVO | null>(null);
const allTemplates = ref<TemplateVO[]>([]);
const resumes = ref<ResumeItem[]>([]);
const resumeId = ref<number | undefined>();
const lastExport = ref<ExportResultVO | null>(null);

const templateId = ref(Number(route.query.templateId || route.query.id));

async function loadTemplatesList() {
  const data = await fetchTemplates(undefined, 1, 20);
  allTemplates.value = data.records ?? [];
}

async function loadTemplate(id: number) {
  template.value = await fetchTemplate(id);
}

async function loadResumes() {
  const list = await fetchResumes();
  resumes.value = list;
  const queryResumeId = Number(route.query.resumeId);
  if (queryResumeId && list.some((r) => r.id === queryResumeId)) {
    resumeId.value = queryResumeId;
  } else if (list.length && resumeId.value == null) {
    resumeId.value = list[0].id;
  }
}

async function load() {
  loading.value = true;
  try {
    if (!templateId.value) {
      const data = await fetchTemplates(undefined, 1, 20);
      allTemplates.value = data.records ?? [];
      if (allTemplates.value.length) {
        templateId.value = allTemplates.value[0].id;
      } else {
        ElMessage.warning("暂无可用模板");
        router.push("/templates");
        return;
      }
    } else {
      await loadTemplatesList();
    }
    await Promise.all([loadTemplate(templateId.value), loadResumes()]);
  } catch (e: any) {
    ElMessage.error(e.message || "加载失败");
    template.value = null;
  } finally {
    loading.value = false;
  }
}

function switchTemplate(id: number) {
  if (id === templateId.value) return;
  templateId.value = id;
  router.replace({
    path: "/resume/preview",
    query: {
      templateId: String(id),
      ...(resumeId.value ? { resumeId: String(resumeId.value) } : {})
    }
  });
  loadTemplate(id).catch((e: any) => ElMessage.error(e.message || "切换模板失败"));
}

async function doExport(format: "word" | "pdf") {
  if (!template.value || !resumeId.value) return;
  exporting.value = format;
  lastExport.value = null;
  const loadingMsg = ElMessage({
    message: format === "pdf" ? "正在生成 PDF，请稍候…" : "正在生成 Word…",
    type: "info",
    duration: 0
  });
  try {
    const job = await exportResumeByTemplate(resumeId.value, template.value.id, format);
    lastExport.value = job;
    ElMessage.success(format === "pdf" ? "PDF 导出成功" : "Word 导出成功");
  } catch (e: any) {
    ElMessage.error(e.message || "导出失败");
  } finally {
    loadingMsg.close();
    exporting.value = "";
  }
}

function goEditSections() {
  if (!resumeId.value) {
    ElMessage.warning("请先选择简历");
    return;
  }
  router.push({
    path: `/resumes/${resumeId.value}/template-edit`,
    query: template.value ? { templateId: String(template.value.id) } : {}
  });
}

watch(
  () => route.query.templateId,
  (val) => {
    const id = Number(val);
    if (id && id !== templateId.value) {
      templateId.value = id;
      loadTemplate(id).catch(() => undefined);
    }
  }
);

onMounted(load);
</script>

<style scoped>
.template-preview {
  padding: 24px 32px 48px;
}
.hero h1 {
  margin: 8px 0;
  font-size: 26px;
  color: #4a7a64;
}
.hero p {
  margin: 0 0 20px;
  color: #666;
}
.preview-panel {
  margin-bottom: 16px;
}
.preview {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
}
.skin-panel {
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
}
.panel-label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #555;
}
.skin-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.skin-item {
  width: 72px;
  cursor: pointer;
  text-align: center;
  font-size: 11px;
  color: #666;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 4px;
}
.skin-item.active {
  border-color: #64a386;
  color: #64a386;
}
.skin-item img {
  width: 64px;
  height: 84px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
  margin: 0 auto 4px;
}
.export-panel {
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
}
.export-panel h3 {
  margin: 0 0 8px;
}
.hint {
  margin: 0 0 16px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}
.export-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.export-result {
  margin-top: 16px;
}
.expire-tip {
  margin: 4px 0 0;
  font-size: 13px;
}
.pdf-tip {
  margin-top: 12px;
}
.mt-8 {
  margin-top: 8px;
}
</style>
