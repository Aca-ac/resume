<template>
  <div class="template-gallery">
    <section class="hero">
      <h1>模板广场</h1>
      <p>选择风格模板，预览后选择简历导出 Word / PDF。</p>
      <el-alert
        v-if="route.query.resumeId"
        type="info"
        :closable="false"
        show-icon
        class="resume-hint"
        :title="`已选定简历 #${route.query.resumeId}，点击模板进入导出页`"
      />
    </section>

    <section class="toolbar">
      <el-radio-group v-model="category" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="简约">简约</el-radio-button>
        <el-radio-button label="专业">专业</el-radio-button>
        <el-radio-button label="创意">创意</el-radio-button>
        <el-radio-button label="学术">学术</el-radio-button>
      </el-radio-group>
    </section>

    <section class="recommend-panel">
      <div class="panel-title">智能推荐（可选填画像）</div>
      <el-form :inline="true" @submit.prevent="loadRecommend">
        <el-form-item label="学历">
          <el-input v-model="recommendForm.education" placeholder="本科 / 应届" clearable />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="recommendForm.industry" placeholder="互联网 / 金融" clearable />
        </el-form-item>
        <el-form-item label="年限">
          <el-input v-model="recommendForm.workYears" placeholder="0 / 3年" clearable />
        </el-form-item>
        <el-form-item label="目标岗位">
          <el-input v-model="recommendForm.targetPosition" placeholder="Java开发 / UI设计师" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="recLoading" @click="loadRecommend">智能推荐</el-button>
          <el-button @click="resetRecommend">恢复列表</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section v-loading="loading" class="grid">
      <div
        v-for="item in cards"
        :key="item.id"
        class="card"
        @click="goPreview(item.id)"
      >
        <div class="thumb-wrap">
          <img class="thumb" :src="previewSrc(item.previewUrl)" :alt="item.name" />
          <el-tag v-if="item.recommended" class="badge" type="warning" effect="dark" size="small">
            推荐
          </el-tag>
        </div>
        <div class="meta">
          <div class="title">{{ item.name }}</div>
          <div class="sub">{{ item.category }} · {{ item.applicableScene || "通用" }}</div>
          <div v-if="item.reason" class="reason">{{ item.reason }}（{{ item.score }}）</div>
        </div>
      </div>
      <el-empty v-if="!loading && cards.length === 0" description="暂无模板" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  fetchTemplates,
  previewSrc,
  recommendTemplates,
  type TemplateVO
} from "@/api/template";

type Card = TemplateVO & { score?: number; reason?: string; recommended?: boolean };

const router = useRouter();
const route = useRoute();
const category = ref("");
const loading = ref(false);
const recLoading = ref(false);
const cards = ref<Card[]>([]);
const recommendForm = reactive({
  education: "",
  industry: "",
  workYears: "",
  targetPosition: ""
});

async function load() {
  loading.value = true;
  try {
    const data = await fetchTemplates(category.value || undefined);
    cards.value = (data?.records ?? []).map((t) => ({ ...t }));
  } catch (e: any) {
    ElMessage.error(e.message || "加载模板失败");
  } finally {
    loading.value = false;
  }
}

async function loadRecommend() {
  recLoading.value = true;
  try {
    const list = await recommendTemplates({ ...recommendForm });
    const sorted = [...(list || [])].sort((a, b) => b.score - a.score);
    cards.value = sorted.map((r, idx) => ({
      ...r.template,
      score: r.score,
      reason: r.reason,
      recommended: idx === 0
    }));
    if (!cards.value.length) {
      ElMessage.info("暂无推荐结果，请调整条件或浏览全部模板");
    }
  } catch (e: any) {
    ElMessage.error(e.message || "推荐失败");
  } finally {
    recLoading.value = false;
  }
}

function resetRecommend() {
  recommendForm.education = "";
  recommendForm.industry = "";
  recommendForm.workYears = "";
  recommendForm.targetPosition = "";
  load();
}

function goPreview(id: number) {
  const query: Record<string, string> = { templateId: String(id) };
  const resumeId = route.query.resumeId;
  if (resumeId) query.resumeId = String(resumeId);
  router.push({ path: "/resume/preview", query });
}

onMounted(load);
</script>

<style scoped>
.template-gallery {
  padding: 24px 32px 48px;
}
.hero h1 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #4a7a64;
}
.hero p {
  margin: 0 0 20px;
  color: #666;
}
.resume-hint {
  margin-bottom: 16px;
}
.toolbar {
  margin-bottom: 16px;
}
.recommend-panel {
  margin-bottom: 24px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  border: 1px solid #e8ecea;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #3d6b57;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  min-height: 160px;
}
.card {
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  transition: box-shadow 0.2s;
}
.card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.thumb-wrap {
  position: relative;
}
.badge {
  position: absolute;
  top: 10px;
  left: 10px;
}
.thumb {
  width: 100%;
  height: 280px;
  object-fit: cover;
  background: #f5f5f5;
  display: block;
}
.meta {
  padding: 12px 14px 16px;
}
.title {
  font-weight: 600;
  margin-bottom: 4px;
}
.sub,
.reason {
  font-size: 13px;
  color: #888;
}
.reason {
  margin-top: 6px;
  color: #3a7afe;
}
</style>
