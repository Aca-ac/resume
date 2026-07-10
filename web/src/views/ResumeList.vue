<template>
  <div class="list-page">
    <div class="toolbar animate-fade-up">
      <h2 class="page-title">My Resumes</h2>
      <div class="toolbar-btns">
        <el-button type="primary" @click="$router.push('/resumes/new')">
          + New Resume
        </el-button>
        <el-button @click="$router.push('/resumes/import')">Import Resume</el-button>
      </div>
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
import { useResumeStore } from "@/stores/resume";
import { exportResumePdf } from "@/api/resume";
import { ElMessage, ElMessageBox } from "element-plus";
const store = useResumeStore();
const loading = ref(false);
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
.toolbar-btns {
  display: flex;
  gap: 12px;
}
.table-card {
  border-radius: var(--app-radius);
}
.resume-table :deep(.el-table__row) {
  transition: background-color 0.2s ease;
}
</style>
