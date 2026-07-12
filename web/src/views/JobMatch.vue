<template>
  <div class="match-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">🎯 职位匹配分析</h1>
      <p class="hero-subtitle">选择简历并粘贴职位描述，智能分析你的匹配程度。</p>
    </section>

    <!-- 内容区域：左右布局改为上下布局 -->
    <section class="content-section">
      <!-- 上半部分：表单 + 概览 -->
      <el-row :gutter="24" class="top-row">
        <!-- 左侧：表单 -->
        <el-col :span="14">
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
                    :rows="8"
                    placeholder="请粘贴职位描述（JD）到这里..."
                />
              </el-form-item>
              <div class="form-actions">
                <el-button
                    type="primary"
                    :loading="isAnalyzing"
                    @click="onMatch"
                    class="btn-analyze"
                    :disabled="isAnalyzing"
                >
                  <el-icon><Search /></el-icon>
                  {{ isAnalyzing ? '分析中...' : '开始分析' }}
                </el-button>
              </div>
            </el-form>
          </div>
        </el-col>

        <!-- 右侧：概览卡片（总分 + 匹配等级 + 雷达图缩略） -->
        <el-col :span="10">
          <div v-if="matchStore.currentAnalysis" class="overview-card">
            <div class="overview-header">
              <span class="overview-title">📊 匹配概览</span>
              <span class="overview-time">{{ matchStore.currentAnalysis.createdAt || '最新分析' }}</span>
            </div>
            <div class="overview-content">
              <!-- 总分环 -->
              <div class="overview-score">
                <div class="score-ring" :style="{ '--score': matchStore.currentAnalysis.matchScore }">
                  <span class="score-value">{{ matchStore.currentAnalysis.matchScore }}</span>
                  <span class="score-label">匹配度</span>
                </div>
                <div class="match-level">
                  <el-tag :type="scoreTagType(matchStore.currentAnalysis.matchScore)" effect="plain" size="large">
                    {{ scoreLabel(matchStore.currentAnalysis.matchScore) }}
                  </el-tag>
                </div>
              </div>
              <!-- 雷达图缩略 -->
              <div class="overview-radar">
                <RadarChart
                    :data="radarData"
                    height="180px"
                    :max-score="100"
                />
              </div>
            </div>
          </div>

          <div v-else class="overview-placeholder">
            <el-icon :size="40"><DocumentCopy /></el-icon>
            <p>完成匹配分析后，概览将显示在这里</p>
          </div>
        </el-col>
      </el-row>

      <!-- 下半部分：完整分析结果（占页面约60%） -->
      <div v-if="matchStore.currentAnalysis" class="result-full">
        <div class="result-full-header">
          <span class="result-full-title">📝 详细分析报告</span>
          <div class="result-full-actions">
            <el-tag size="small" type="info" effect="plain">
              v{{ matchStore.currentAnalysis.version || '1.0' }}
            </el-tag>
          </div>
        </div>

        <!-- 亮点与不足 -->
        <div v-if="highlights.length || weaknesses.length" class="analysis-tags">
          <div v-if="highlights.length" class="tag-group">
            <span class="tag-label">✅ 亮点</span>
            <el-tag
                v-for="(item, idx) in highlights"
                :key="'h-' + idx"
                type="success"
                size="small"
                effect="plain"
                class="analysis-tag"
            >
              {{ item }}
            </el-tag>
          </div>
          <div v-if="weaknesses.length" class="tag-group">
            <span class="tag-label">⚠️ 待提升</span>
            <el-tag
                v-for="(item, idx) in weaknesses"
                :key="'w-' + idx"
                type="warning"
                size="small"
                effect="plain"
                class="analysis-tag"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>

        <!-- 详细分析文本 -->
        <div class="analysis-content">
          <pre>{{ matchStore.currentAnalysis.analysis || '暂无详细分析' }}</pre>
        </div>

        <!-- 优化建议 - 可折叠框 -->
        <div v-if="suggestions.length" class="suggestions-collapse">
          <div class="suggestions-toggle" @click="showSuggestions = !showSuggestions">
            <span class="suggestions-title">
              💡 优化建议（{{ suggestions.length }}条）
            </span>
            <el-icon>
              <ArrowDown v-if="!showSuggestions" />
              <ArrowUp v-else />
            </el-icon>
          </div>
          <el-collapse-transition>
            <div v-show="showSuggestions" class="suggestions-list">
              <div
                  v-for="(suggestion, idx) in suggestions"
                  :key="'s-' + idx"
                  class="suggestion-item"
              >
                <el-tag
                    :type="suggestionType(suggestion.priority)"
                    size="small"
                    effect="plain"
                    class="suggestion-tag"
                >
                  {{ suggestion.title || suggestion.type }}
                </el-tag>
                <span class="suggestion-desc">{{ suggestion.description }}</span>
              </div>
            </div>
          </el-collapse-transition>
        </div>
      </div>

      <!-- 无结果时的占位 -->
      <div v-else class="result-full-placeholder">
        <el-icon :size="64"><Document /></el-icon>
        <p>选择简历并粘贴职位描述，点击「开始分析」</p>
        <span class="placeholder-hint">AI 将智能分析你的匹配程度</span>
      </div>
    </section>

    <!-- 历史记录 -->
    <section class="history-section">
      <div class="history-header">
        <span class="history-title">📜 匹配历史</span>
        <span class="history-count">共 {{ matchStore.history.length }} 条记录</span>
        <el-button size="small" @click="loadHistory" :loading="isLoadingHistory">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
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
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
                v-if="row.analysisId"
                type="primary"
                link
                size="small"
                @click="viewAnalysis(row.analysisId)"
                :disabled="isViewingDetail"
            >
              {{ isViewingDetail && currentViewingId === row.analysisId ? '加载中...' : '查看详情' }}
            </el-button>
            <span v-else class="text-muted">-</span>
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
import { computed, onMounted, ref, nextTick } from "vue";
import { useMatchStore } from "@/stores/match";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";
import {
  Document,
  Search,
  DocumentCopy,
  Refresh,
  ArrowDown,
  ArrowUp,
} from "@element-plus/icons-vue";
import RadarChart from "@/components/RadarChart.vue";
import type { RadarDataItem } from "@/components/RadarChart.vue";

const matchStore = useMatchStore();
const resumeStore = useResumeStore();

// ===== 状态 =====
const resumes = computed(() => resumeStore.list);
const resumeId = ref<number | null>(null);
const jdText = ref("");
const isAnalyzing = ref(false);
const isLoadingHistory = ref(false);
const isViewingDetail = ref(false);
const currentViewingId = ref<number | null>(null);
const showSuggestions = ref(true);

// ===== 计算属性 =====
// 雷达图数据
const radarData = computed<RadarDataItem[]>(() => {
  const analysis = matchStore.currentAnalysis;
  if (!analysis?.dimensions?.length) {
    return [];
  }
  return analysis.dimensions.map(d => ({
    name: d.name,
    value: d.score,
    description: d.description,
  }));
});

// 亮点列表
const highlights = computed(() => {
  return matchStore.currentAnalysis?.highlights || [];
});

// 不足列表
const weaknesses = computed(() => {
  return matchStore.currentAnalysis?.weaknesses || [];
});

// 建议列表
const suggestions = computed(() => {
  return matchStore.currentAnalysis?.suggestions || [];
});

// ===== 方法 =====
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

function suggestionType(priority: string): string {
  const map: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'info',
  };
  return map[priority] || 'info';
}

async function loadHistory() {
  isLoadingHistory.value = true;
  try {
    await matchStore.loadHistory();
  } catch (e: any) {
    ElMessage.error(e.message || '加载历史失败');
  } finally {
    isLoadingHistory.value = false;
  }
}

async function viewAnalysis(analysisId: number) {
  if (isViewingDetail.value) return;

  isViewingDetail.value = true;
  currentViewingId.value = analysisId;

  try {
    await matchStore.loadAnalysisDetail(analysisId);
    ElMessage.success('已加载分析详情');
    // 滚动到结果区域
    await nextTick();
    const resultEl = document.querySelector('.result-full');
    if (resultEl) {
      resultEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载详情失败');
  } finally {
    isViewingDetail.value = false;
    currentViewingId.value = null;
  }
}

async function onMatch() {
  // 防止重复点击
  if (isAnalyzing.value) {
    return;
  }

  if (!resumeId.value) {
    ElMessage.warning("请选择简历");
    return;
  }
  if (!jdText.value.trim()) {
    ElMessage.warning("请粘贴职位描述");
    return;
  }

  isAnalyzing.value = true;
  try {
    await matchStore.runMatch(resumeId.value, jdText.value);
    ElMessage.success("匹配分析完成！");
    showSuggestions.value = true;
    // 滚动到结果区域
    await nextTick();
    const resultEl = document.querySelector('.result-full');
    if (resultEl) {
      resultEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  } catch (e: any) {
    ElMessage.error(e.message || "匹配分析失败，请重试");
  } finally {
    isAnalyzing.value = false;
  }
}

// ===== 生命周期 =====
onMounted(async () => {
  await resumeStore.loadList();
  await loadHistory();
  if (resumeStore.list.length) {
    resumeId.value = resumeStore.list[0].id;
  }
});
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
  margin-bottom: 28px;
  padding: 24px 40px;
  background: #FBFCCD;
  border-radius: 16px;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 30px;
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
  margin-bottom: 24px;
}

/* ===== 上半部分：表单 + 概览 ===== */
.top-row {
  margin-bottom: 24px;
}

.form-card {
  width: 100%;
}

.card-header {
  margin-bottom: 16px;
}

.card-title {
  display: block;
  font-size: 17px;
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
  min-height: 140px;
}
.el-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.btn-analyze {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 8px;
  padding: 11px 36px;
  font-size: 15px;
  min-width: 140px;
}
.btn-analyze:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}
.btn-analyze:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* ===== 概览卡片 ===== */
.overview-card {
  height: 100%;
  padding: 16px 20px;
  background: #f8fbf9;
  border-radius: 12px;
  border: 1px solid #e8f0ec;
  min-height: 200px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.overview-title {
  font-weight: 600;
  color: #3d6b57;
  font-size: 15px;
}

.overview-time {
  font-size: 12px;
  color: #8aa89a;
}

.overview-content {
  display: flex;
  gap: 16px;
  align-items: center;
}

.overview-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.score-ring {
  flex-shrink: 0;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: conic-gradient(
      #64A386 calc(var(--score) * 1%),
      #ebeef5 calc(var(--score) * 1%)
  );
  position: relative;
}

.score-ring::before {
  content: "";
  position: absolute;
  inset: 6px;
  border-radius: 50%;
  background: #f8fbf9;
}

.score-value {
  position: relative;
  z-index: 1;
  font-size: 1.6rem;
  font-weight: 700;
  color: #2c4d3d;
  line-height: 1;
}

.score-value::after {
  content: "%";
  font-size: 0.8rem;
  font-weight: 500;
  color: #8aa89a;
}

.score-label {
  position: relative;
  z-index: 1;
  font-size: 11px;
  color: #8aa89a;
  margin-top: 1px;
}

.match-level {
  margin-top: 2px;
}

.overview-radar {
  flex: 1;
  min-width: 0;
}

.overview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 180px;
  color: #a0bcae;
}
.overview-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 8px;
}
.overview-placeholder p {
  margin: 0;
  font-size: 14px;
}

/* ===== 下半部分：完整分析结果（占约60%） ===== */
.result-full {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 2px solid #f0f5f2;
  min-height: 400px;
}

.result-full-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-full-title {
  font-size: 18px;
  font-weight: 600;
  color: #3d6b57;
}

.result-full-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 分析标签 */
.analysis-tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.tag-label {
  font-size: 13px;
  font-weight: 500;
  color: #4f6b5d;
  margin-right: 4px;
}

.analysis-tag {
  max-width: 320px;
  white-space: normal;
  word-break: break-word;
  height: auto;
  line-height: 1.4;
  padding: 4px 12px;
}

/* 分析文本 */
.analysis-content {
  background: #f8fbf9;
  border-radius: 10px;
  padding: 16px 20px;
  border: 1px solid #e8f0ec;
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.analysis-content pre {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #4f6b5d;
  font-size: 14px;
  font-family: inherit;
}

/* ===== 优化建议 - 可折叠框 ===== */
.suggestions-collapse {
  border: 1px solid #e8f0ec;
  border-radius: 10px;
  overflow: hidden;
  background: #fafdfb;
}

.suggestions-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}
.suggestions-toggle:hover {
  background: #f0f7f4;
}

.suggestions-title {
  font-weight: 600;
  color: #2c4d3d;
  font-size: 14px;
}

.suggestions-toggle .el-icon {
  color: #8aa89a;
  font-size: 18px;
  transition: transform 0.3s;
}

.suggestions-list {
  padding: 0 18px 12px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f8f6;
}
.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-tag {
  flex-shrink: 0;
  font-size: 12px;
}

.suggestion-desc {
  font-size: 13px;
  color: #4f6b5d;
  line-height: 1.6;
}

/* ===== 结果占位 ===== */
.result-full-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: #f8fbf9;
  border-radius: 12px;
  border: 2px dashed #dce8e2;
  color: #a0bcae;
  margin-top: 8px;
}
.result-full-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 12px;
}
.result-full-placeholder p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}
.placeholder-hint {
  font-size: 13px;
  color: #b8cec2;
  margin-top: 6px;
}

/* ===== 历史记录 ===== */
.history-section {
  background-color: #fff;
  padding: 20px 28px 24px;
  border-radius: 16px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
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
  padding: 36px 0;
  color: #b8cec2;
}
.empty-history .el-icon {
  color: #c8d8d2;
  margin-bottom: 6px;
}
.empty-history p {
  margin: 0;
  font-size: 14px;
}

.text-muted {
  color: #b8cec2;
  font-size: 13px;
}

.blank-area {
  width: 100%;
  min-height: 40px;
}

/* ===== 过渡动画 ===== */
.fade-slide-enter-active {
  transition: all 0.4s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .overview-content {
    flex-direction: column;
    align-items: center;
  }
  .overview-radar {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .match-page {
    padding: 12px;
  }
  .hero-title {
    font-size: 22px;
  }
  .hero-section {
    padding: 16px 20px;
  }
  .content-section {
    padding: 14px 16px;
  }
  .history-section {
    padding: 14px 16px 18px;
  }
  .analysis-tag {
    max-width: 100%;
  }
  .result-full {
    min-height: 300px;
  }
  .analysis-content {
    max-height: 200px;
  }
  .btn-analyze {
    width: 100%;
    justify-content: center;
  }
}
</style>