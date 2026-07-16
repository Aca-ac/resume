<template>
  <div class="history-page">
    <section class="hero">
      <h1>面试历史</h1>
      <p>查看过往模拟面试记录与复盘报告</p>
      <div class="hero-actions">
        <el-button type="primary" @click="router.push('/interview/start')">开始新面试</el-button>
      </div>
    </section>

    <el-table v-loading="loading" :data="interview.history" stripe style="width: 100%">
      <el-table-column prop="id" label="会话ID" width="100" />
      <el-table-column prop="jobTitle" label="目标职位" min-width="160" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          {{ row.state || row.status }}
        </template>
      </el-table-column>
      <el-table-column label="进度" width="120">
        <template #default="{ row }">
          {{ row.questionIndex || 0 }}/{{ row.maxQuestions || 5 }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="开始时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/interview/${row.id}/chat`)">
            继续/查看
          </el-button>
          <el-button link type="success" @click="router.push(`/interview/${row.id}/report`)">
            报告
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="size"
        :current-page="page"
        :total="interview.historyTotal"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { ElMessage } from "element-plus";

const router = useRouter();
const interview = useInterviewStore();
const loading = ref(false);
const page = ref(1);
const size = 10;

async function load() {
  loading.value = true;
  try {
    await interview.loadHistory(page.value, size);
  } catch (e: any) {
    ElMessage.error(e.message || "加载历史失败");
  } finally {
    loading.value = false;
  }
}

function onPage(p: number) {
  page.value = p;
  load();
}

onMounted(load);
</script>

<style scoped>
.history-page {
  padding: 24px 32px 48px;
}
.hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #3d5a4b;
}
.hero p {
  margin: 0 0 16px;
  color: #666;
}
.hero-actions {
  margin-bottom: 20px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
