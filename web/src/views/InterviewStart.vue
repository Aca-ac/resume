// InterviewStart.vue
<template>
  <div class="start-page">
    <!-- 顶部导航栏 - 与仪表盘保持一致 -->
    <header class="top-header">
      <div class="header-left"></div>

      <!-- 中间：功能按钮组（简历管理带下拉） -->
      <div class="header-center">
        <!-- 简历管理 - 带下拉菜单 -->
        <div class="dropdown-wrapper">
          <div class="transparent-btn" @click="toggleResumeDropdown">
            简历管理
            <el-icon :size="12" class="dropdown-arrow" :class="{ rotated: resumeDropdownOpen }">
              <ArrowDown />
            </el-icon>
          </div>
          <div v-show="resumeDropdownOpen" class="dropdown-menu" @mouseleave="closeResumeDropdown">
            <div class="dropdown-item" @click="goResumes">
              <el-icon><Document /></el-icon>
              <span>我的简历</span>
            </div>
            <div class="dropdown-item" @click="goResumeCreate">
              <el-icon><Plus /></el-icon>
              <span>新建简历</span>
            </div>
            <div class="dropdown-item" @click="goResumeTemplates">
              <el-icon><Files /></el-icon>
              <span>模板库</span>
            </div>
          </div>
        </div>

        <div class="transparent-btn" @click="goMatch">职位匹配</div>
        <div class="transparent-btn active" @click="goInterview">面试练习</div>
      </div>

      <!-- 右侧 -->
      <div class="header-right">
        <div class="auth-dropdown">
          <div v-if="authStore.isLoggedIn" class="user-menu">
            <div class="user-avatar">{{ userNameFirstChar }}</div>
            <span class="user-name">{{ userNickName }}</span>
            <el-icon :size="14" class="dropdown-icon"><ArrowDown /></el-icon>
            <div class="dropdown-menu">
              <div class="dropdown-item" @click="$router.push('/profile')">
                <el-icon><User /></el-icon><span>个人中心</span>
              </div>
              <div class="dropdown-item logout-item" @click="handleLogout">
                <el-icon><SwitchButton /></el-icon><span>退出登录</span>
              </div>
            </div>
          </div>
          <el-button v-else size="large" @click="$router.push('/login')" class="login-btn">
            登录 / 注册
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主视觉标语 - 与仪表盘风格一致 -->
    <section class="hero-section">
      <h1 class="hero-title">🎯 模拟面试练习</h1>
      <p class="hero-subtitle">
        选择一份简历和目标职位，AI 面试官将根据你的经历进行针对性提问，
        帮助你提前适应真实面试场景，提升应答能力。
      </p>
    </section>

    <!-- 面试设置卡片 - 与仪表盘轮播区域风格一致 -->
    <section class="content-section">
      <div class="form-card">
        <div class="card-header">
          <span class="card-title">面试设置</span>
          <span class="card-subtitle">填写信息，开始模拟面试</span>
        </div>

        <el-form label-width="100px" label-position="top">
          <el-form-item label="选择简历">
            <el-select v-model="resumeId" style="width: 100%" placeholder="请选择一份简历">
              <el-option v-for="r in resumes" :key="r.id" :label="r.title" :value="r.id" />
            </el-select>
            <div class="form-hint">选择已创建的简历，AI 将基于你的经历进行提问</div>
          </el-form-item>

          <el-form-item label="目标职位">
            <el-input v-model="jobTitle" placeholder="例如：后端开发工程师" />
            <div class="form-hint">输入你希望应聘的职位名称</div>
          </el-form-item>

          <el-button type="primary" size="large" :loading="loading" @click="onStart" class="start-btn">
            <el-icon v-if="!loading"><ChatDotRound /></el-icon>
            {{ loading ? '正在启动面试...' : '开始面试 →' }}
          </el-button>
        </el-form>
      </div>
    </section>

    <!-- 底部留白 -->
    <div class="blank-area"></div>

    <!-- 动态气泡背景 -->
    <div class="bubbles" aria-hidden="true">
      <div class="bubble" v-for="i in 15" :key="i" :style="getBubbleStyle(i)"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ChatDotRound,
  User,
  ArrowDown,
  SwitchButton,
  Document,
  Plus,
  Files
} from "@element-plus/icons-vue";
import { useInterviewStore } from "@/stores/interview";
import { useResumeStore } from "@/stores/resume";
import { useAuthStore } from "@/stores/auth";
import { ElMessage } from "element-plus";

const router = useRouter();
const interview = useInterviewStore();
const resumeStore = useResumeStore();
const authStore = useAuthStore();

// ===== 简历管理下拉 =====
const resumeDropdownOpen = ref(false);

const toggleResumeDropdown = () => {
  resumeDropdownOpen.value = !resumeDropdownOpen.value;
};
const closeResumeDropdown = () => {
  resumeDropdownOpen.value = false;
};

const goResumes = () => {
  closeResumeDropdown();
  router.push('/resumes');
};
const goResumeCreate = () => {
  closeResumeDropdown();
  router.push('/resumes/create');
};
const goResumeTemplates = () => {
  closeResumeDropdown();
  router.push('/resumes/templates');
};

// ===== 其他导航 =====
const goMatch = () => router.push('/match');
const goInterview = () => router.push('/interview/start');

// ===== 用户信息 =====
const userNickName = computed(() => {
  if (!authStore.userInfo) return "用户";
  return authStore.userInfo.nickname;
});
const userNameFirstChar = computed(() => {
  const name = userNickName.value;
  return name.slice(0, 1);
});

// ===== 面试逻辑 =====
const resumes = computed(() => resumeStore.list);
const resumeId = ref<number | null>(null);
const jobTitle = ref("后端开发工程师");
const loading = ref(false);

const handleLogout = () => {
  authStore.clearAuth();
  router.push('/login');
};

// 生成气泡样式
const getBubbleStyle = (i: number) => {
  const size = 20 + Math.random() * 60;
  const left = Math.random() * 100;
  const duration = 15 + Math.random() * 25;
  const delay = Math.random() * 20;
  const opacity = 0.1 + Math.random() * 0.25;
  return {
    width: size + 'px',
    height: size + 'px',
    left: left + '%',
    animationDuration: duration + 's',
    animationDelay: delay + 's',
    opacity: opacity,
    background: `radial-gradient(circle at 30% 30%, rgba(100, 163, 134, ${0.3 + Math.random() * 0.4}), rgba(79, 172, 254, ${0.2 + Math.random() * 0.3}))`
  };
};

onMounted(async () => {
  await resumeStore.loadList();
  if (resumeStore.list.length) resumeId.value = resumeStore.list[0].id;
});

async function onStart() {
  if (!resumeId.value) {
    ElMessage.warning("请先选择一份简历");
    return;
  }
  loading.value = true;
  try {
    const session = await interview.start(resumeId.value, jobTitle.value);
    router.push(`/interview/${session.id}/chat`);
  } catch (e: any) {
    ElMessage.error(e.message || "启动面试失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.start-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 40%, #FBFCCD 70%, #CDE2E8 100%);
  background-size: 400% 400%;
  animation: gradientMove 12s ease-in-out infinite;
  position: relative;
  overflow: hidden;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* ===== 动态气泡 ===== */
.bubbles {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bubble {
  position: absolute;
  bottom: -80px;
  border-radius: 50%;
  animation: bubbleFloat linear infinite;
  filter: blur(2px);
  will-change: transform;
}

@keyframes bubbleFloat {
  0% {
    transform: translateY(0) scale(0.8) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-110vh) scale(1.2) rotate(720deg);
    opacity: 0;
  }
}

/* ========== 顶部导航栏 - 与仪表盘一致 ========== */
.top-header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(100, 163, 134, 0.15);
}

.header-left { flex: 1; }
.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
}
.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

/* ========== 透明功能按钮 - 与仪表盘一致 ========== */
.transparent-btn {
  padding: 10px 28px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  color: #64A386;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}
.transparent-btn:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(100, 163, 134, 0.15);
}
.transparent-btn.active {
  background: rgba(100, 163, 134, 0.25);
  border-color: #64A386;
  color: #3a6a5a;
}

.dropdown-arrow {
  transition: transform 0.3s ease;
}
.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

/* ========== 简历管理下拉菜单 - 与仪表盘一致 ========== */
.dropdown-wrapper {
  position: relative;
}

.dropdown-wrapper .dropdown-menu {
  position: absolute;
  top: 52px;
  left: 50%;
  transform: translateX(-50%);
  min-width: 160px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.09);
  padding: 8px 0;
  z-index: 999;
  border: 1px solid rgba(100, 163, 134, 0.1);
}

.dropdown-wrapper .dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  font-size: 14px;
  color: #2c4d3d;
  transition: background 0.2s;
  cursor: pointer;
}
.dropdown-wrapper .dropdown-item:hover {
  background: #f0f7f4;
  color: #64A386;
}
.dropdown-wrapper .dropdown-item .el-icon {
  color: #64A386;
}

/* ========== 用户下拉 - 与仪表盘一致 ========== */
.auth-dropdown {
  position: relative;
}
.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 16px 6px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: background 0.25s ease;
  height: 44px;
}
.user-menu:hover {
  background: rgba(255, 255, 255, 0.7);
}
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #87CEEB;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-name {
  font-size: 15px;
  color: #2c4d3d;
}
.dropdown-icon {
  transition: transform 0.2s ease;
}
.user-menu:hover .dropdown-icon {
  transform: rotate(180deg);
}

.auth-dropdown .dropdown-menu {
  position: absolute;
  top: 50px;
  right: 0;
  min-width: 160px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.09);
  padding: 8px 0;
  opacity: 0;
  visibility: hidden;
  transform: translateY(10px);
  transition: all 0.25s ease;
  z-index: 999;
}
.user-menu:hover .auth-dropdown .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}
.auth-dropdown .dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  font-size: 14px;
  color: #333;
  transition: background 0.2s;
  cursor: pointer;
}
.auth-dropdown .dropdown-item:hover {
  background: #f0f7f4;
  color: #64A386;
}
.logout-item {
  color: #e16262;
}
.logout-item:hover {
  background: #fef2f2;
  color: #dc4444;
}

.login-btn {
  background: rgba(100, 163, 134, 0.85);
  border: none;
  color: #fff;
  border-radius: 999px;
}

/* ========== 标语 - 与仪表盘一致 ========== */
.hero-section {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 36px;
  padding: 28px 40px;
  background: #FBFCCD;
  border-radius: 16px;
}
.hero-title {
  margin: 0 0 12px;
  font-size: 32px;
  color: #64A386;
}
.hero-subtitle {
  margin: 0;
  color: #4f6b5d;
  font-size: 16px;
  line-height: 1.7;
}

/* ========== 内容区域 - 与仪表盘轮播区域风格一致 ========== */
.content-section {
  position: relative;
  z-index: 1;
  background-color: #fff;
  padding: 32px 40px;
  border-radius: 16px;
  max-width: 600px;
  margin: 0 auto;
}

.form-card {
  width: 100%;
}

.card-header {
  margin-bottom: 28px;
  text-align: center;
}

.card-title {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #3d6b57;
  margin-bottom: 6px;
}

.card-subtitle {
  display: block;
  font-size: 14px;
  color: #8aa89a;
}

/* ===== 表单样式 ===== */
.el-form {
  width: 100%;
}

.el-form-item {
  margin-bottom: 22px;
}

.el-form-item :deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c4d3d;
  padding-bottom: 6px;
}

.el-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 44px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  transition: all 0.3s ease;
}
.el-select :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
.el-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.30);
}

.el-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 44px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  transition: all 0.3s ease;
}
.el-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
.el-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.30);
}

.form-hint {
  font-size: 12px;
  color: #a0bcae;
  margin-top: 6px;
  padding-left: 2px;
}

.start-btn {
  width: 100%;
  margin-top: 8px;
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  font-size: 16px;
  font-weight: 500;
  height: 48px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}
.start-btn:hover {
  opacity: 0.92;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(100, 163, 134, 0.25);
}

/* ========== 底部留白 ========== */
.blank-area {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 80px;
}
</style>