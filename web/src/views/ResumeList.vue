<template>
  <div class="list-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">📄 我的简历</h1>
      <p class="hero-subtitle">
        管理你的所有简历文档，支持新建、导入、编辑、优化和导出 PDF。
      </p>
    </section>

    <!-- 内容区域 -->
    <section class="content-section">
      <div class="toolbar">
        <span class="toolbar-title">简历列表</span>
        <div class="toolbar-btns">
          <el-button type="primary" @click="$router.push('/resumes/new')" class="btn-primary">
            <el-icon><Plus /></el-icon> 新建简历
          </el-button>
          <el-button @click="$router.push('/resumes/import')" class="btn-outline">
            <el-icon><Upload /></el-icon> 导入简历
          </el-button>
        </div>
      </div>

      <el-table :data="store.list" v-loading="loading" stripe class="resume-table">
        <el-table-column prop="title" label="简历标题" min-width="180">
          <template #default="{ row }">
            <span class="resume-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'IMPORT' ? 'success' : 'info'">
              {{ row.sourceType === "IMPORT" ? "📥 导入" : "✏️ 新建" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button link class="action-btn" @click="$router.push(`/resumes/${row.id}/edit`)">
              编辑
            </el-button>
            <el-button link type="primary" class="action-btn" @click="$router.push(`/resumes/${row.id}/optimize`)">
              ✨ 优化
            </el-button>
            <el-button link class="action-btn" @click="onExport(row.id)">
              📄 PDF
            </el-button>
            <el-button link type="danger" class="action-btn" @click="onDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useResumeStore } from "@/stores/resume";
import { exportResumePdf } from "@/api/resume";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Upload } from "@element-plus/icons-vue";

const router = useRouter();
const store = useResumeStore();
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  await store.loadList();
  loading.value = false;
});

async function onDelete(id: number) {
  await ElMessageBox.confirm("确定要删除这份简历吗？", "确认删除", {
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
    type: "warning"
  });
  await store.remove(id);
  ElMessage.success("已删除");
}

async function onExport(id: number) {
  await exportResumePdf(id);
}
</script>

<style scoped>
.list-page {
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
  background-color: #fff;
  padding: 24px 28px;
  border-radius: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-title {
  font-size: 18px;
  font-weight: 600;
  color: #3d6b57;
}

.toolbar-btns {
  display: flex;
  gap: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 8px;
}
.btn-primary:hover {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}

.btn-outline {
  border: 1px solid rgba(100, 163, 134, 0.3);
  color: #64A386;
  background: transparent;
  border-radius: 8px;
}
.btn-outline:hover {
  background: rgba(100, 163, 134, 0.08);
  border-color: #64A386;
}

.resume-table {
  border-radius: 12px;
  overflow: hidden;
}

.resume-table :deep(.el-table__header th) {
  background: #f0f7f4;
  color: #2c4d3d;
  font-weight: 600;
}

.resume-table :deep(.el-table__row) {
  transition: background-color 0.2s ease;
}
.resume-table :deep(.el-table__row:hover) {
  background-color: #f8fbf9;
}

.resume-title {
  font-weight: 500;
  color: #2c4d3d;
}

.action-btn {
  font-size: 13px;
  padding: 4px 8px;
}
.action-btn:hover {
  text-decoration: underline;

}


.blank-area {
  width: 100%;
  min-height: 80px;

}
</style>