// InterviewReport.vue
<template>
  <div class="report-page">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <el-button class="back-home-btn" @click="goHome">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </el-button>
      </div>
      <div class="header-center">
        <div class="nav-btn">简历管理</div>
        <div class="nav-btn">职位匹配</div>
        <div class="nav-btn active">面试练习</div>
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

    <div class="page-content">
      <div class="toolbar">
        <h2 class="page-title">面试报告</h2>
        <div class="toolbar-actions">
          <el-button type="primary" :loading="loading" @click="onGenerate">
            重新生成报告
          </el-button>
          <el-button @click="$router.push('/interview/start')">开始新面试</el-button>
        </div>
      </div>

      <transition name="scale-fade">
        <el-card v-if="report" class="report-card animate-fade-up" shadow="hover">
          <div class="report-header">
            <el-tag type="success" size="large">面试已完成</el-tag>
          </div>
          <div class="report-content">
            <pre>{{ report }}</pre>
          </div>
        </el-card>
      </transition>

      <el-empty
          v-if="!report && !loading"
          description="暂未生成报告，请完成面试"
          class="animate-fade-up"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { useAuthStore } from "@/stores/auth";
import { ElMessage } from "element-plus";
import { User, ArrowDown, SwitchButton, ArrowLeft } from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const interview = useInterviewStore();
const authStore = useAuthStore();

const sessionId = computed(() => Number(route.params.sessionId));
const loading = ref(false);
const report = ref("");

const userNickName = computed(() => authStore.userInfo?.nickname || "用户");
const userNameFirstChar = computed(() => userNickName.value.slice(0, 1));

const goHome = () => router.push('/dashboard');

const handleLogout = () => {
  authStore.clearAuth();
  router.push('/login');
};

async function loadReport() {
  loading.value = true;
  try {
    const session = await interview.loadReport(sessionId.value);
    report.value = session.report || "";
  } catch (e: any) {
    ElMessage.error(e.message || "获取报告失败");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadReport();
});

async function onGenerate() {
  await loadReport();
}
</script>

<style scoped>
.report-page {
  width: 100%;
  min-height: 100vh;
  padding: 20px 40px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 40%, #FBFCCD 70%, #CDE2E8 100%);
  background-size: 400% 400%;
  animation: gradientMove 12s ease-in-out infinite;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* ===== 顶部导航栏 ===== */
.top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 0 8px 16px 8px;
  border-bottom: 1px solid rgba(100, 163, 134, 0.12);
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

.header-left {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.header-right {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.back-home-btn {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #4a7a6a;
  border-radius: 999px;
  padding: 8px 18px;
  font-size: 13px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}
.back-home-btn:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(100, 163, 134, 0.12);
}

/* ===== 导航按钮（仅展示，无跳转） ===== */
.nav-btn {
  padding: 8px 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #5a8a7a;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: default;
  transition: all 0.3s ease;
}
.nav-btn:hover {
  background: rgba(255, 255, 255, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(100, 163, 134, 0.1);
}
.nav-btn.active {
  background: rgba(100, 163, 134, 0.25);
  border-color: #64A386;
  color: #3a6a5a;
}

.auth-dropdown { position: relative; }
.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 14px 4px 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: background 0.25s ease;
  height: 40px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}
.user-menu:hover { background: rgba(255, 255, 255, 0.7); }
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #87CEEB;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-name { font-size: 14px; color: #2c4d3d; }
.dropdown-icon { transition: transform 0.2s ease; color: #5a8a7a; }
.user-menu:hover .dropdown-icon { transform: rotate(180deg); }

.dropdown-menu {
  position: absolute;
  top: 46px;
  right: 0;
  min-width: 150px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  padding: 6px 0;
  opacity: 0;
  visibility: hidden;
  transform: translateY(8px);
  transition: all 0.25s ease;
  z-index: 999;
  border: 1px solid rgba(255, 255, 255, 0.3);
}
.user-menu:hover .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}
.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 13px;
  color: #333;
  transition: background 0.2s;
  cursor: pointer;
}
.dropdown-item:hover { background: #f0f7f4; color: #64A386; }
.logout-item { color: #e16262; }
.logout-item:hover { background: #fef2f2; color: #dc4444; }

.login-btn {
  background: rgba(100, 163, 134, 0.85);
  border: none;
  color: #fff;
  border-radius: 999px;
  padding: 8px 20px;
  font-size: 14px;
}
.login-btn:hover { background: rgba(100, 163, 134, 1); }

/* ===== 页面内容 ===== */
.page-content {
  max-width: 820px;
  margin: 0 auto;
  padding: 0 8px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #2c4d3d;
  margin: 0;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.report-card {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-left: 4px solid #64A386;
  overflow: hidden;
}

.report-header {
  padding: 16px 20px 0;
  border-bottom: 1px solid rgba(100, 163, 134, 0.1);
}

.report-content {
  padding: 20px;
}

.report-content pre {
  white-space: pre-wrap;
  margin: 0;
  line-height: 1.8;
  color: #2c4d3d;
  font-family: inherit;
}

.animate-fade-up {
  animation: fadeUp 0.6s ease-out;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.scale-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.scale-fade-enter-from {
  opacity: 0;
  transform: translateY(16px);

}

</style>