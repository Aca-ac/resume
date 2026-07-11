<template>
  <div class="dashboard">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import {
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const router = useRouter();
const authStore = useAuthStore();

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

const handleCardClick = () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录查看完整模板');
    router.push('/login');
  } else {
    ElMessage.info('模板功能即将开放，敬请期待');
  }
};

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
  min-height: 100%;
  padding: 24px 32px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
  border-radius: 12px;
}

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
  cursor: pointer;
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
</style>