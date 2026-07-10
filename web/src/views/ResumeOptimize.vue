<template>
  <div class="optimize-page">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left"></div>

      <div class="header-center">
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
        <div class="transparent-btn" @click="goInterview">面试练习</div>
      </div>

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

    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">✨ AI 简历优化</h1>
      <p class="hero-subtitle">
        输入目标职位，AI 将根据职位要求智能优化你的简历内容，提升匹配度。
      </p>
    </section>

    <!-- 优化设置 -->
    <section class="content-section">
      <div class="optimize-card">
        <div class="card-header">
          <span class="card-title">🎯 优化设置</span>
          <span class="card-subtitle">输入目标职位，点击优化</span>
        </div>

        <div class="optimize-form">
          <el-form inline>
            <el-form-item label="目标职位">
              <el-input
                  v-model="targetRole"
                  placeholder="例如：后端开发工程师"
                  class="target-input"
                  @keyup.enter="onOptimize"
              />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="onOptimize" class="btn-optimize">
              <span v-if="!loading">🚀 开始优化</span>
              <span v-else>优化中...</span>
            </el-button>
          </el-form>
        </div>
      </div>
    </section>

    <!-- 优化结果预览 -->
    <section class="previews-section">
      <el-row :gutter="24">
        <el-col :span="12">
          <div v-if="original" class="preview-card">
            <div class="preview-header">
              <span class="preview-title">📄 原始简历</span>
              <el-tag size="small" type="info">修改前</el-tag>
            </div>
            <ResumePreview :title="original.title || '原始'" :content="original.content" class="preview" />
          </div>
        </el-col>
        <el-col :span="12">
          <transition name="scale-fade">
            <div v-if="optimized" class="preview-card highlight">
              <div class="preview-header">
                <span class="preview-title">✨ 优化后的简历</span>
                <el-tag size="small" type="success">AI 优化</el-tag>
              </div>
              <ResumePreview :title="optimized.title || '优化后'" :content="optimized.content" class="preview" />
            </div>
            <div v-else class="preview-placeholder">
              <el-icon :size="48"><MagicStick /></el-icon>
              <p>点击「开始优化」生成 AI 优化版本</p>
              <span class="placeholder-hint">AI 将根据目标职位优化简历内容</span>
            </div>
          </transition>
        </el-col>
      </el-row>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { optimizeResume } from "@/api/resume";
import ResumePreview from "@/components/ResumePreview.vue";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";
import {
  User,
  ArrowDown,
  SwitchButton,
  Document,
  Plus,
  Files,
  MagicStick
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const store = useResumeStore();
const authStore = useAuthStore();

const targetRole = ref("后端开发工程师");
const loading = ref(false);
const original = ref<{ title: string; content: string } | null>(null);
const optimized = ref<{ title: string; content: string } | null>(null);

// ===== 用户信息 =====
const userNickName = computed(() => {
  if (!authStore.userInfo) return "用户";
  return authStore.userInfo.nickname;
});
const userNameFirstChar = computed(() => {
  const name = userNickName.value;
  return name.slice(0, 1);
});

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
  router.push('/resumes/new');
};
const goResumeTemplates = () => {
  closeResumeDropdown();
  router.push('/resumes/templates');
};

const goMatch = () => router.push('/match');
const goInterview = () => router.push('/interview/start');

const handleLogout = () => {
  authStore.clearAuth();
  router.push('/login');
};

// ===== 优化逻辑 =====
onMounted(async () => {
  await store.loadOne(Number(route.params.id));
  if (store.current) {
    original.value = {
      title: store.current.title,
      content: store.current.content
    };
  }
});

async function onOptimize() {
  if (!targetRole.value.trim()) {
    ElMessage.warning("请输入目标职位");
    return;
  }
  loading.value = true;
  try {
    const result = await optimizeResume(Number(route.params.id), targetRole.value);
    optimized.value = {
      title: targetRole.value,
      content: result.content
    };
    ElMessage.success("优化完成！");
  } catch (e: any) {
    ElMessage.error(e.message || "优化失败，请重试");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.optimize-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
}

/* ========== 顶部导航栏 ========== */
.top-header {
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

.dropdown-arrow {
  transition: transform 0.3s ease;
}
.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

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

/* ========== 标语 ========== */
.hero-section {
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

/* ========== 优化设置 ========== */
.content-section {
  margin-bottom: 28px;
}

.optimize-card {
  background-color: #fff;
  padding: 28px 32px;
  border-radius: 16px;
  max-width: 720px;
  margin: 0 auto;
}

.card-header {
  margin-bottom: 20px;
}

.card-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #3d6b57;
}

.card-subtitle {
  display: block;
  font-size: 13px;
  color: #8aa89a;
  margin-top: 2px;
}

.optimize-form {
  display: flex;
  align-items: center;
  gap: 12px;
}

.optimize-form :deep(.el-form) {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
}

.optimize-form :deep(.el-form-item) {
  flex: 1;
  margin-bottom: 0;
}

.optimize-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c4d3d;
  margin-right: 12px;
}

.target-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 44px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  transition: all 0.3s ease;
}
.target-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
.target-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.30);
}

.btn-optimize {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 10px;
  padding: 0 32px;
  height: 44px;
  font-weight: 500;
  white-space: nowrap;
}
.btn-optimize:hover {
  opacity: 0.92;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}

/* ========== 预览区域 ========== */
.previews-section {
  max-width: 1200px;
  margin: 0 auto;
}

.preview-card {
  background-color: #fff;
  padding: 20px 24px 24px;
  border-radius: 16px;
}

.preview-card.highlight {
  border: 2px solid #64A386;
  box-shadow: 0 4px 20px rgba(100, 163, 134, 0.12);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #3d6b57;
}

.preview {
  background: #fafdfb;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8f0ec;
  max-height: 500px;
  overflow-y: auto;
}

.preview-placeholder {
  background-color: #fff;
  padding: 60px 24px;
  border-radius: 16px;
  text-align: center;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #dce8e2;
}

.preview-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 16px;
}

.preview-placeholder p {
  margin: 0;
  font-size: 16px;
  color: #8aa89a;
}

.placeholder-hint {
  font-size: 13px;
  color: #b8cec2;
  margin-top: 8px;
}

.blank-area {
  width: 100%;
  min-height: 40px;
}

.scale-fade-enter-active {
  transition: all 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.scale-fade-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.97);
}
</style>