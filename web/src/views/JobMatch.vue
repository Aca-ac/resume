<template>
  <div class="match-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">🎯 职位匹配分析</h1>
      <p class="hero-subtitle">选择简历并粘贴职位描述，智能分析你的匹配程度。</p>
    </section>

    <!-- 内容区域 -->
    <section class="content-section">
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="form-card">
            <div class="card-header">
              <span class="card-title">📋 匹配设置</span>
              <span class="card-subtitle">选择简历并输入职位描述</span>
            </div>
            <el-form label-width="80px" label-position="top">
              <el-form-item label="选择简历">
                <el-select v-model="resumeId" placeholder="请选择简历" style="width: 100%">
                  <el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="职位描述">
                <el-input
                    v-model="jdText"
                    type="textarea"
                    :rows="12"
                    placeholder="请粘贴职位描述（JD）到这里..."
                />
              </el-form-item>
              <div class="form-actions">
                <el-button
                    type="primary"
                    :loading="loading"
                    @click="onMatch"
                    class="btn-analyze"
                >
                  <el-icon><Search /></el-icon> 开始分析
                </el-button>
              </div>
            </el-form>
          </div>
        </el-col>

        <el-col :span="12">
          <transition name="fade-slide">
            <div v-if="matchStore.latest" class="result-card">
              <div class="card-header">
                <span class="card-title">📊 匹配结果</span>
                <span class="card-subtitle">{{ matchStore.latest.createdAt || '最新分析' }}</span>
              </div>

              <div class="result-content">
                <div class="score-ring" :style="{ '--score': matchStore.latest.matchScore }">
                  <span class="score-value">{{ matchStore.latest.matchScore }}</span>
                  <span class="score-label">匹配度</span>
                </div>

                <div class="analysis-section">
                  <div class="analysis-header">
                    <span class="analysis-title">分析详情</span>
                    <el-tag :type="scoreTagType(matchStore.latest.matchScore)" effect="plain" size="large">
                      {{ scoreLabel(matchStore.latest.matchScore) }}
                    </el-tag>
                  </div>
                  <div class="analysis-content">
                    <pre>{{ matchStore.latest.analysis || '暂无详细分析' }}</pre>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="result-placeholder">
              <el-icon :size="48"><DocumentCopy /></el-icon>
              <p>完成匹配分析后，结果将显示在这里</p>
              <span class="placeholder-hint">选择简历并粘贴职位描述，点击「开始分析」</span>
            </div>
          </transition>
        </el-col>
      </el-row>
    </section>

    <!-- 历史记录 -->
    <section class="history-section">
      <div class="history-header">
        <span class="history-title">📜 匹配历史</span>
        <span class="history-count">共 {{ matchStore.history.length }} 条记录</span>
      </div>

      <el-table :data="matchStore.history" class="history-table" stripe>
        <el-table-column prop="resumeId" label="简历" min-width="150">
          <template #default="{ row }">
            <span class="resume-name">{{ getResumeTitle(row.resumeId) || row.resumeId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="matchScore" label="匹配度" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="scoreTagType(row.matchScore)" effect="plain" size="large">
              {{ row.matchScore }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="分析时间" min-width="180" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.analysis ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.analysis ? '已完成' : '待分析' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!matchStore.history.length" class="empty-history">
        <el-icon :size="32"><Document /></el-icon>
        <p>暂无匹配记录</p>
      </div>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMatchStore } from "@/stores/match";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";
import {
  Document,
  Search,
  DocumentCopy
} from "@element-plus/icons-vue";

const router = useRouter();
const matchStore = useMatchStore();
const resumeStore = useResumeStore();

const resumes = computed(() => resumeStore.list);
const resumeId = ref<number | null>(null);
const jdText = ref("");
const loading = ref(false);

// ===== 页面逻辑 =====
function getResumeTitle(id: number): string {
  const resume = resumeStore.list.find(r => r.id === id);
  return resume?.title || '';
}

function scoreTagType(score: number) {
  if (score >= 80) return "success";
  if (score >= 60) return "warning";
  return "danger";
}

function scoreLabel(score: number): string {
  if (score >= 80) return "高度匹配 🎉";
  if (score >= 60) return "部分匹配 💡";
  return "待提升 📈";
}

onMounted(async () => {
  await resumeStore.loadList();
  await matchStore.loadHistory();
  if (resumeStore.list.length) {
    resumeId.value = resumeStore.list[0].id;
  }
});

async function onMatch() {
  if (!resumeId.value || !jdText.value.trim()) {
    ElMessage.warning("请选择简历并粘贴职位描述");
    return;
  }
  loading.value = true;
  try {
    await matchStore.runMatch(resumeId.value, jdText.value);
    ElMessage.success("匹配分析完成！");
  } catch (e: any) {
    ElMessage.error(e.message || "匹配分析失败，请重试");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.match-page {
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
  padding: 28px 32px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.form-card {
  width: 100%;
}

.result-card {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.result-placeholder {
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
.result-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 12px;
}
.result-placeholder p {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}
.placeholder-hint {
  font-size: 13px;
  color: #b8cec2;
  margin-top: 6px;
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

.el-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  transition: all 0.3s ease;
}
.el-select :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
.el-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.30);
}

.el-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  font-family: inherit;
  line-height: 1.7;
  transition: all 0.3s ease;
  min-height: 200px;
}
.el-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn-analyze {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 8px;
  padding: 12px 36px;
  font-size: 15px;
  width: 100%;
}
.btn-analyze:hover {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}
.btn-analyze.is-loading {
  opacity: 0.8;
}

.result-content {
  display: flex;
  gap: 28px;
  align-items: flex-start;
  padding: 8px 0;
}

.score-ring {
  flex-shrink: 0;
  width: 130px;
  height: 130px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: conic-gradient(
      #67c23a calc(var(--score) * 1%),
      #ebeef5 calc(var(--score) * 1%)
  );
  position: relative;
}

.score-ring::before {
  content: "";
  position: absolute;
  inset: 10px;
  border-radius: 50%;
  background: #fff;
}


.score-value {

  position: relative;
  z-index: 1;
  font-size: 2.2rem;
  font-weight: 700;
  color: #2c4d3d;
  line-height: 1;
}

.score-label {
  position: relative;
  z-index: 1;
  font-size: 13px;
  color: #8aa89a;
  margin-top: 2px;
}

.analysis-section {
  flex: 1;
  min-width: 0;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.analysis-title {
  font-weight: 600;
  color: #2c4d3d;
  font-size: 15px;
}

.analysis-content {
  background: #f8fbf9;
  border-radius: 10px;
  padding: 16px 18px;
  border: 1px solid #e8f0ec;
  max-height: 220px;
  overflow-y: auto;
}

.analysis-content pre {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #4f6b5d;
  font-size: 14px;
  font-family: inherit;
}

.history-section {
  background-color: #fff;
  padding: 24px 32px 28px;
  border-radius: 16px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.history-title {
  font-size: 17px;
  font-weight: 600;
  color: #3d6b57;
}

.history-count {
  font-size: 13px;
  color: #8aa89a;
}

.history-table {
  border-radius: 10px;
  overflow: hidden;
}

.history-table :deep(.el-table__header th) {
  background: #f0f7f4;
  color: #3d6b57;
  font-weight: 600;
}

.resume-name {
  color: #2c4d3d;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #b8cec2;
}
.empty-history .el-icon {
  color: #c8d8d2;
  margin-bottom: 8px;
}
.empty-history p {
  margin: 0;
  font-size: 14px;
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

@media (max-width: 992px) {
  .result-content {
    flex-direction: column;
    align-items: center;
  }
  .analysis-section {
    width: 100%;
  }
  .score-ring {
    width: 110px;
    height: 110px;
  }
  .score-value {
    font-size: 1.8rem;
  }
}

@media (max-width: 768px) {
  .match-page {
    padding: 16px;
  }
  .hero-title {
    font-size: 24px;
  }
  .content-section {
    padding: 16px 18px;
  }
  .history-section {
    padding: 16px 18px 20px;
  }
  .result-card {
    min-height: 300px;
  }
}
</style>