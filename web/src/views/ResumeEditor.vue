<template>
  <div class="editor-page">
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
      <h1 class="hero-title">✏️ 编辑简历</h1>
      <p class="hero-subtitle">修改简历标题和内容，更新你的简历文档。</p>
    </section>

    <!-- 编辑区域 -->
    <section class="content-section">
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="form-card">
            <div class="card-header">
              <span class="card-title">📝 编辑内容</span>
              <span class="card-subtitle">修改简历信息</span>
            </div>
            <el-form :model="form" label-width="80px" label-position="top">
              <el-form-item label="简历标题">
                <el-input v-model="form.title" placeholder="请输入简历标题" />
              </el-form-item>
              <el-form-item label="简历内容">
                <el-input
                    v-model="form.content"
                    type="textarea"
                    :rows="16"
                    placeholder="请填写你的简历内容..."
                />
              </el-form-item>
              <div class="form-actions">
                <el-button @click="handleBack" class="btn-cancel">取消</el-button>
                <el-button type="primary" :loading="saving" @click="onSave" class="btn-save">
                  <el-icon><Check /></el-icon> 保存修改
                </el-button>
              </div>
            </el-form>
          </div>
        </el-col>
        <el-col :span="12">
          <transition name="fade-slide">
            <div v-if="form.content" class="preview-card">
              <div class="card-header">
                <span class="card-title">👁️ 实时预览</span>
                <span class="card-subtitle">简历效果</span>
              </div>
              <ResumePreview
                  :title="form.title || '预览'"
                  :content="form.content"
                  class="preview"
              />
            </div>
            <div v-else class="preview-placeholder">
              <el-icon :size="48"><Document /></el-icon>
              <p>输入内容后，此处将显示实时预览</p>
            </div>
          </transition>
        </el-col>
      </el-row>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import ResumePreview from "@/components/ResumePreview.vue";
import { useResumeStore } from "@/stores/resume";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  User,
  ArrowDown,
  SwitchButton,
  Document,
  Plus,
  Files,
  Check
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const store = useResumeStore();
const authStore = useAuthStore();
const saving = ref(false);

const originForm = reactive({ title: "", content: "" });
const form = reactive({ title: "", content: "" });

const isModified = ref(false);
watch([() => form.title, () => form.content], () => {
  isModified.value = form.title !== originForm.title || form.content !== originForm.content;
});

const isEdit = computed(() => Boolean(route.params.id));

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
  if (isEdit.value) {
    await store.loadOne(Number(route.params.id));
    if (store.current) {
      form.title = store.current.title;
      form.content = store.current.content;
      originForm.title = store.current.title;
      originForm.content = store.current.content;
    }
  }
});

async function onSave() {
  if (!form.title?.trim()) {
    ElMessage.warning("请先填写简历标题");
    return;
  }
  saving.value = true;
  try {
    await store.save({
      id: isEdit.value ? Number(route.params.id) : undefined,
      title: form.title,
      content: form.content
    });
    ElMessage.success("保存成功");
    originForm.title = form.title;
    originForm.content = form.content;
    isModified.value = false;
    router.push("/resumes");
  } finally {
    saving.value = false;
  }
}

const handleBack = async () => {
  if (!isModified.value) {
    router.push("/resumes");
    return;
  }

  ElMessageBox.confirm(
      "当前简历内容未保存，是否保存更改？",
      "提示",
      {
        confirmButtonText: "保存",
        cancelButtonText: "放弃",
        type: "warning",
        closeOnClickModal: false
      }
  )
      .then(async () => {
        await onSave();
      })
      .catch(() => {
        router.push("/resumes");
      });
};
</script>

<style scoped>
.editor-page {
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

/* ========== 内容区域 ========== */
.content-section {
  background-color: #fff;
  padding: 28px 32px;
  border-radius: 16px;
}

.form-card {
  width: 100%;
}

.preview-card {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: #f8fbf9;
  border-radius: 12px;
  border: 2px dashed #dce8e2;
  color: #a0bcae;
}
.preview-placeholder .el-icon {
  color: #c8d8d2;
  margin-bottom: 12px;
}
.preview-placeholder p {
  margin: 0;
  font-size: 14px;
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

.el-form-item :deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c4d3d;
  padding-bottom: 4px;
}

.el-input :deep(.el-input__wrapper) {
  border-radius: 10px;
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

.el-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: #fafdfb;
  font-family: inherit;
  line-height: 1.7;
  transition: all 0.3s ease;
}
.el-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn-cancel {
  border: 1px solid rgba(100, 163, 134, 0.3);
  color: #64A386;
  background: transparent;
  border-radius: 8px;
  padding: 10px 28px;
}
.btn-cancel:hover {
  background: rgba(100, 163, 134, 0.08);
  border-color: #64A386;
}

.btn-save {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  color: #fff;
  border-radius: 8px;
  padding: 10px 28px;
}
.btn-save:hover {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(100, 163, 134, 0.25);
}

.preview {
  background: #fafdfb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8f0ec;
}

.blank-area {
  width: 100%;
  min-height: 40px;
}

.fade-slide-enter-active {
  transition: all 0.4s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
</style>