<template>
  <el-container class="layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <!-- Logo区域（左侧） -->
      <div class="header-left">
        <div class="logo" @click="navigateTo('/dashboard')">
          <span class="logo-text">AI简历</span>
        </div>
      </div>

      <!-- 导航菜单（居中） -->
      <div class="header-center">
        <div class="nav-menu">
          <!-- 首页 -->
          <div
              class="nav-item"
              :class="{ 'is-active': $route.path === '/dashboard' }"
              @click="navigateTo('/dashboard')"
          >
            首页
          </div>

          <!-- 简历制作 -->
          <el-dropdown
              trigger="click"
              @command="handleResumeCommand"
              @visible-change="onResumeDropdownVisible"
              class="dropdown-trigger"
          >
            <div
                :class="['nav-item', {
                'is-active': isResumeActive,
                'is-open': resumeDropdownOpen
              }]"
                @click="toggleResumeDropdown"
            >
              简历制作
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="list">简历列表</el-dropdown-item>
                <el-dropdown-item command="new">简历制作</el-dropdown-item>
                <el-dropdown-item command="import">简历导入</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- AI简历分析 -->
          <div
              class="nav-item"
              :class="{ 'is-active': $route.path === '/match' }"
              @click="navigateTo('/match')"
          >
            AI简历分析
          </div>

          <!-- AI面试练习 -->
          <div
              class="nav-item"
              :class="{ 'is-active': $route.path.startsWith('/interview') }"
              @click="navigateTo('/interview/start')"
          >
            AI面试练习
          </div>
        </div>
      </div>

      <!-- 右侧用户区域 -->
      <div class="header-right">
        <!-- 通知按钮 -->
        <el-badge
            v-if="auth.isLoggedIn"
            :value="3"
            :hidden="true"
            class="notification-badge"
        >
          <el-button link><el-icon><Bell /></el-icon></el-button>
        </el-badge>

        <!-- 用户下拉 / 登录按钮 -->
        <el-dropdown
            v-if="auth.isLoggedIn"
            trigger="click"
            @command="handleUserCommand"
        >
          <div class="header-user">
            <el-avatar :size="32" class="avatar">{{ avatarLetter }}</el-avatar>
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

        <!-- 未登录显示登录按钮 -->
        <el-button v-else @click="goToLogin" class="login-btn">
          登录 / 注册
        </el-button>
      </div>
    </el-header>

    <!-- 内容区 -->
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from 'element-plus';
import {
  User,
  SwitchButton,
  ArrowDown,
  Bell
} from "@element-plus/icons-vue";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const resumeDropdownOpen = ref(false);

// 判断简历相关路由是否激活
const isResumeActive = computed(() => {
  return route.path.startsWith('/resumes');
});

// 导航跳转
const navigateTo = (path: string) => {
  const authRequiredPaths = ['/resumes', '/match', '/interview'];
  const needsAuth = authRequiredPaths.some(p => path.startsWith(p));

  if (needsAuth && !auth.isLoggedIn) {
    ElMessage.warning('请先登录再访问此功能');
    router.push('/login');
    return;
  }

  router.push(path);
};

// 切换简历下拉菜单
const toggleResumeDropdown = () => {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录再访问简历功能');
    router.push('/login');
    return;
  }
  resumeDropdownOpen.value = !resumeDropdownOpen.value;
};

// 简历下拉菜单命令
const handleResumeCommand = (command: string) => {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录再访问简历功能');
    router.push('/login');
    return;
  }

  const pathMap: Record<string, string> = {
    list: '/resumes',
    new: '/resumes/new',
    import: '/resumes/import'
  };
  router.push(pathMap[command]);
  resumeDropdownOpen.value = false;
};

// 简历下拉显示状态
const onResumeDropdownVisible = (visible: boolean) => {
  if (auth.isLoggedIn) {
    resumeDropdownOpen.value = visible;
  }
};

// 用户下拉菜单命令
const handleUserCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile');
  } else if (command === 'logout') {
    auth.logout();
    router.push('/dashboard');
    ElMessage.success('已退出登录');
  }
};

// 跳转到登录
const goToLogin = () => {
  router.push('/login');
};

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
</script>

<style scoped>
.layout {
  min-height: 100vh;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ========== 顶部栏 ========== */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 50%, #FBFCCD 100%);
  padding: 0 32px;
  height: 64px;
  flex-shrink: 0;
  gap: 20px;
  box-shadow: 0 2px 12px rgba(100, 163, 134, 0.15);
}

/* ========== 左侧 Logo ========== */
.header-left {
  flex: 0 0 auto;
  min-width: 100px;
}

.logo {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #64A386;
  letter-spacing: 1px;
}

.logo-text::before {
  content: "✦ ";
  color: #64A386;
}

/* ========== 居中导航 ========== */
.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(10px);
  padding: 4px 6px;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 8px rgba(100, 163, 134, 0.08);
}


.nav-item {
  padding: 8px 22px;
  border-radius: 24px;
  cursor: pointer;
  color: #4a6a5a;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  height: 40px;
  line-height: 40px;
  white-space: nowrap;
  letter-spacing: 0.3px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.5);
  color: #3d5a4d;
  transform: translateY(-1px);
}

.nav-item.is-active {
  background: rgba(100, 163, 134, 0.25);
  color: #3d7a5e;
  box-shadow: 0 2px 8px rgba(100, 163, 134, 0.12);
}

.nav-item.is-open {
  background: rgba(255, 255, 255, 0.5);
  color: #3d5a4d;
}

.dropdown-arrow {
  font-size: 12px;
  transition: transform 0.3s ease;
}

.nav-item.is-open .dropdown-arrow {
  transform: rotate(180deg);
}

.dropdown-trigger {
  user-select: none;
}

:deep(.el-dropdown) {
  display: flex;
  align-items: center;
}

:deep(.el-dropdown__popper) {
  margin-top: 8px !important;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.15);
  min-width: 140px;
  background: #fff;
  border: 1px solid rgba(100, 163, 134, 0.1);
}

:deep(.el-dropdown-menu) {
  padding: 4px;
}

:deep(.el-dropdown-menu .el-dropdown-item) {
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: #4a6a5a;
}

:deep(.el-dropdown-menu .el-dropdown-item:hover) {
  background: #FBFCCD;
  color: #3d7a5e;
}

/* ========== 右侧操作区 ========== */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  min-width: 120px;
  justify-content: flex-end;
}

.notification-badge :deep(.el-badge__content) {
  background: #64A386;
  border: none;
}

:deep(.el-button.is-link) {
  color: #4a6a5a;
  font-size: 20px;
}
:deep(.el-button.is-link:hover) {
  color: #3d7a5e;
}

.login-btn {
  background: rgba(100, 163, 134, 0.15);
  border: 1px solid rgba(100, 163, 134, 0.3);
  color: #3d7a5e;
  border-radius: 24px;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
  height: 38px;
}

.login-btn:hover {
  background: rgba(100, 163, 134, 0.25);
  border-color: rgba(100, 163, 134, 0.5);
  color: #2d5a3e;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.header-user:hover {
  background: rgba(255, 255, 255, 0.4);
}

.header-user .user-name {
  font-size: 14px;
  color: #3d5a4d;
  font-weight: 500;
  white-space: nowrap;
}

.header-user .dropdown-icon {
  color: #64A386;
  font-size: 12px;
  transition: transform 0.3s ease;
}

.header-user:hover .dropdown-icon {
  color: #3d7a5e;
}

.avatar {
  background: linear-gradient(135deg, #64A386 0%, #BCDDBE 100%);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

:deep(.el-dropdown-menu) {
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.12);
  min-width: 140px;
  margin-top: 8px !important;
  border: 1px solid rgba(100, 163, 134, 0.08);
}

:deep(.el-dropdown-menu .el-dropdown-item) {
  border-radius: 8px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #4a6a5a;
}

:deep(.el-dropdown-menu .el-dropdown-item:hover) {
  background: #FBFCCD;
  color: #3d7a5e;
}

:deep(.el-dropdown-menu .el-dropdown-item .el-icon) {
  font-size: 16px;
  color: #64A386;
}

:deep(.el-divider--horizontal) {
  margin: 6px 0;
  border-color: rgba(100, 163, 134, 0.1);
}

/* ========== 主内容 ========== */
.main {
  padding:0;
  flex: 1;
  overflow-y: auto;
  background: #f5f8f6;
}

.main::-webkit-scrollbar {
  width: 6px;
}

.main::-webkit-scrollbar-track {
  background: transparent;
}

.main::-webkit-scrollbar-thumb {
  background: #BCDDBE;
  border-radius: 3px;
}

.main::-webkit-scrollbar-thumb:hover {
  background: #64A386;
}

/* ========== 移动端适配 ========== */
@media (max-width: 820px) {
  .header {
    padding: 0 16px;
    gap: 12px;
    height: 60px;
  }

  .nav-menu {
    gap: 4px;
    padding: 3px 4px;
  }

  .nav-item {
    padding: 6px 14px;
    font-size: 13px;
    height: 34px;
    line-height: 34px;
  }

  .header-left {
    min-width: 60px;
  }

  .logo-text {
    font-size: 16px;
  }

  .header-right {
    min-width: 60px;
  }

  .header-user .user-name {
    display: none;
  }

  .header-user {
    padding: 4px;
  }

  .header-user .dropdown-icon {
    display: none;
  }

  .login-btn {
    padding: 6px 14px;
    font-size: 13px;
    height: 34px;
  }
}

@media (max-width: 600px) {
  .header {
    padding: 0 10px;
    gap: 8px;
    height: 56px;
    flex-wrap: nowrap;
  }

  .header-left {
    min-width: 40px;
  }

  .logo-text {
    font-size: 14px;
  }

  .nav-menu {
    gap: 2px;
    padding: 2px 3px;
    border-radius: 20px;
  }

  .nav-item {
    padding: 4px 10px;
    font-size: 11px;
    height: 28px;
    line-height: 28px;
    border-radius: 16px;
  }

  .header-right {
    min-width: 40px;
    gap: 6px;
  }

  :deep(.el-button.is-link) {
    font-size: 16px;
  }

  .avatar {
    width: 28px !important;
    height: 28px !important;
    font-size: 12px;
  }

  .login-btn {
    padding: 4px 10px;
    font-size: 11px;
    height: 28px;
    border-radius: 16px;
  }

  .dropdown-arrow {
    display: none;
  }
}

@media (max-width: 420px) {
  .header {
    padding: 0 6px;
    gap: 4px;
    height: 50px;
  }

  .logo-text {
    font-size: 12px;
  }

  .nav-item {
    padding: 3px 7px;
    font-size: 10px;
    height: 24px;
    line-height: 24px;
    border-radius: 12px;
  }

  .login-btn {
    padding: 3px 8px;
    font-size: 10px;
    height: 24px;
  }

  .avatar {
    width: 24px !important;
    height: 24px !important;
    font-size: 10px;
  }
}
</style>
