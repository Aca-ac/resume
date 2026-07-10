// InterviewChat.vue
<template>
  <div class="chat-page">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <el-button class="back-home-btn" @click="goHome">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </el-button>
      </div>
      <div class="header-center">
        <div class="nav-btn" @click="goResumes">简历管理</div>
        <div class="nav-btn" @click="goMatch">职位匹配</div>
        <div class="nav-btn active" @click="goInterview">面试练习</div>
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
      <div class="chat-header">
        <h2 class="page-title">面试对话</h2>
        <el-button type="primary" plain @click="$router.push(`/interview/${sessionId}/report`)">
          查看报告
        </el-button>
      </div>

      <div class="messages-panel">
        <transition-group name="msg" tag="div" class="messages">
          <div
              v-for="(m, idx) in interview.messages"
              :key="idx"
              class="msg-bubble"
              :class="m.role"
          >
            <div class="msg-avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <div class="msg-body">
              <span class="msg-role">{{ m.role === 'user' ? '我' : '面试官' }}</span>
              <p>{{ m.content }}</p>
            </div>
          </div>
          <div v-if="loading" key="typing" class="msg-bubble assistant typing">
            <div class="msg-avatar">AI</div>
            <div class="msg-body">
              <span class="typing-dots"><span></span><span></span><span></span></span>
            </div>
          </div>
        </transition-group>
      </div>

      <el-input
          v-model="answer"
          type="textarea"
          :rows="4"
          placeholder="输入你的回答..."
          class="input-area"
          @keydown.ctrl.enter="onSend"
      />
      <div class="actions">
        <el-button type="primary" :loading="loading" @click="onSend">发送回答</el-button>
        <el-button @click="$router.push(`/interview/${sessionId}/report`)">结束面试 → 查看报告</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useInterviewStore } from "@/stores/interview";
import { useAuthStore } from "@/stores/auth";
import { fetchMessages } from "@/api/interview";
import { ElMessage } from "element-plus";
import { User, ArrowDown, SwitchButton, ArrowLeft } from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const interview = useInterviewStore();
const authStore = useAuthStore();
const sessionId = computed(() => Number(route.params.sessionId));
const answer = ref("");
const loading = ref(false);

const userNickName = computed(() => authStore.userInfo?.nickname || "用户");
const userNameFirstChar = computed(() => userNickName.value.slice(0, 1));

const goHome = () => router.push('/dashboard');
const goResumes = () => router.push('/resumes');
const goMatch = () => router.push('/match');
const goInterview = () => router.push('/interview/start');

const handleLogout = () => {
  authStore.clearAuth();
  router.push('/login');
};

onMounted(async () => {
  interview.sessionId = sessionId.value;
  interview.messages = await fetchMessages(sessionId.value);
});

async function onSend() {
  if (!answer.value.trim()) return;
  loading.value = true;
  try {
    await interview.answer(answer.value);
    answer.value = "";
  } catch (e: any) {
    ElMessage.error(e.message || "发送失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.chat-page {
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

/* ===== 返回首页按钮 ===== */
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

/* ===== 导航按钮 ===== */
.nav-btn {
  padding: 8px 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #5a8a7a;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
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

/* ===== 用户下拉 ===== */
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

.chat-header {
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

.messages-panel {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(100, 163, 134, 0.1);
  padding: 24px;
  min-height: 340px;
  max-height: 500px;
  overflow-y: auto;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-bubble {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.msg-bubble.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.user .msg-avatar {
  background: linear-gradient(135deg, #409eff, #337ecc);
}
.assistant .msg-avatar {
  background: linear-gradient(135deg, #67c23a, #529b2e);
}

.msg-body {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  padding: 12px 16px;
  border-radius: 12px;
  border-top-left-radius: 4px;
}

.user .msg-body {
  background: rgba(64, 158, 255, 0.1);
  border-top-left-radius: 12px;
  border-top-right-radius: 4px;
}

.msg-role {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.msg-body p {
  margin: 6px 0 0;
  line-height: 1.6;
  color: #2c4d3d;
}

.msg-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.msg-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}

.typing-dots span {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  margin: 0 3px;
  animation: typingBounce 1.2s ease-in-out infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.input-area {
  margin-bottom: 12px;
}
.input-area :deep(.el-textarea__inner) {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.actions {
  display: flex;
  gap: 12px;
}
.actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
}
</style>