<!-- src/views/Dashboard.vue -->
<template>
  <div class="dashboard">
    <h2 class="page-title">欢迎回来 👋</h2>
    <p class="subtitle">您的 AI 驱动职业助手</p>
    <el-row :gutter="20" class="cards">
      <el-col v-for="card in cards" :key="card.path" :span="8">
        <el-card
            class="feature-card hover-lift"
            shadow="hover"
            @click="$router.push(card.path)"
        >
          <div class="card-icon" :style="{ background: card.gradient }">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
          <el-button type="primary" plain class="card-btn">进入</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { Document, Connection, ChatDotRound } from "@element-plus/icons-vue";

const cards = [
  {
    title: "简历管理",
    desc: "创建、编辑并使用 AI 优化您的简历",
    path: "/resumes",
    icon: Document,
    gradient: "linear-gradient(135deg, #667eea, #764ba2)"
  },
  {
    title: "职位匹配",
    desc: "将您的简历与职位描述进行智能匹配分析",
    path: "/match",
    icon: Connection,
    gradient: "linear-gradient(135deg, #f093fb, #f5576c)"
  },
  {
    title: "面试练习",
    desc: "与 AI 面试官进行模拟面试并获得反馈",
    path: "/interview/start",
    icon: ChatDotRound,
    gradient: "linear-gradient(135deg, #4facfe, #00f2fe)"
  }
];
</script>

<style scoped>
.dashboard {
  max-width: 1100px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.subtitle {
  margin: -4px 0 32px;
  color: #909399;
  font-size: 16px;
}

.feature-card {
  cursor: pointer;
  border-radius: 16px;
  border: none;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
}

.feature-card:hover .card-icon {
  transform: scale(1.08) rotate(-3deg);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 16px;
  transition: transform 0.3s ease;
}

.feature-card h3 {
  margin: 0 0 8px;
  font-size: 1.15rem;
  color: #1a1a2e;
}

.feature-card p {
  margin: 0 0 16px;
  color: #909399;
  font-size: 0.9rem;
  line-height: 1.5;
}

.card-btn {
  transition: transform 0.2s ease;
}

.feature-card:hover .card-btn {
  transform: translateX(4px);
}
</style>