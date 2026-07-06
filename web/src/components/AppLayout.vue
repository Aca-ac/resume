<template>
  <el-container class="layout">
    <el-aside width="240px" class="aside">
      <div class="brand">
        <span class="brand-icon">✦</span>
        <span>Resume Assistant</span>
      </div>
      <el-menu
        router
        :default-active="$route.path"
        class="nav-menu"
        background-color="transparent"
        text-color="#b8c5d6"
        active-text-color="#fff"
      >
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path" class="nav-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="main-wrap">
      <el-header class="header">
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-user">
          <el-avatar :size="32" class="avatar">{{ avatarLetter }}</el-avatar>
          <span>{{ auth.username }}</span>
          <el-button link type="danger" @click="onLogout">Logout</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.path" class="page-content" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { DataBoard, Document, Connection, ChatDotRound } from "@element-plus/icons-vue";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
auth.load();
const router = useRouter();
const route = useRoute();

const navItems = [
  { path: "/dashboard", label: "Dashboard", icon: DataBoard },
  { path: "/resumes", label: "Resumes", icon: Document },
  { path: "/match", label: "Job Match", icon: Connection },
  { path: "/interview/start", label: "Interview", icon: ChatDotRound }
];

const titleMap: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/resumes": "My Resumes",
  "/match": "Job Match",
  "/interview/start": "Mock Interview"
};

const currentTitle = computed(() => {
  const path = route.path;
  if (path.startsWith("/resumes/") && path.endsWith("/edit")) return "Edit Resume";
  if (path.startsWith("/resumes/") && path.endsWith("/optimize")) return "AI Optimize";
  if (path.startsWith("/interview/") && path.endsWith("/chat")) return "Interview Chat";
  if (path.startsWith("/interview/") && path.endsWith("/report")) return "Interview Report";
  return titleMap[path] ?? "Resume Assistant";
});

const avatarLetter = computed(() => (auth.username?.[0] ?? "U").toUpperCase());

function onLogout() {
  auth.logout();
  router.push("/login");
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: var(--app-sidebar);
  color: #fff;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);
  animation: slideInLeft 0.4s ease both;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px;
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: 0.02em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--app-gradient);
  font-size: 14px;
  animation: pulse-soft 3s ease-in-out infinite;
}

.nav-menu {
  border-right: none !important;
  padding: 12px 8px;
}

.nav-menu :deep(.el-menu-item) {
  border-radius: 10px;
  margin-bottom: 4px;
  transition: all 0.25s ease;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  transform: translateX(4px);
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.35), rgba(118, 75, 162, 0.2)) !important;
  box-shadow: inset 3px 0 0 #667eea;
}

.nav-menu :deep(.el-icon) {
  margin-right: 8px;
}

.main-wrap {
  background: #f0f2f5;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  animation: fadeIn 0.4s ease both;
}

.header-title {
  font-weight: 600;
  font-size: 1.1rem;
  color: #303133;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  background: var(--app-gradient);
  color: #fff;
  font-weight: 600;
}

.main {
  padding: 24px;
}

.page-content {
  animation: fadeUp 0.4s ease both;
}
</style>