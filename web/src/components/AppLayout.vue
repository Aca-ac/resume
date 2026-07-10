<!-- src/components/AppLayout.vue -->
<template>
  <el-container class="layout">
    <el-aside width="240px" class="aside">
      <div class="brand">
        <span class="brand-icon">📄</span>
        <span>简历助手</span>
      </div>

      <!-- 导航菜单 -->
      <el-menu
          :default-active="$route.path"
          :default-openeds="['resume-group']"
          class="sidebar-menu"
          router
          unique-opened
          background-color="#1e1b40"
          text-color="#c8d0ff"
          active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>仪表板</span>
        </el-menu-item>

        <!-- 一级菜单：简历管理 二级三个选项 -->
        <el-sub-menu index="resume-group">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>简历管理</span>
          </template>
          <el-menu-item index="/resumes">简历列表</el-menu-item>
          <el-menu-item index="/resumes/new">简历创建</el-menu-item>
          <el-menu-item index="/resumes/import">简历导入</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/match">
          <el-icon><Link /></el-icon>
          <span>职位匹配</span>
        </el-menu-item>

        <el-menu-item index="/interview/start">
          <el-icon><ChatDotRound /></el-icon>
          <span>面试练习</span>
        </el-menu-item>
      </el-menu>

    </el-aside>

    <el-container class="main-wrap">
      <el-header class="header">
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-actions">
          <!-- 通知按钮 -->
          <el-badge :value="3" :hidden="true" class="notification-badge">
            <el-button link><el-icon><Bell /></el-icon></el-button>
          </el-badge>

          <!-- 用户头像 - 右上角 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="header-user">
              <el-avatar :size="36" class="avatar">{{ avatarLetter }}</el-avatar>
              <span class="user-name">{{ userDisplayName }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide">
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
import {
  DataBoard,
  Document,
  Connection,
  ChatDotRound,
  User,
  SwitchButton,
  ArrowDown,
  Bell
} from "@element-plus/icons-vue";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const navItems = [
  { path: "/dashboard", label: "仪表板", icon: DataBoard },
  { path: "/resumes", label: "简历管理", icon: Document },
  { path: "/match", label: "职位匹配", icon: Connection },
  { path: "/interview/start", label: "面试练习", icon: ChatDotRound }
];

const titleMap: Record<string, string> = {
  "/dashboard": "仪表板",
  "/resumes": "我的简历",
  "/match": "职位匹配",
  "/interview/start": "模拟面试"
};

const currentTitle = computed(() => {
  const path = route.path;
  if (path.startsWith("/resumes/") && path.endsWith("/edit")) return "编辑简历";
  if (path.startsWith("/resumes/") && path.endsWith("/optimize")) return "AI 优化";
  if (path.startsWith("/interview/") && path.endsWith("/chat")) return "面试对话";
  if (path.startsWith("/interview/") && path.endsWith("/report")) return "面试报告";
  if (path === "/profile") return "个人中心";
  return titleMap[path] ?? "简历助手";
});

// 用户显示名称
const userDisplayName = computed(() => {
  const user = auth.userInfo;
  if (user?.name) return user.name;
  if (user?.nickname) return user.nickname;
  return "用户";
});

// 用户头像首字母
const avatarLetter = computed(() => {
  const user = auth.userInfo;
  const name = user?.name || user?.nickname || "用户";
  return name.charAt(0).toUpperCase();
});

// 下拉菜单命令处理
const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile');
  } else if (command === 'logout') {
    auth.logout();
    router.push('/login');
  }
};
</script>

<style scoped>
.layout {
  min-height: 100vh;
  height: 100vh;
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.aside {
  background: #1a1a2e;
  color: #fff;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  width: 240px !important;
  flex-shrink: 0;
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
  flex-shrink: 0;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-size: 16px;
}

.nav-menu {
  border-right: none !important;
  padding: 12px 8px;
  flex: 1;
  overflow-y: auto;
  background: transparent !important;
}

.nav-menu :deep(.el-menu-item) {
  border-radius: 10px;
  margin-bottom: 4px;
  transition: all 0.25s ease;
  height: 44px;
  line-height: 44px;
  color: #b8c5d6;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  transform: translateX(4px);
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.35), rgba(118, 75, 162, 0.2)) !important;
  box-shadow: inset 3px 0 0 #667eea;
  color: #fff !important;
}

.nav-menu :deep(.el-icon) {
  margin-right: 8px;
}

.nav-menu::-webkit-scrollbar {
  width: 4px;
}

.nav-menu::-webkit-scrollbar-track {
  background: transparent;
}

.nav-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

/* ========== 主内容区 ========== */
.main-wrap {
  background: #f0f2f5;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* ========== 顶部栏 ========== */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  padding: 0 24px;
  height: 64px;
  flex-shrink: 0;
}

.header-title {
  font-weight: 600;
  font-size: 1.15rem;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 用户头像 - 右上角 */
.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  transition: background 0.3s ease;
}

.header-user:hover {
  background: #f0f2f5;
}

.header-user .user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
}

.header-user .dropdown-icon {
  color: #909399;
  font-size: 14px;
  transition: transform 0.3s ease;
}

.header-user:hover .dropdown-icon {
  color: #606266;
}

.avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

/* 通知按钮 */
.notification-badge :deep(.el-badge__content) {
  background: #f56c6c;
}

/* ========== 下拉菜单 ========== */
:deep(.el-dropdown-menu) {
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  min-width: 140px;
  margin-top: 8px !important;
}

:deep(.el-dropdown-menu .el-dropdown-item) {
  border-radius: 8px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

:deep(.el-dropdown-menu .el-dropdown-item:hover) {
  background: #f0f2f5;
}

:deep(.el-dropdown-menu .el-dropdown-item .el-icon) {
  font-size: 16px;
}

:deep(.el-dropdown-menu .el-dropdown-item.is-disabled) {
  color: #c0c4cc;
  cursor: not-allowed;
}

/* ========== 主内容 ========== */
.main {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.main::-webkit-scrollbar {
  width: 6px;
}

.main::-webkit-scrollbar-track {
  background: transparent;
}

.main::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.main::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* ========== 页面切换动画 ========== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.page-content {
  min-height: 100%;
}

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .aside {
    width: 60px !important;
  }

  .brand span:not(.brand-icon) {
    display: none;
  }

  .nav-menu :deep(.el-menu-item span) {
    display: none;
  }

  .nav-menu :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 12px !important;
  }

  .nav-menu :deep(.el-menu-item .el-icon) {
    margin-right: 0;
    font-size: 20px;
  }

  .header-user .user-name,
  .header-user .dropdown-icon {
    display: none;
  }

  .header-user {
    padding: 4px;
  }
}
</style>