<!-- src/views/MatchRecommend.vue -->
<template>
  <div class="match-recommend-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">🎯 AI 岗位匹配推荐</h1>
      <p class="hero-subtitle">
        基于你的简历内容，智能匹配推荐最合适的岗位，支持联网搜索和平台内岗位匹配
      </p>
    </section>

    <!-- 内容区域 -->
    <section class="content-section">
      <!-- 上半部分：表单 + 概览 -->
      <el-row :gutter="24" class="top-row">
        <!-- 左侧：表单 -->
        <el-col :span="14">
          <div class="form-card">
            <div class="card-header">
              <span class="card-title">📋 匹配设置</span>
              <span class="card-subtitle">选择简历，AI将为你匹配推荐合适岗位</span>
            </div>
            <el-form label-width="80px" label-position="top">
              <el-form-item label="选择简历">
                <el-select
                    v-model="selectedResumeId"
                    placeholder="请选择简历"
                    style="width: 100%"
                    filterable
                    :loading="resumeLoading"
                >
                  <el-option
                      v-for="r in resumeList"
                      :key="r.id"
                      :label="r.title || `简历 #${r.id}`"
                      :value="r.id"
                  />
                </el-select>
                <div v-if="resumeList.length === 0 && !resumeLoading" class="empty-tip">
                  暂无简历，请先去 <router-link to="/resume">简历管理</router-link> 创建
                </div>
              </el-form-item>

              <el-form-item label="联网搜索">
                <el-switch
                    v-model="enableWebSearch"
                    active-text="开启"
                    inactive-text="关闭"
                    :active-value="true"
                    :inactive-value="false"
                />
                <span class="switch-hint">开启后可获取外部招聘平台岗位</span>
              </el-form-item>

              <div class="form-actions">
                <el-button
                    type="primary"
                    :loading="store.loading"
                    @click="onMatch"
                    class="btn-analyze"
                    :disabled="!selectedResumeId || store.loading || resumeList.length === 0"
                >
                  <el-icon><Search /></el-icon>
                  {{ store.loading ? "匹配中..." : "开始匹配" }}
                </el-button>
                <el-button
                    v-if="store.hasRecommendations || store.improvementSuggestion"
                    plain
                    @click="clearResult"
                >
                  清空结果
                </el-button>
              </div>
            </el-form>
          </div>
        </el-col>

        <!-- 右侧：概览卡片 -->
        <el-col :span="10">
          <div v-if="store.hasRecommendations" class="overview-card">
            <div class="overview-header">
              <span class="overview-title">📊 匹配概览</span>
              <span class="overview-time">{{ formatTime(store.currentResult?.createdAt) }}</span>
            </div>
            <div class="overview-content">
              <div class="overview-score">
                <div class="score-ring" :style="{ '--score': store.topMatchScore }">
                  <span class="score-value">{{ store.topMatchScore }}</span>
                  <span class="score-label">最高匹配</span>
                </div>
                <div class="match-level">
                  <el-tag :type="store.matchLevel.type" effect="plain" size="large">
                    {{ store.matchLevel.label }}
                  </el-tag>
                </div>
                <div class="match-count">
                  推荐 <strong>{{ store.recommendations.length }}</strong> 个岗位
                </div>
              </div>
              <div class="overview-radar">
                <RadarChart
                    v-if="radarData.length"
                    :data="radarData"
                    height="180px"
                    :max-score="100"
                />
                <div v-else class="radar-placeholder">暂无维度数据</div>
              </div>
            </div>
          </div>

          <div v-else-if="store.improvementSuggestion" class="overview-card suggestion-card">
            <div class="overview-header">
              <span class="overview-title">💡 简历改进建议</span>
              <el-tag
                  :type="getPriorityTagType(store.improvementSuggestion.priority)"
                  size="small"
                  effect="plain"
              >
                {{ getPriorityLabel(store.improvementSuggestion.priority) }}
              </el-tag>
            </div>
            <div class="suggestion-preview">
              <p class="problem-desc">{{ store.improvementSuggestion.problemDescription }}</p>
              <div class="suggestion-tags">
                <el-tag
                    v-for="(s, idx) in store.improvementSuggestion.suggestions.slice(0, 2)"
                    :key="idx"
                    size="small"
                    type="warning"
                    effect="plain"
                >
                  {{ s }}
                </el-tag>
                <el-tag
                    v-if="store.improvementSuggestion.suggestions.length > 2"
                    size="small"
                    type="info"
                >
                  +{{ store.improvementSuggestion.suggestions.length - 2 }} 条
                </el-tag>
              </div>
              <el-button type="primary" link size="small" @click="scrollToSuggestions">
                查看全部建议 →
              </el-button>
            </div>
          </div>

          <div v-else class="overview-placeholder">
            <el-icon :size="40"><DocumentCopy /></el-icon>
            <p>{{ resumeList.length === 0 ? '请先创建简历' : '选择简历，点击「开始匹配」' }}</p>
            <span class="placeholder-hint">{{ resumeList.length === 0 ? '前往简历管理页面创建' : 'AI将为你推荐最匹配的岗位' }}</span>
          </div>
        </el-col>
      </el-row>

      <!-- ===== 推荐结果网格 ===== -->
      <div v-if="store.hasRecommendations" class="result-section">
        <div class="result-header">
          <span class="result-title">📝 推荐岗位</span>
          <span class="result-count">共 {{ store.recommendations.length }} 个推荐</span>
        </div>

        <div class="recommend-grid">
          <el-card
              v-for="(item, index) in store.recommendations"
              :key="index"
              class="recommend-card"
              :class="{ 'top-match': index === 0 }"
              shadow="hover"
              @click="viewDetail(item)"
          >
            <div class="card-content">
              <div class="card-top">
                <div class="rank-badge" v-if="index === 0">🏆</div>
                <div class="rank-badge rank-number" v-else>#{{ index + 1 }}</div>
                <el-tag
                    :type="getScoreTagType(item.matchScore)"
                    size="large"
                    effect="plain"
                    class="score-tag"
                >
                  {{ item.matchScore }}%
                </el-tag>
              </div>

              <h3 class="job-name">{{ item.jobName }}</h3>

              <div class="job-meta">
                <el-tag
                    :type="item.source === 'NETWORK' ? 'primary' : 'success'"
                    size="small"
                    effect="plain"
                >
                  {{ item.source === "NETWORK" ? "🌐 联网搜索" : "📦 平台内" }}
                </el-tag>
                <span v-if="item.sourceJobId" class="source-id">
                  ID: {{ item.sourceJobId }}
                </span>
              </div>

              <div class="jd-preview">
                {{ getJdPreview(item.jdContent) }}
              </div>

              <div class="card-footer">
                <span class="click-hint">点击查看完整详情 →</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 改进建议详情 -->
      <div v-else-if="store.improvementSuggestion" class="result-section suggestion-section" id="suggestion-section">
        <div class="result-header">
          <span class="result-title">💡 简历改进建议</span>
          <el-tag
              :type="getPriorityTagType(store.improvementSuggestion.priority)"
              size="small"
              effect="plain"
          >
            {{ getPriorityLabel(store.improvementSuggestion.priority) }}优先级
          </el-tag>
        </div>

        <div class="suggestion-detail-card">
          <div class="suggestion-problem">
            <h4>📌 问题诊断</h4>
            <p>{{ store.improvementSuggestion.problemDescription }}</p>
          </div>

          <div class="suggestion-list">
            <h4>📝 改进建议</h4>
            <ul>
              <li v-for="(s, idx) in store.improvementSuggestion.suggestions" :key="idx">
                <el-icon><Check /></el-icon>
                <span>{{ s }}</span>
              </li>
            </ul>
          </div>

          <div class="suggestion-priority">
            <span class="priority-label">优先级：</span>
            <el-tag
                :type="getPriorityTagType(store.improvementSuggestion.priority)"
                size="small"
            >
              {{ getPriorityLabel(store.improvementSuggestion.priority) }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="result-section-placeholder">
        <el-icon :size="64"><Document /></el-icon>
        <p>{{ resumeList.length === 0 ? '请先创建简历' : '选择简历并点击「开始匹配」' }}</p>
        <span class="placeholder-hint">{{ resumeList.length === 0 ? '前往简历管理页面创建简历后重试' : 'AI将综合分析你的简历，推荐最匹配的岗位' }}</span>
      </div>
    </section>

    <!-- 历史记录 -->
    <section class="history-section">
      <div class="history-header">
        <span class="history-title">📜 推荐历史</span>
        <span class="history-count">共 {{ store.total }} 条记录</span>
        <el-button size="small" @click="loadHistory" :loading="store.historyLoading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>

      <el-table
          v-if="store.history.length"
          :data="store.history"
          class="history-table"
          stripe
          @row-click="viewHistoryDetail"
      >
        <el-table-column prop="resumeId" label="简历ID" width="100" align="center" />
        <el-table-column prop="jobName" label="岗位名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="matchScore" label="匹配度" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getScoreTagType(row.matchScore)" effect="plain" size="large">
              {{ row.matchScore }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120" align="center">
          <template #default="{ row }">
            <el-tag
                :type="row.source === 1 ? 'primary' : 'success'"
                size="small"
                effect="plain"
            >
              {{ row.source === 1 ? "联网搜索" : "平台内" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="推荐时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="viewHistoryDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="empty-history">
        <el-icon :size="32"><Document /></el-icon>
        <p>暂无推荐记录</p>
      </div>

      <div v-if="store.total > 0" class="history-pagination">
        <el-pagination
            v-model:current-page="historyPage"
            v-model:page-size="historySize"
            :total="store.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="onPageChange"
            @size-change="onPageChange"
        />
      </div>
    </section>

    <!-- ===== 详情弹窗（点击卡片弹出） ===== -->
    <el-dialog
        v-model="detailDialogVisible"
        :title="detailJobName"
        width="720px"
        :close-on-click-modal="true"
        destroy-on-close
        class="detail-dialog"
    >
      <div v-if="detailData" class="detail-content">
        <div class="detail-header">
          <div class="detail-score">
            <el-tag :type="getScoreTagType(detailData.matchScore)" size="large" effect="plain">
              {{ detailData.matchScore }}% 匹配
            </el-tag>
          </div>
          <div class="detail-source">
            <el-tag
                :type="detailData.source === 'NETWORK' ? 'primary' : 'success'"
                size="small"
                effect="plain"
            >
              {{ detailData.source === "NETWORK" ? "🌐 联网搜索" : "📦 平台内" }}
            </el-tag>
            <span v-if="detailData.sourceUrl" class="source-link">
              <el-icon><Link /></el-icon>
              <a :href="detailData.sourceUrl" target="_blank" rel="noopener">查看来源</a>
            </span>
            <span v-if="detailData.sourceJobId" class="source-id">
              岗位 ID: {{ detailData.sourceJobId }}
            </span>
          </div>
        </div>

        <div class="detail-jd">
          <h4>📋 职位描述</h4>
          <div class="jd-full" v-html="formatJdContent(detailData.jdContent)"></div>
        </div>

        <!-- 匹配理由已移除 -->

        <div class="detail-time">
          <span>推荐时间：{{ formatTime(detailData.createdAt) }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import {
  Document,
  Search,
  DocumentCopy,
  Refresh,
  Link,
  Check,
} from "@element-plus/icons-vue";
import { useMatchRecommendStore } from "@/stores/matchRecommend";
import { useResumeStore } from "@/stores/resume";
import RadarChart from "@/components/RadarChart.vue";
import type { RadarDataItem } from "@/components/RadarChart.vue";
import type { JobRecommendation } from "@/types/api";

const store = useMatchRecommendStore();
const resumeStore = useResumeStore();

// ===== 本地状态 =====
const selectedResumeId = ref<number | null>(null);
const enableWebSearch = ref<boolean>(true);
const detailDialogVisible = ref(false);
const detailData = ref<JobRecommendation | null>(null);
const detailJobName = ref<string>("");
const historyPage = ref(1);
const historySize = ref(20);
const resumeLoading = ref(false);

// ===== 简历列表（从 store 获取） =====
const resumeList = computed(() => resumeStore.list);

// 当简历列表变化时，自动选中第一个
watch(
    () => resumeList.value,
    (list) => {
      if (list.length > 0 && !selectedResumeId.value) {
        selectedResumeId.value = list[0].id;
      }
      if (list.length > 0 && selectedResumeId.value) {
        const exists = list.some(r => r.id === selectedResumeId.value);
        if (!exists) {
          selectedResumeId.value = list[0].id;
        }
      }
    },
    { immediate: true }
);

// ===== 计算属性 =====
const radarData = computed<RadarDataItem[]>(() => {
  const recommendations = store.recommendations;
  if (recommendations.length === 0) return [];

  return recommendations.slice(0, 5).map((item) => ({
    name: item.jobName.length > 8 ? item.jobName.slice(0, 8) + "..." : item.jobName,
    value: item.matchScore,
  }));
});

// ===== 工具方法 =====
function formatTime(time: string | undefined): string {
  if (!time) return "--";
  try {
    const date = new Date(time);
    if (isNaN(date.getTime())) return time;
    return date.toLocaleString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return time;
  }
}

function getScoreTagType(score: number): string {
  if (score >= 80) return "success";
  if (score >= 60) return "warning";
  return "danger";
}

function getPriorityTagType(priority: "high" | "medium" | "low"): string {
  const map = { high: "danger", medium: "warning", low: "info" };
  return map[priority] || "info";
}

function getPriorityLabel(priority: "high" | "medium" | "low"): string {
  const map = { high: "高", medium: "中", low: "低" };
  return map[priority] || "中";
}

function formatJdContent(content: string): string {
  if (!content) return "暂无职位描述";
  return content
      .replace(/\n/g, "<br>")
      .replace(/(公司|薪资|职位描述|职责要求|任职资格|岗位要求|工作职责|任职要求)/g, "<strong>$1</strong>");
}

function getJdPreview(content: string): string {
  if (!content) return "暂无职位描述";
  const preview = content.replace(/\n/g, " ").slice(0, 100);
  return preview + (content.length > 100 ? "..." : "");
}

function scrollToSuggestions() {
  const el = document.getElementById("suggestion-section");
  if (el) {
    el.scrollIntoView({ behavior: "smooth", block: "start" });
  }
}

function clearResult() {
  store.clearCurrent();
  ElMessage.info("已清空推荐结果");
}

// ===== 查看详情 =====
function viewDetail(item: JobRecommendation) {
  detailData.value = {
    ...item,
    // 确保 createdAt 存在，用于详情弹窗显示
    createdAt: store.currentResult?.createdAt || new Date().toISOString(),
  };
  detailJobName.value = item.jobName;
  detailDialogVisible.value = true;
}

async function viewHistoryDetail(row: any) {
  try {
    const detail = await store.loadDetail(row.id);
    detailData.value = {
      jobName: detail.jobName,
      jdContent: detail.jdContent,
      matchScore: detail.matchScore,
      matchReason: detail.matchReason,
      source: detail.source === 1 ? "NETWORK" : "PLATFORM",
      sourceUrl: detail.sourceUrl,
      sourceJobId: detail.sourceJobId,
      createdAt: detail.createdAt,
    };
    detailJobName.value = detail.jobName;
    detailDialogVisible.value = true;
  } catch (error: any) {
    ElMessage.error(error.message || "加载详情失败");
  }
}

// ===== 业务方法 =====
async function onMatch() {
  if (!selectedResumeId.value) {
    ElMessage.warning("请选择简历");
    return;
  }

  try {
    await store.runRecommend(selectedResumeId.value, enableWebSearch.value);

    if (store.hasRecommendations) {
      ElMessage.success(`匹配完成！找到 ${store.recommendations.length} 个推荐岗位`);
    } else if (store.improvementSuggestion) {
      ElMessage.warning("当前暂无匹配岗位，已生成改进建议");
    } else {
      ElMessage.info("匹配完成，未找到推荐岗位");
    }
  } catch (error: any) {
    ElMessage.error(error.message || "匹配失败，请重试");
  }
}

async function loadHistory() {
  try {
    await store.loadHistory(historyPage.value, historySize.value);
  } catch (error: any) {
    ElMessage.error(error.message || "加载历史记录失败");
  }
}

async function onPageChange() {
  await loadHistory();
}

async function loadResumes() {
  resumeLoading.value = true;
  try {
    await resumeStore.loadList();
  } catch (error: any) {
    ElMessage.error(error.message || "加载简历列表失败");
  } finally {
    resumeLoading.value = false;
  }
}

// ===== 生命周期 =====
onMounted(async () => {
  await loadResumes();
  await loadHistory();
});
</script>

<style scoped>
.match-recommend-page {
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

.switch-hint {
  font-size: 12px;
  color: #8aa89a;
  margin-left: 12px;
}

.empty-tip {
  font-size: 13px;
  color: #e6a23c;
  margin-top: 6px;
}

.empty-tip a {
  color: #64A386;
  text-decoration: none;
  font-weight: 500;
}

.empty-tip a:hover {
  text-decoration: underline;
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
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.3);
}

.el-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.3);
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
  background: conic-gradient(#64A386 calc(var(--score) * 1%), #ebeef5 calc(var(--score) * 1%));
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

.match-count {
  font-size: 13px;
  color: #4f6b5d;
}

.overview-radar {
  flex: 1;
  min-width: 0;
}

.radar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 180px;
  color: #b8cec2;
  font-size: 13px;
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

.placeholder-hint {
  font-size: 12px;
  color: #b8cec2;
  margin-top: 4px;
}

/* ===== 建议卡片 ===== */
.suggestion-card {
  border-color: #fde2e2;
}

.suggestion-preview {
  padding: 4px 0;
}

.problem-desc {
  margin: 0 0 10px;
  font-size: 14px;
  color: #5a4a4a;
  line-height: 1.6;
}

.suggestion-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

/* ===== 结果区域 ===== */
.result-section {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 2px solid #f0f5f2;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-title {
  font-size: 18px;
  font-weight: 600;
  color: #3d6b57;
}

.result-count {
  font-size: 13px;
  color: #8aa89a;
}

.result-section-placeholder {
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

.result-section-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 12px;
}

.result-section-placeholder p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* ===== 推荐卡片网格 ===== */
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.recommend-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #e8f0ec;
  cursor: pointer;
}

.recommend-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(100, 163, 134, 0.15);
}

.recommend-card.top-match {
  border-color: #64A386;
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.15);
}

.recommend-card.top-match:hover {
  box-shadow: 0 8px 35px rgba(100, 163, 134, 0.25);
}

.recommend-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rank-badge {
  font-size: 22px;
  width: 32px;
  text-align: center;
}

.rank-number {
  font-size: 15px;
  font-weight: 600;
  color: #8aa89a;
}

.score-tag {
  font-weight: 600;
  font-size: 15px;
}

.job-name {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #2c4d3d;
  line-height: 1.4;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.source-id {
  font-size: 12px;
  color: #8aa89a;
}

.jd-preview {
  font-size: 14px;
  color: #6a8a7a;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 48px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #f0f5f2;
}

.click-hint {
  font-size: 12px;
  color: #b0c8bc;
  transition: color 0.3s ease;
}

.recommend-card:hover .click-hint {
  color: #64A386;
}

/* ===== 建议详情 ===== */
.suggestion-section {
  border-top: 2px solid #fde2e2;
}

.suggestion-detail-card {
  padding: 20px;
  background: #f8fbf9;
  border-radius: 12px;
  border: 1px solid #e8f0ec;
}

.suggestion-problem {
  margin-bottom: 16px;
}

.suggestion-problem h4 {
  margin: 0 0 8px;
  color: #3d6b57;
}

.suggestion-problem p {
  margin: 0;
  color: #4f6b5d;
  line-height: 1.8;
}

.suggestion-list h4 {
  margin: 0 0 10px;
  color: #3d6b57;
}

.suggestion-list ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestion-list ul li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0;
  color: #4f6b5d;
  line-height: 1.6;
  border-bottom: 1px solid #f0f5f2;
}

.suggestion-list ul li:last-child {
  border-bottom: none;
}

.suggestion-list ul li .el-icon {
  color: #64A386;
  flex-shrink: 0;
  margin-top: 2px;
}

.suggestion-priority {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e8f0ec;
}

.priority-label {
  font-size: 14px;
  color: #4f6b5d;
  margin-right: 8px;
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
  cursor: pointer;
}

.history-table :deep(.el-table__header th) {
  background: #f0f7f4;
  color: #3d6b57;
  font-weight: 600;
}

.history-table :deep(.el-table__row:hover) {
  background: #f8fbf9;
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

.history-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ===== 详情弹窗 ===== */
.detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f0f5f2;
  padding-bottom: 14px;
}

.detail-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #2c4d3d;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 4px 0;
}

.detail-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8f0ec;
}

.detail-source {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.source-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #64A386;
}

.source-link a {
  color: #64A386;
  text-decoration: none;
}

.source-link a:hover {
  text-decoration: underline;
}

.source-id {
  font-size: 13px;
  color: #8aa89a;
}

.detail-jd h4 {
  margin: 0 0 8px;
  color: #3d6b57;
}

.detail-jd {
  margin-bottom: 16px;
}

.jd-full {
  font-size: 14px;
  color: #4f6b5d;
  line-height: 1.8;
  padding: 12px 16px;
  background: #f8fbf9;
  border-radius: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.detail-time {
  font-size: 13px;
  color: #8aa89a;
  padding-top: 12px;
  border-top: 1px solid #e8f0ec;
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

  .recommend-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .match-recommend-page {
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

  .btn-analyze {
    width: 100%;
    justify-content: center;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .detail-dialog {
    width: 95% !important;
  }
}
</style>