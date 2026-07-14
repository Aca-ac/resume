<template>
  <div class="template-gallery">
    <section class="hero">
      <h1>模板广场</h1>
      <p>选择风格模板，预览后可在导出时使用对应 templateId。</p>
    </section>

    <section class="toolbar">
      <el-radio-group v-model="category" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="简约">简约</el-radio-button>
        <el-radio-button label="专业">专业</el-radio-button>
        <el-radio-button label="创意">创意</el-radio-button>
        <el-radio-button label="学术">学术</el-radio-button>
      </el-radio-group>
      <el-button type="primary" plain :loading="recLoading" @click="loadRecommend">智能推荐</el-button>
    </section>


    <section v-loading="loading" class="grid">
      <div
        v-for="item in cards"
        :key="item.id"
        class="card"
        @click="goPreview(item.id)"
      >
        <img class="thumb" :src="previewSrc(item.previewUrl)" :alt="item.name" />
        <div class="meta">
          <div class="title">{{ item.name }}</div>
          <div class="sub">{{ item.category }} · {{ item.applicableScene || "通用" }}</div>
          <div v-if="item.reason" class="reason">推荐：{{ item.reason }}（{{ item.score }}）</div>
        </div>
      </div>
      <el-empty v-if="!loading && cards.length === 0" description="暂无模板" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  fetchTemplates,
  previewSrc,
  recommendTemplates,
  type TemplateRecommendVO,
  type TemplateVO
} from "@/api/template";

type Card = TemplateVO & { score?: number; reason?: string };

const router = useRouter();
const category = ref("");
const loading = ref(false);
const recLoading = ref(false);
const cards = ref<Card[]>([]);

async function load() {
  loading.value = true;
  try {
    const data = await fetchTemplates(category.value || undefined);
    cards.value = data?.records ?? [];
  } catch (e: any) {
    ElMessage.error(e.message || "加载模板失败");
  } finally {
    loading.value = false;
  }
}

async function loadRecommend() {
  recLoading.value = true;
  try {
    const list = (await recommendTemplates({})) as TemplateRecommendVO[];
    cards.value = (list || []).map((r) => ({
      ...r.template,
      score: r.score,
      reason: r.reason
    }));
  } catch (e: any) {
    ElMessage.error(e.message || "推荐失败");
  } finally {
    recLoading.value = false;
  }
}

function goPreview(id: number) {
  router.push({ path: "/resume/preview", query: { templateId: String(id) } });
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
}
.hero p {
  margin: 0 0 20px;
  color: #666;
}
.toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
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
