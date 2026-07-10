<template>
  <div class="list-page">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left"></div>

      <!-- 中间：功能按钮组 -->
      <div class="header-center">
        <div class="dropdown-wrapper">
          <div class="transparent-btn active" @click="toggleResumeDropdown">
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

    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">📄 我的简历</h1>
      <p class="hero-subtitle">
        管理你的所有简历文档，支持新建、导入、编辑、优化和导出 PDF。
      </p>
    </section>

    <!-- 内容区域 -->
    <section class="content-section">
      <div class="toolbar">
        <span class="toolbar-title">简历列表</span>
        <div class="toolbar-btns">
          <el-button type="primary" @click="$router.push('/resumes/new')" class="btn-primary">
            <el-icon><Plus /></el-icon> 新建简历
          </el-button>
          <el-button @click="$router.push('/resumes/import')" class="btn-outline">
            <el-icon><Upload /></el-icon> 导入简历
          </el-button>
        </div>
      </div>

      <el-table :data="store.list" v-loading="loading" stripe class="resume-table">
        <el-table-column prop="title" label="简历标题" min-width="180">
          <template #default="{ row }">
            <span class="resume-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'IMPORT' ? 'success' : 'info'">
              {{ row.sourceType === "IMPORT" ? "📥 导入" : "✏️ 新建" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button link class="action-btn" @click="$router.push(`/resumes/${row.id}/edit`)">
              编辑
            </el-button>
            <el-button link type="primary" class="action-btn" @click="$router.push(`/resumes/${row.id}/optimize`)">
              ✨ 优化
            </el-button>
            <el-button link class="action-btn" @click="onExport(row.id)">
              📄 PDF
            </el-button>
            <el-button link type="danger" class="action-btn" @click="onDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useResumeStore } from "@/stores/resume";
import { useAuthStore } from "@/stores/auth";
import { exportResumePdf } from "@/api/resume";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  User,
  ArrowDown,
  SwitchButton,
  Document,
  Plus,
  Files,
  Upload
} from "@element-plus/icons-vue";

const router = useRouter();
const store = useResumeStore();
const authStore = useAuthStore();
const loading = ref(false);

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

// ===== 页面逻辑 =====
onMounted(async () => {
  loading.value = true;
  await store.loadList();
  loading.value = false;
});

async function onDelete(id: number) {
  await ElMessageBox.confirm("确定要删除这份简历吗？", "确认删除", {
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
    type: "warning"
  });
  await store.remove(id);
  ElMessage.success("已删除");
}

async function onExport(id: number) {
  await exportResumePdf(id);
}
</script>

<style scoped>
.list-page {
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

/* ========== 透明功能按钮 ========== */
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

/* ========== 下拉菜单 ========== */
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

/* ========== 用户下拉 ========== */
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

/* ========== 内容区域 ========== */
.content-section {
  background-color: #fff;
  padding: 24px 28px;
  border-radius: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-title {
  font-size: 18px;
  font-weight: 600;
  color: #3d6b57;
}

.toolbar-btns {
  display: flex;
  gap: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 8px;
}
.btn-primary:hover {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}

.btn-outline {
  border: 1px solid rgba(100, 163, 134, 0.3);
  color: #64A386;
  background: transparent;
  border-radius: 8px;
}
.btn-outline:hover {
  background: rgba(100, 163, 134, 0.08);
  border-color: #64A386;
}

/* ===== 表格样式 ===== */
.resume-table {
  border-radius: 12px;
  overflow: hidden;
}

.resume-table :deep(.el-table__header th) {
  background: #f0f7f4;
  color: #2c4d3d;
  font-weight: 600;
}

.resume-table :deep(.el-table__row) {
  transition: background-color 0.2s ease;
}
.resume-table :deep(.el-table__row:hover) {
  background-color: #f8fbf9;
}

.resume-title {
  font-weight: 500;
  color: #2c4d3d;
}

.action-btn {
  font-size: 13px;
  padding: 4px 8px;
}
.action-btn:hover {
  text-decoration: underline;
}

.blank-area {
  width: 100%;
  min-height: 80px;
}
</style>