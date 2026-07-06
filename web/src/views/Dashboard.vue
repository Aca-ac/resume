<template>
  <div class="dashboard">
    <h2 class="page-title animate-fade-up">Welcome back</h2>
    <p class="subtitle animate-fade-up stagger-1">Your AI-powered career toolkit</p>
    <el-row :gutter="20" class="cards">
      <el-col v-for="(card, i) in cards" :key="card.path" :span="8">
        <el-card
          class="feature-card hover-lift animate-fade-up"
          :class="`stagger-${i + 2}`"
          shadow="hover"
          @click="$router.push(card.path)"
        >
          <div class="card-icon" :style="{ background: card.gradient }">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
          <el-button type="primary" plain class="card-btn">Open →</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { Document, Connection, ChatDotRound } from "@element-plus/icons-vue";

const cards = [
  {
    title: "Resumes",
    desc: "Create, edit and AI-optimize your CV.",
    path: "/resumes",
    icon: Document,
    gradient: "linear-gradient(135deg, #667eea, #764ba2)"
  },
  {
    title: "Job Match",
    desc: "Compare your resume against any job description.",
    path: "/match",
    icon: Connection,
    gradient: "linear-gradient(135deg, #f093fb, #f5576c)"
  },
  {
    title: "Interview",
    desc: "Practice with an AI interviewer and get feedback.",
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

.subtitle {
  margin: -12px 0 28px;
  color: #909399;
  animation-delay: 80ms;
}

.feature-card {
  cursor: pointer;
  border-radius: var(--app-radius);
  border: none;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s ease;
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