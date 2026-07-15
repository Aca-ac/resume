<!-- src/views/JobMatch.vue -->
<template>
  <div class="match-page">
    <!-- 主视觉标语 -->
    <header class="hero-section" aria-labelledby="hero-title">
      <h1 id="hero-title" class="hero-title">
        <span aria-hidden="true">🎯</span>
        职位匹配分析
      </h1>
      <p class="hero-subtitle">选择简历，粘贴职位描述或选择已保存岗位，智能分析匹配程度。</p>
    </header>

    <!-- 内容区域 -->
    <main class="content-section" aria-label="职位匹配分析工具">
      <!-- 上半部分：表单 + 概览 -->
      <div class="top-row">
        <!-- 左侧：表单 -->
        <div class="form-column">
          <div class="form-card">
            <div class="card-header">
              <span class="card-title" id="form-title">📋 匹配设置</span>
              <span class="card-subtitle">选择简历并输入 JD 或选择岗位</span>
            </div>

            <el-form
                ref="formRef"
                :model="formData"
                :rules="formRules"
                label-width="80px"
                label-position="top"
                novalidate
                @submit.prevent="onMatch"
            >
              <el-form-item
                  label="选择简历"
                  prop="resumeId"
                  :error="formErrors.resumeId"
              >
                <el-select
                    v-model="formData.resumeId"
                    placeholder="请选择简历"
                    style="width: 100%"
                    id="resume-select"
                    name="resumeId"
                    :aria-describedby="'resume-hint'"
                    :aria-invalid="!!formErrors.resumeId"
                    @change="clearFieldError('resumeId')"
                >
                  <el-option
                      v-for="r in resumes"
                      :key="r.id"
                      :label="r.title"
                      :value="r.id"
                  />
                </el-select>
                <div id="resume-hint" class="form-hint">选择一份已创建的简历进行分析</div>
                <div v-if="formErrors.resumeId" class="field-error" role="alert">
                  <span aria-hidden="true">⚠️</span>
                  {{ formErrors.resumeId }}
                </div>
              </el-form-item>

              <el-form-item label="JD 来源" prop="matchMode">
                <el-radio-group
                    v-model="formData.matchMode"
                    id="match-mode-group"
                    role="radiogroup"
                    aria-label="职位描述来源选择"
                    @change="clearFieldError('matchMode')"
                >
                  <el-radio-button
                      value="jd"
                      aria-label="粘贴职位描述"
                  >粘贴 JD</el-radio-button>
                  <el-radio-button
                      value="job"
                      aria-label="选择已保存岗位"
                  >选择岗位</el-radio-button>
                </el-radio-group>
                <div v-if="formErrors.matchMode" class="field-error" role="alert">
                  <span aria-hidden="true">⚠️</span>
                  {{ formErrors.matchMode }}
                </div>
              </el-form-item>

              <el-form-item
                  v-if="formData.matchMode === 'job'"
                  label="选择岗位"
                  prop="jobId"
                  :error="formErrors.jobId"
              >
                <el-select
                    v-model="formData.jobId"
                    placeholder="请选择已保存的岗位"
                    style="width: 100%"
                    filterable
                    id="job-select"
                    name="jobId"
                    :loading="isLoadingJobs"
                    :aria-describedby="'job-hint'"
                    :aria-invalid="!!formErrors.jobId"
                    @change="clearFieldError('jobId')"
                >
                  <el-option
                      v-for="j in jobs"
                      :key="j.id"
                      :label="j.jobName"
                      :value="j.id"
                  />
                </el-select>
                <div id="job-hint" class="form-hint">选择已保存的岗位进行匹配分析</div>
                <div v-if="formErrors.jobId" class="field-error" role="alert">
                  <span aria-hidden="true">⚠️</span>
                  {{ formErrors.jobId }}
                </div>
              </el-form-item>

              <el-form-item
                  v-else
                  label="职位描述"
                  prop="jdText"
                  :error="formErrors.jdText"
              >
                <el-input
                    v-model="formData.jdText"
                    type="textarea"
                    :rows="8"
                    placeholder="请粘贴职位描述（JD）到这里..."
                    id="jd-textarea"
                    name="jdText"
                    :aria-describedby="'jd-hint'"
                    :aria-invalid="!!formErrors.jdText"
                    @input="clearFieldError('jdText')"
                />
                <div id="jd-hint" class="form-hint">粘贴完整的职位描述以获得准确分析</div>
                <div v-if="formErrors.jdText" class="field-error" role="alert">
                  <span aria-hidden="true">⚠️</span>
                  {{ formErrors.jdText }}
                </div>
              </el-form-item>

              <div class="form-actions">
                <el-button
                    type="primary"
                    :loading="isAnalyzing"
                    @click="onMatch"
                    class="btn-analyze"
                    :disabled="isAnalyzing"
                    :aria-label="isAnalyzing ? '正在分析中...' : '开始匹配分析'"
                >
                  <el-icon v-if="!isAnalyzing" aria-hidden="true"><Search /></el-icon>
                  <span v-if="!isAnalyzing">开始分析</span>
                  <span v-else>分析中...</span>
                </el-button>
                <el-button
                    v-if="formData.matchMode === 'jd' && formData.jdText"
                    @click="clearJdText"
                    :disabled="isAnalyzing"
                    aria-label="清空职位描述"
                >
                  清空
                </el-button>
              </div>

              <!-- 键盘快捷键提示 -->
              <div class="keyboard-hint" aria-hidden="true">
                按 <kbd>Ctrl</kbd> + <kbd>Enter</kbd> 快速分析
              </div>
            </el-form>
          </div>
        </div>

        <!-- 右侧：概览卡片 -->
        <div class="overview-column">
          <div v-if="matchStore.currentAnalysis" class="overview-card" role="region" aria-label="匹配概览">
            <div class="overview-header">
              <span class="overview-title">📊 匹配概览</span>
              <span class="overview-time">{{ matchStore.currentAnalysis.createdAt || '最新分析' }}</span>
            </div>
            <div class="overview-content">
              <!-- 总分环 -->
              <div class="overview-score" role="img" :aria-label="`匹配度 ${matchStore.currentAnalysis.matchScore}%`">
                <div class="score-ring" :style="{ '--score': matchStore.currentAnalysis.matchScore }">
                  <span class="score-value" aria-hidden="true">{{ matchStore.currentAnalysis.matchScore }}</span>
                  <span class="score-label" aria-hidden="true">匹配度</span>
                </div>
                <div class="match-level">
                  <span
                      class="match-level-tag"
                      :class="scoreTagClass(matchStore.currentAnalysis.matchScore)"
                      role="status"
                      aria-live="polite"
                  >
                    {{ scoreLabel(matchStore.currentAnalysis.matchScore) }}
                  </span>
                </div>
              </div>
              <!-- 雷达图缩略 -->
              <div class="overview-radar" aria-label="各维度匹配度雷达图">
                <RadarChart
                    :data="radarData"
                    height="180px"
                    :max-score="100"
                />
              </div>
            </div>
          </div>

          <div v-else class="overview-placeholder" role="status" aria-live="polite">
            <span aria-hidden="true" style="font-size:40px;">📄</span>
            <p>完成匹配分析后，概览将显示在这里</p>
          </div>
        </div>
      </div>

      <!-- 下半部分：完整分析结果 -->
      <div
          v-if="matchStore.currentAnalysis"
          class="result-full"
          role="article"
          aria-label="详细分析报告"
      >
        <div class="result-full-header">
          <span class="result-full-title">📝 详细分析报告</span>
          <div class="result-full-actions">
            <span class="version-tag" aria-label="版本信息">
              v{{ matchStore.currentAnalysis.version || '1.0' }}
            </span>
          </div>
        </div>

        <!-- 亮点与不足 -->
        <div v-if="hasHighlightsOrWeaknesses" class="analysis-tags">
          <div v-if="highlights.length" class="tag-group">
            <span class="tag-label">✅ 亮点</span>
            <ul class="tag-list" aria-label="匹配亮点列表">
              <li
                  v-for="(item, idx) in highlights"
                  :key="'h-' + idx"
                  class="tag-item"
              >
                <span class="tag-item-content tag-success">{{ item }}</span>
              </li>
            </ul>
          </div>
          <div v-if="weaknesses.length" class="tag-group">
            <span class="tag-label">⚠️ 待提升</span>
            <ul class="tag-list" aria-label="待提升项目列表">
              <li
                  v-for="(item, idx) in weaknesses"
                  :key="'w-' + idx"
                  class="tag-item"
              >
                <span class="tag-item-content tag-warning">{{ item }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 优化建议 -->
        <div v-if="suggestions.length" class="suggestions-collapse">
          <button
              class="suggestions-toggle"
              @click="showSuggestions = !showSuggestions"
              :aria-expanded="showSuggestions"
              aria-controls="suggestions-list"
              :aria-label="showSuggestions ? '收起优化建议' : '展开优化建议'"
          >
            <span class="suggestions-title">
              💡 优化建议（{{ suggestions.length }}条）
            </span>
            <span class="suggestions-icon" aria-hidden="true">
              <ArrowDown v-if="!showSuggestions" />
              <ArrowUp v-else />
            </span>
          </button>
          <div
              v-show="showSuggestions"
              id="suggestions-list"
              class="suggestions-list"
              role="list"
              aria-label="优化建议列表"
          >
            <div
                v-for="(suggestion, idx) in suggestions"
                :key="'s-' + idx"
                class="suggestion-item"
                role="listitem"
            >
              <span
                  class="suggestion-tag"
                  :class="suggestionTagClass(suggestion.priority)"
                  :aria-label="`优先级：${suggestion.priority || 'medium'}`"
              >
                {{ suggestion.title || suggestion.type || '建议' }}
              </span>
              <span class="suggestion-desc">{{ suggestion.description }}</span>
            </div>
          </div>
        </div>

        <div v-else class="no-suggestions" role="status" aria-live="polite">
          <span>暂无优化建议</span>
        </div>

        <!-- 加载状态提示 -->
        <div v-if="isAnalyzing" class="sr-only" role="status" aria-live="polite">
          正在分析匹配度，请稍候...
        </div>
      </div>

      <!-- 无结果时的占位 -->
      <div v-else class="result-full-placeholder" role="status" aria-live="polite">
        <span aria-hidden="true" style="font-size:64px;">📄</span>
        <p>选择简历并粘贴职位描述，点击「开始分析」</p>
        <span class="placeholder-hint">AI 将智能分析你的匹配程度</span>
      </div>

      <!-- 全局错误 -->
      <div
          v-if="globalError"
          class="global-error"
          role="alert"
          aria-live="assertive"
      >
        <span aria-hidden="true">❌</span>
        {{ globalError }}
        <button
            class="error-close-btn"
            @click="clearGlobalError"
            aria-label="关闭错误提示"
        >
          ✕
        </button>
      </div>
    </main>

    <!-- 历史记录 -->
    <section class="history-section" aria-labelledby="history-title">
      <div class="history-header">
        <span id="history-title" class="history-title">📜 匹配历史</span>
        <span class="history-count">共 {{ matchStore.history.length }} 条记录</span>
        <el-button
            size="small"
            @click="loadHistory"
            :loading="isLoadingHistory"
            :aria-label="isLoadingHistory ? '正在刷新历史记录' : '刷新历史记录'"
        >
          <el-icon v-if="!isLoadingHistory" aria-hidden="true"><Refresh /></el-icon>
          {{ isLoadingHistory ? '刷新中...' : '刷新' }}
        </el-button>
      </div>

      <div v-if="matchStore.history.length" class="table-wrapper">
        <table class="history-table" aria-label="匹配历史记录列表">
          <thead>
          <tr>
            <th scope="col">简历</th>
            <th scope="col">岗位</th>
            <th scope="col" class="text-center">匹配度</th>
            <th scope="col">分析时间</th>
            <th scope="col" class="text-center">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="row in matchStore.history" :key="row.id || row.analysisId">
            <td>
              <span class="resume-name">{{ getResumeTitle(row.resumeId) || row.resumeId }}</span>
            </td>
            <td>
              <span v-if="row.jobId">{{ getJobName(row.jobId) || `#${row.jobId}` }}</span>
              <span v-else class="text-muted">手动 JD</span>
            </td>
            <td class="text-center">
                <span
                    class="score-tag"
                    :class="scoreTagClass(row.matchScore)"
                    :aria-label="`匹配度 ${row.matchScore}%`"
                >
                  {{ row.matchScore }}%
                </span>
            </td>
            <td>{{ row.createdAt || row.analyzedAt || '-' }}</td>
            <td class="text-center">
              <button
                  v-if="row.analysisId"
                  class="view-detail-btn"
                  @click="viewAnalysis(row.analysisId)"
                  :disabled="isViewingDetail && currentViewingId === row.analysisId"
                  :aria-label="`查看 #${row.analysisId} 分析详情`"
              >
                {{ isViewingDetail && currentViewingId === row.analysisId ? '加载中...' : '查看详情' }}
              </button>
              <span v-else class="text-muted">-</span>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-history" role="status" aria-live="polite">
        <span aria-hidden="true" style="font-size:32px;">📄</span>
        <p>暂无匹配记录</p>
      </div>
    </section>

    <div class="blank-area" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, reactive, nextTick } from "vue";
import { useRoute } from "vue-router";
import { useMatchStore } from "@/stores/match";
import { useResumeStore } from "@/stores/resume";
import { jobApi } from "@/api/job";
import type { JobSimple } from "@/types/job";
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
import type { FormInstance, FormRules } from "element-plus";

const matchStore = useMatchStore();
const resumeStore = useResumeStore();
const route = useRoute();

// ===== 表单引用 =====
const formRef = ref<FormInstance>();

// ===== 表单数据 =====
const formData = reactive({
  resumeId: null as number | null,
  matchMode: "jd" as "jd" | "job",
  jdText: "",
  jobId: null as number | null,
});

// ===== 表单验证规则 =====
const formRules: FormRules = {
  resumeId: [
    {
      required: true,
      message: '请选择一份简历',
      trigger: 'change'
    },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('请选择一份简历'));
        } else {
          callback();
        }
      },
      trigger: 'change'
    }
  ],
  matchMode: [
    { required: true, message: '请选择JD来源', trigger: 'change' }
  ],
  jobId: [
    {
      validator: (rule, value, callback) => {
        if (formData.matchMode === 'job' && !value) {
          callback(new Error('请选择一个岗位'));
        } else {
          callback();
        }
      },
      trigger: 'change'
    }
  ],
  jdText: [
    {
      validator: (rule, value, callback) => {
        if (formData.matchMode === 'jd' && !value?.trim()) {
          callback(new Error('请粘贴职位描述'));
        } else if (formData.matchMode === 'jd' && value?.trim().length < 10) {
          callback(new Error('职位描述至少需要10个字符'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// ===== 状态 =====
const resumes = computed(() => resumeStore.list);
const jobs = ref<JobSimple[]>([]);
const isLoadingJobs = ref(false);
const isAnalyzing = ref(false);
const isLoadingHistory = ref(false);
const isViewingDetail = ref(false);
const currentViewingId = ref<number | null>(null);
const showSuggestions = ref(true);
const globalError = ref("");
const formErrors = reactive({
  resumeId: "",
  matchMode: "",
  jdText: "",
  jobId: ""
});

// ===== 计算属性 =====
const hasHighlightsOrWeaknesses = computed(() => {
  return highlights.value.length > 0 || weaknesses.value.length > 0;
});

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

function getJobName(id: number): string {
  const job = jobs.value.find(j => j.id === id);
  return job?.jobName || '';
}

function clearFieldError(field: keyof typeof formErrors) {
  formErrors[field] = "";
}

function clearGlobalError() {
  globalError.value = "";
}

function clearJdText() {
  formData.jdText = "";
  clearFieldError('jdText');
  const textarea = document.getElementById('jd-textarea');
  if (textarea) textarea.focus();
}

function scoreTagClass(score: number): string {
  if (score >= 80) return "score-success";
  if (score >= 60) return "score-warning";
  return "score-danger";
}

function scoreLabel(score: number): string {
  if (score >= 80) return "高度匹配 🎉";
  if (score >= 60) return "部分匹配 💡";
  return "待提升 📈";
}

function suggestionTagClass(priority: string): string {
  const map: Record<string, string> = {
    high: 'suggestion-high',
    medium: 'suggestion-medium',
    low: 'suggestion-low',
  };
  return map[priority] || 'suggestion-medium';
}

async function loadJobs() {
  isLoadingJobs.value = true;
  try {
    const [mine, community] = await Promise.all([
      jobApi.getJobList({ page: 1, size: 100 }),
      jobApi.getCommunityJobs({ page: 1, size: 100 }),
    ]);
    const map = new Map<number, JobSimple>();
    for (const j of mine.content ?? []) {
      map.set(j.id, j);
    }
    for (const j of community.content ?? []) {
      if (!map.has(j.id)) {
        map.set(j.id, {
          id: j.id,
          jobName: j.jobName,
          source: j.source,
          sourceUrls: [],
          createdAt: j.createdAt,
          owner: j.owner,
        });
      }
    }
    jobs.value = Array.from(map.values());
    if (formData.jobId && !map.has(formData.jobId)) {
      formData.jobId = null;
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载岗位列表失败');
  } finally {
    isLoadingJobs.value = false;
  }
}

async function loadHistory() {
  if (isLoadingHistory.value) return;
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
    await nextTick();
    const resultEl = document.querySelector('.result-full');
    if (resultEl) {
      resultEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
      (resultEl as HTMLElement).focus({ preventScroll: true });
    }
  } catch (e: any) {
    const msg = e.message || '加载详情失败';
    ElMessage.error(msg);
    globalError.value = msg;
  } finally {
    isViewingDetail.value = false;
    currentViewingId.value = null;
  }
}

async function onMatch() {
  // 清除旧错误
  clearGlobalError();
  Object.keys(formErrors).forEach(key => {
    formErrors[key as keyof typeof formErrors] = "";
  });

  if (isAnalyzing.value) return;
  if (!formRef.value) return;

  // 验证表单
  try {
    await formRef.value.validate();
  } catch (e: any) {
    // 提取字段错误
    if (e && typeof e === 'object') {
      const errors = e as Record<string, { message: string }[]>;
      for (const [field, fieldErrors] of Object.entries(errors)) {
        if (fieldErrors && fieldErrors.length > 0) {
          const errorKey = field as keyof typeof formErrors;
          if (errorKey in formErrors) {
            formErrors[errorKey] = fieldErrors[0].message;
          }
        }
      }
    }
    // 聚焦到第一个错误
    const firstError = document.querySelector('.el-form-item.is-error input, .el-form-item.is-error .el-select, .el-form-item.is-error .el-textarea');
    if (firstError) {
      (firstError as HTMLElement).focus();
    }
    return;
  }

  isAnalyzing.value = true;
  try {
    if (formData.matchMode === "job" && formData.jobId) {
      await matchStore.runMatchByJob(formData.resumeId!, formData.jobId);
    } else {
      await matchStore.runMatch(formData.resumeId!, formData.jdText);
    }
    ElMessage.success("匹配分析完成！");
    showSuggestions.value = true;
    await nextTick();
    const resultEl = document.querySelector('.result-full');
    if (resultEl) {
      resultEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
      (resultEl as HTMLElement).focus({ preventScroll: true });
    }
  } catch (e: any) {
    const msg = e.message || "匹配分析失败，请重试";
    globalError.value = msg;
    ElMessage.error(msg);
    const errorEl = document.querySelector('.global-error');
    if (errorEl) {
      (errorEl as HTMLElement).focus({ preventScroll: true });
    }
  } finally {
    isAnalyzing.value = false;
  }
}

// ===== 键盘快捷键 =====
const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault();
    onMatch();
  }
};

// ===== 生命周期 =====
onMounted(async () => {
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown);

  await resumeStore.loadList();
  await loadJobs();
  await loadHistory();

  if (resumeStore.list.length) {
    formData.resumeId = resumeStore.list[0].id;
  }

  const queryJobId = Number(route.query.jobId);
  if (queryJobId > 0) {
    formData.matchMode = "job";
    formData.jobId = queryJobId;
  } else if (jobs.value.length) {
    formData.jobId = jobs.value[0].id;
  }
});

// 清理事件监听
import { onBeforeUnmount } from "vue";
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
/* ===== 屏幕阅读器专用类 ===== */
.sr-only {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  border: 0 !important;
  white-space: nowrap !important;
}

/* ===== 页面基础样式 ===== */
.match-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
}

/* ===== 标题区域 ===== */
.hero-section {
  text-align: center;
  margin-bottom: 28px;
  padding: 24px 40px;
  background: #FBFCCD;
  border-radius: 16px;
  border: 2px solid #e8e9b8;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 30px;
  color: #2c4d3d;
  line-height: 1.3;
}

.hero-title span[aria-hidden="true"] {
  margin-right: 4px;
}

.hero-subtitle {
  margin: 0;
  color: #3d5a4b;
  font-size: 16px;
  line-height: 1.7;
}

/* ===== 内容区域 ===== */
.content-section {
  background-color: #ffffff;
  padding: 24px 28px;
  border-radius: 16px;
  margin-bottom: 24px;
  border: 1px solid rgba(100, 163, 134, 0.08);
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.06);
}

/* ===== 上半部分 ===== */
.top-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.form-column {
  min-width: 0;
}

.overview-column {
  min-width: 0;
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
  color: #2c4d3d;
}

.card-subtitle {
  display: block;
  font-size: 13px;
  color: #6b8a7a;
  margin-top: 2px;
}

/* ===== 表单样式 ===== */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c4d3d;
  padding-bottom: 4px;
  font-size: 14px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.1);
}

:deep(.el-form-item.is-error .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.1);
}

:deep(.el-form-item.is-error .el-textarea__inner) {
  border-color: #dc2626;
  box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.1);
}

/* ===== 选择器和输入框 ===== */
.el-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  min-height: 44px;
  box-shadow: 0 0 0 1px #b8ccbf;
  background: #ffffff;
  transition: box-shadow 0.2s ease;
}

.el-select :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4c8a6e, 0 0 0 4px rgba(76, 138, 110, 0.12);
}

.el-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #4c8a6e;
}

.el-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  border-color: #b8ccbf;
  background: #ffffff;
  font-family: inherit;
  line-height: 1.7;
  transition: border-color 0.2s, box-shadow 0.2s;
  min-height: 140px;
  font-size: 14px;
}

.el-textarea :deep(.el-textarea__inner:focus) {
  border-color: #4c8a6e;
  box-shadow: 0 0 0 3px rgba(76, 138, 110, 0.12);
  outline: none;
}

/* ===== 单选按钮组 ===== */
:deep(.el-radio-group) {
  display: flex;
  gap: 4px;
}

:deep(.el-radio-button__inner) {
  border-radius: 8px !important;
  border: 1px solid #b8ccbf !important;
  color: #2c4d3d;
  font-weight: 500;
  padding: 8px 20px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border-color: #4c8a6e !important;
  color: #ffffff;
  box-shadow: none;
}

:deep(.el-radio-button__inner:focus-visible) {
  outline: 2px solid #2c4d3d;
  outline-offset: 2px;
}

/* ===== 表单提示 ===== */
.form-hint {
  font-size: 13px;
  color: #6b8a7a;
  margin-top: 4px;
  padding-left: 2px;
  line-height: 1.5;
}

/* ===== 字段错误 ===== */
.field-error {
  color: #b91c1c;
  font-size: 14px;
  margin-top: 4px;
  padding: 4px 8px;
  background: #fef2f2;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-error span[aria-hidden="true"] {
  font-size: 14px;
}

/* ===== 表单操作按钮 ===== */
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.btn-analyze {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #ffffff;
  border-radius: 10px;
  padding: 12px 36px;
  font-size: 15px;
  font-weight: 500;
  min-width: 140px;
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s, box-shadow 0.2s;
}

.btn-analyze:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(76, 138, 110, 0.25);
}

.btn-analyze:focus-visible {
  outline: 3px solid #2c4d3d;
  outline-offset: 2px;
}

.btn-analyze:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-analyze .el-icon {
  font-size: 18px;
}

/* ===== 键盘提示 ===== */
.keyboard-hint {
  text-align: center;
  margin-top: 10px;
  font-size: 12px;
  color: #6b8a7a;
}

kbd {
  display: inline-block;
  padding: 2px 8px;
  background: #f0f2f5;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  color: #2c4d3d;
  box-shadow: 0 1px 0 #d0d7de;
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
  color: #2c4d3d;
  font-size: 15px;
}

.overview-time {
  font-size: 12px;
  color: #6b8a7a;
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
  color: #6b8a7a;
}

.score-label {
  position: relative;
  z-index: 1;
  font-size: 11px;
  color: #6b8a7a;
  margin-top: 1px;
}

.match-level-tag {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.match-level-tag.score-success {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7d7c5;
}

.match-level-tag.score-warning {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.match-level-tag.score-danger {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fca5a5;
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
  color: #6b8a7a;
}

.overview-placeholder span {
  color: #b8cec2;
  margin-bottom: 8px;
}

.overview-placeholder p {
  margin: 0;
  font-size: 14px;
}

/* ===== 全局错误 ===== */
.global-error {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fef2f2;
  border-radius: 10px;
  border-left: 4px solid #dc2626;
  margin-top: 16px;
  color: #991b1b;
  font-size: 15px;
  flex-wrap: wrap;
}

.global-error:focus-visible {
  outline: 2px solid #dc2626;
  outline-offset: 2px;
}

.error-close-btn {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 20px;
  color: #991b1b;
  cursor: pointer;
  padding: 0 4px;
  min-height: 32px;
  min-width: 32px;
  border-radius: 4px;
}

.error-close-btn:hover {
  background: rgba(220, 38, 38, 0.1);
}

.error-close-btn:focus-visible {
  outline: 2px solid #dc2626;
  outline-offset: 2px;
}

/* ===== 结果区域 ===== */
.result-full {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 2px solid #f0f5f2;
  min-height: 400px;
  outline: none;
}

.result-full:focus-visible {
  outline: 2px solid #4c8a6e;
  outline-offset: 2px;
  border-radius: 4px;
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
  color: #2c4d3d;
}

.version-tag {
  font-size: 12px;
  padding: 2px 12px;
  background: #f0f5f2;
  color: #6b8a7a;
  border-radius: 12px;
  border: 1px solid #dce8e2;
}

/* ===== 分析标签 ===== */
.analysis-tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 6px;
}

.tag-label {
  font-size: 13px;
  font-weight: 500;
  color: #3d5a4b;
  margin-right: 4px;
  padding-top: 2px;
}

.tag-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  display: inline-block;
}

.tag-item-content {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.5;
  border: 1px solid;
}

.tag-item-content.tag-success {
  background: #ecfdf5;
  color: #065f46;
  border-color: #a7d7c5;
}

.tag-item-content.tag-warning {
  background: #fffbeb;
  color: #92400e;
  border-color: #fde68a;
}

/* ===== 优化建议 ===== */
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
  width: 100%;
  padding: 12px 18px;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  font-size: inherit;
  transition: background 0.2s;
  min-height: 48px;
}

.suggestions-toggle:hover {
  background: #f0f7f4;
}

.suggestions-toggle:focus-visible {
  outline: 2px solid #4c8a6e;
  outline-offset: -2px;
}

.suggestions-title {
  font-weight: 600;
  color: #2c4d3d;
  font-size: 14px;
}

.suggestions-icon {
  color: #6b8a7a;
  font-size: 18px;
  display: flex;
  align-items: center;
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
  font-weight: 500;
  padding: 2px 12px;
  border-radius: 12px;
  border: 1px solid;
}

.suggestion-tag.suggestion-high {
  background: #fef2f2;
  color: #991b1b;
  border-color: #fca5a5;
}

.suggestion-tag.suggestion-medium {
  background: #fffbeb;
  color: #92400e;
  border-color: #fde68a;
}

.suggestion-tag.suggestion-low {
  background: #eff6ff;
  color: #1e40af;
  border-color: #93c5fd;
}

.suggestion-desc {
  font-size: 13px;
  color: #3d5a4b;
  line-height: 1.6;
}

.no-suggestions {
  padding: 16px;
  text-align: center;
  color: #6b8a7a;
  font-size: 14px;
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
  color: #6b8a7a;
  margin-top: 8px;
}

.result-full-placeholder span {
  color: #b8cec2;
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
  background-color: #ffffff;
  padding: 20px 28px 24px;
  border-radius: 16px;
  border: 1px solid rgba(100, 163, 134, 0.08);
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.06);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 8px;
}

.history-title {
  font-size: 17px;
  font-weight: 600;
  color: #2c4d3d;
}

.history-count {
  font-size: 13px;
  color: #6b8a7a;
}

/* ===== 自定义表格 ===== */
.table-wrapper {
  overflow-x: auto;
  border-radius: 10px;
  border: 1px solid #e8f0ec;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.history-table thead {
  background: #f0f7f4;
}

.history-table th {
  padding: 10px 16px;
  text-align: left;
  font-weight: 600;
  color: #2c4d3d;
  border-bottom: 1px solid #dce8e2;
}

.history-table td {
  padding: 10px 16px;
  border-bottom: 1px solid #f0f5f2;
  color: #3d5a4b;
}

.history-table tbody tr:hover {
  background: #f8fbf9;
}

.history-table .text-center {
  text-align: center;
}

.resume-name {
  color: #2c4d3d;
}

.score-tag {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 13px;
}

.score-tag.score-success {
  background: #ecfdf5;
  color: #065f46;
}

.score-tag.score-warning {
  background: #fffbeb;
  color: #92400e;
}

.score-tag.score-danger {
  background: #fef2f2;
  color: #991b1b;
}

.view-detail-btn {
  background: none;
  border: none;
  color: #4c8a6e;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 4px;
  min-height: 32px;
  text-decoration: underline;
}

.view-detail-btn:hover:not(:disabled) {
  background: #ecfdf5;
}

.view-detail-btn:focus-visible {
  outline: 2px solid #4c8a6e;
  outline-offset: 2px;
}

.view-detail-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.text-muted {
  color: #b8cec2;
  font-size: 13px;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 36px 0;
  color: #b8cec2;
}

.empty-history span {
  color: #c8d8d2;
  margin-bottom: 6px;
}

.empty-history p {
  margin: 0;
  font-size: 14px;
}

.blank-area {
  width: 100%;
  min-height: 40px;
}

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .top-row {
    grid-template-columns: 1fr;
  }

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

  .btn-analyze {
    width: 100%;
    justify-content: center;
  }

  .form-actions {
    flex-direction: column;
  }

  .history-table {
    font-size: 13px;
  }

  .history-table th,
  .history-table td {
    padding: 8px 10px;
  }

  .result-full {
    min-height: 300px;
  }

  :deep(.el-radio-button__inner) {
    padding: 6px 14px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 19px;
  }

  .score-ring {
    width: 72px;
    height: 72px;
  }

  .score-value {
    font-size: 1.3rem;
  }

  .history-header {
    flex-direction: column;
    align-items: stretch;
  }
}

/* ===== 高对比度模式 ===== */
@media (prefers-contrast: high) {
  .hero-section {
    border: 3px solid #1a4d36;
  }

  .content-section {
    border: 2px solid #2c4d3d;
  }

  .history-section {
    border: 2px solid #2c4d3d;
  }

  .overview-card {
    border: 2px solid #2c4d3d;
  }

  .el-select :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 2px #2c4d3d !important;
  }

  .el-textarea :deep(.el-textarea__inner) {
    border: 2px solid #2c4d3d;
  }

  .btn-analyze {
    background: #2c6b4f;
    border: 2px solid #1a4d36;
  }

  .btn-analyze:hover:not(:disabled) {
    background: #1a4d36;
  }

  .suggestions-collapse {
    border: 2px solid #2c4d3d;
  }

  .suggestions-toggle {
    border-bottom: 1px solid #2c4d3d;
  }

  .history-table {
    border: 2px solid #2c4d3d;
  }

  .history-table thead {
    background: #e8f0ec;
  }

  .history-table th {
    border-bottom: 2px solid #2c4d3d;
  }
}

/* ===== 减少动画 ===== */
@media (prefers-reduced-motion: reduce) {
  .match-page {
    animation: none;
  }

  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}

/* ===== 打印样式 ===== */
@media print {
  .match-page {
    background: white !important;
    padding: 20px;
  }

  .hero-section {
    border: 1px solid #ccc !important;
    background: #f9f9f9 !important;
  }

  .content-section,
  .history-section {
    border: 1px solid #ccc !important;
    box-shadow: none !important;
  }

  .btn-analyze,
  .view-detail-btn,
  .suggestions-toggle,
  .error-close-btn {
    display: none !important;
  }

  .blank-area {
    display: none !important;
  }
}
</style>