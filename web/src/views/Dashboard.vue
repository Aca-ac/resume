<template>
  <div class="dashboard">
    <!-- 顶部导航栏 -->
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
      <h1 class="hero-title">AI智能简历助手</h1>
      <p class="hero-subtitle">
        精选优质简历模板、智能简历优化、岗位智能匹配、全真模拟面试，一站式帮你打磨亮眼求职履历。
      </p>
    </section>

    <!-- 简历模板轮播 -->
    <section class="carousel-section">
      <h3 class="carousel-title">精选简历模板预览</h3>

      <div class="carousel-container">
        <div class="carousel-arrow left-arrow" @click="prevSlide">
          <el-icon :size="24"><ArrowLeft /></el-icon>
        </div>

        <div class="carousel-wrapper">
          <div
              class="carousel-card"
              v-for="(item, index) in visibleCards"
              :key="index"
              :class="getCardClass(index)"
              @click="handleCardClick"
          >
            <div class="card-image-wrapper">
              <img :src="item.img" alt="简历模板预览" class="card-image" draggable="false" />
              <div class="card-tip">仅预览 · 暂未开放使用</div>
            </div>
          </div>
        </div>

        <div class="carousel-arrow right-arrow" @click="nextSlide">
          <el-icon :size="24"><ArrowRight /></el-icon>
        </div>

        <div class="carousel-dots">
          <span
              v-for="(_, index) in templateImgs.length"
              :key="index"
              class="dot"
              :class="{ active: index === currentIndex }"
              @click="goToSlide(index)"
          ></span>
        </div>
      </div>
    </section>

    <div class="blank-area"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import {
  User,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  SwitchButton,
  Document,
  Plus,
  Files
} from '@element-plus/icons-vue';

const router = useRouter();
const authStore = useAuthStore();

const resumeDropdownOpen = ref(false);

const userNickName = computed(() => {
  if (!authStore.userInfo) return "用户";
  return authStore.userInfo.nickname;
});
const userNameFirstChar = computed(() => {
  const name = userNickName.value;
  return name.slice(0, 1);
});

const currentIndex = ref(0);
let autoplayTimer: ReturnType<typeof setInterval> | null = null;

const templateImgs = [
  new URL('@/assets/resumepicture/picture1.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture2.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture3.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture4.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture5.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture6.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture7.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture8.jpg', import.meta.url).href,
  new URL('@/assets/resumepicture/picture9.jpg', import.meta.url).href,
];

// ===== 简历管理下拉 =====
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

const goMatch = () => router.push('/match');
const goInterview = () => router.push('/interview/start');

const handleCardClick = () => {
  if (!authStore.isLoggedIn) {
    router.push('/login');
  }
};

const handleLogout = () => {
  authStore.clearAuth();
  router.push('/login');
};

// ===== 轮播 =====
const goToSlide = (index: number) => currentIndex.value = index;
const prevSlide = () => {
  currentIndex.value = currentIndex.value === 0 ? templateImgs.length - 1 : currentIndex.value - 1;
};
const nextSlide = () => {
  currentIndex.value = currentIndex.value === templateImgs.length - 1 ? 0 : currentIndex.value + 1;
};

const visibleCards = computed(() => {
  const prev = currentIndex.value === 0 ? templateImgs.length - 1 : currentIndex.value - 1;
  const next = currentIndex.value === templateImgs.length - 1 ? 0 : currentIndex.value + 1;
  return [
    { img: templateImgs[prev], position: 'prev' },
    { img: templateImgs[currentIndex.value], position: 'active' },
    { img: templateImgs[next], position: 'next' },
  ];
});
const getCardClass = (index: number) => visibleCards.value[index].position;

const startAutoplay = () => {
  autoplayTimer = setInterval(() => nextSlide(), 3500);
};
const stopAutoplay = () => {
  if (autoplayTimer) clearInterval(autoplayTimer);
};

onMounted(() => startAutoplay());
onUnmounted(() => stopAutoplay());
</script>

<style scoped>
.dashboard {
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

.dropdown-arrow {
  transition: transform 0.3s ease;
}
.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

/* ========== 简历管理下拉菜单 ========== */
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
  padding: 28px;
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

/* ========== 轮播 ========== */
.carousel-section {
  background-color: #fff;
  padding: 26px;
  border-radius: 16px;
  margin-bottom: 36px;
}
.carousel-title {
  margin: 0 0 22px;
  color: #3d6b57;
  text-align: center;
  font-size: 20px;
}
.carousel-container {
  position: relative;
  width: 100%;
  max-width: 920px;
  margin: 0 auto;
  height: 470px;
  overflow: hidden;
}
.carousel-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.carousel-card {
  position: absolute;
  width: 260px;
  height: 370px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  cursor: not-allowed;
  overflow: hidden;
}
.carousel-card .card-image-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}
.carousel-card .card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}
.carousel-card .card-tip {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(100, 163, 134, 0.9);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  white-space: nowrap;
}
.carousel-card.prev {
  transform: translateX(-130%) scale(0.85) rotateY(15deg);
  opacity: 0.6;
  z-index: 1;
}
.carousel-card.active {
  transform: translateX(0) scale(1) rotateY(0);
  opacity: 1;
  z-index: 3;
}
.carousel-card.next {
  transform: translateX(130%) scale(0.85) rotateY(-15deg);
  opacity: 0.6;
  z-index: 1;
}

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(4px);
  color: #64A386;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.25s ease;
  z-index: 10;
}
.carousel-arrow:hover {
  background: rgba(255, 255, 255, 0.9);
  opacity: 1;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}
.left-arrow { left: 14px; }
.right-arrow { right: 14px; }

.carousel-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 9px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #c8d8d2;
  cursor: pointer;
  transition: all 0.3s ease;
}
.dot.active {
  background-color: #64A386;
  width: 26px;
  border-radius: 6px;
}

.blank-area {
  width: 100%;
  min-height: 280px;
}
</style>