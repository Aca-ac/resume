<template>
  <div class="list-page">
    <div class="toolbar animate-fade-up">
      <h2 class="page-title">My Resumes</h2>
      <el-button type="primary" @click="$router.push('/resumes/new')">
        + New Resume
      </el-button>
      <el-upload :show-file-list="false" :http-request="onImport" accept=".doc,.docx,.pdf,.jpg,.jpeg,.png">
        <el-button :loading="importing">Import</el-button>
      </el-upload>
    </div>
    <el-card class="table-card animate-fade-up stagger-1" shadow="never">
      <el-table :data="store.list" v-loading="loading" stripe class="resume-table">
        <el-table-column prop="title" label="Title" />
        <el-table-column label="Source" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'IMPORT' ? 'success' : 'info'">
              {{ row.sourceType === "IMPORT" ? "导入" : "新建" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated" />
        <el-table-column label="Actions" width="360">
          <template #default="{ row }">
            <el-button link @click="$router.push(`/resumes/${row.id}/edit`)">Edit</el-button>
            <el-button link type="primary" @click="$router.push(`/resumes/${row.id}/optimize`)">Optimize</el-button>
            <el-button link @click="onExport(row.id)">PDF</el-button>
            <el-button link type="danger" @click="onDelete(row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useResumeStore } from "@/stores/resume";
import { exportResumePdf, importResume, runResumeOcr } from "@/api/resume";
import { ElMessage, ElMessageBox } from "element-plus";

const router = useRouter();
const store = useResumeStore();
const loading = ref(false);
const importing = ref(false);

onMounted(async () => {
  loading.value = true;
  await store.loadList();
  loading.value = false;
});

async function onDelete(id: number) {
  await ElMessageBox.confirm("Delete this resume?", "Confirm");
  await store.remove(id);
  ElMessage.success("Deleted");
}

async function onExport(id: number) {
  await exportResumePdf(id);
}

async function onImport(options: { file: File }) {
  importing.value = true;
  try {
    const result = await importResume(options.file);
    await store.loadList();
    const isImage = ["JPG", "JPEG", "PNG"].includes(result.fileType);
    const ocrFailed = result.parseStatus === "OCR_FALLBACK" || result.content.startsWith("【");

    if (isImage && ocrFailed && result.fileId) {
      ElMessage.warning("图片 OCR 未成功，正在重试…");
      try {
        const retry = await runResumeOcr(result.fileId);
        if (retry.ocrText && !retry.ocrText.startsWith("【")) {
          ElMessage.success("图片 OCR 识别完成");
          await store.loadList();
          router.push(`/resumes/${result.resumeId}/edit`);
          return;
        }
      } catch {
        // fall through to edit page with placeholder content
      }
      ElMessage.error("图片 OCR 识别失败，请检查 DASHSCOPE_API_KEY 配置后手动编辑");
    } else if (isImage) {
      ElMessage.success("图片 OCR 识别完成");
    } else {
      ElMessage.success(`Imported: ${result.title}`);
    }
    router.push(`/resumes/${result.resumeId}/edit`);
  } catch {
    ElMessage.error("Import failed");
  } finally {
    importing.value = false;
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar .page-title {
  margin: 0;
}

.table-card {
  border-radius: var(--app-radius);
}

.resume-table :deep(.el-table__row) {
  transition: background-color 0.2s ease;
}
</style>
