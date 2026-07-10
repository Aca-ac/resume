<template>
  <div class="import-page">
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
      <h1 class="hero-title">📥 导入简历</h1>
      <p class="hero-subtitle">
        支持上传 Word、PDF 或图片格式的简历文件，系统将自动提取内容。
      </p>
    </section>

    <!-- 导入区域 -->
    <section class="content-section">
      <div class="import-card">
        <div class="upload-icon-wrapper">
          <el-icon :size="64" class="upload-icon"><UploadFilled /></el-icon>
        </div>
        <h3 class="upload-title">上传简历文件</h3>
        <p class="upload-desc">拖拽或点击上传，支持 doc、docx、pdf、jpg、jpeg、png</p>

        <el-upload
            :show-file-list="false"
            :http-request="onImport"
            accept=".doc,.docx,.pdf,.jpg,.jpeg,.png"
            class="upload-area"
        >
          <el-button size="large" :loading="importing" class="btn-upload">
            <el-icon v-if="!importing"><Upload /></el-icon>
            {{ importing ? '正在导入...' : '选择文件上传' }}
          </el-button>
        </el-upload>

        <div class="format-tips">
          <span class="tip-item">📄 Word 文档</span>
          <span class="tip-item">📑 PDF 文件</span>
          <span class="tip-item">🖼️ 图片 (OCR识别)</span>
        </div>
      </div>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { useResumeStore } from "@/stores/resume";
import { useAuthStore } from "@/stores/auth";
import { importResume, runResumeOcr } from "@/api/resume";
import { ElMessage } from "element-plus";
import {
  User,
  ArrowDown,
  SwitchButton,
  Document,
  Plus,
  Files,
  Upload,
  UploadFilled
} from "@element-plus/icons-vue";

const router = useRouter();
const store = useResumeStore();
const authStore = useAuthStore();
const importing = ref(false);

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

// ===== 导入逻辑 =====
async function onImport(options: { file: File }) {
  importing.value = true;
  try {
    const result = await importResume(options.file);
    await store.loadList();
    ElMessage.success(`导入成功: ${result.title}`);
    const isImage = ["JPG", "JPEG", "PNG"].includes(result.fileType);
    if (isImage && result.content.startsWith("【")) {
      ElMessage.warning("图片 OCR 未成功，可稍后重试识别");
    } else if (isImage) {
      ElMessage.success("图片 OCR 识别完成");
    }
    if (isImage && result.fileId) {
      try {
        await runResumeOcr(result.fileId);
        await store.loadList();
      } catch {
        // import 已尝试 OCR，此处静默
      }
    }
    router.push("/resumes");
  } catch {
    ElMessage.error("导入失败，请检查文件格式");
  } finally {
    importing.value = false;
  }
}
</script>

<style scoped>
.import-page {
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

/* ========== 导入区域 ========== */
.content-section {
  max-width: 600px;
  margin: 0 auto;
}

.import-card {
  background-color: #fff;
  padding: 48px 40px;
  border-radius: 16px;
  text-align: center;
}

.upload-icon-wrapper {
  width: 96px;
  height: 96px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: #f0f7f4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  color: #64A386;
}

.upload-title {
  font-size: 20px;
  font-weight: 600;
  color: #3d6b57;
  margin: 0 0 8px;
}

.upload-desc {
  color: #8aa89a;
  font-size: 14px;
  margin: 0 0 28px;
}

.upload-area {
  display: block;
}

.btn-upload {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 10px;
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 500;
  min-width: 200px;
}
.btn-upload:hover {
  opacity: 0.92;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(100, 163, 134, 0.25);
}

.format-tips {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.tip-item {
  font-size: 13px;
  color: #a0bcae;
  background: #f8fbf9;
  padding: 4px 16px;
  border-radius: 20px;
}

.blank-area {
  width: 100%;
  min-height: 40px;
}
</style>