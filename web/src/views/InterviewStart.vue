<template>
  <div class="start-page">
    <!-- 主视觉标语 -->
    <section class="hero-section">
      <h1 class="hero-title">🎯 模拟面试练习</h1>
      <p class="hero-subtitle">
        选择一份简历和目标职位，AI 面试官将根据你的经历进行针对性提问，
        帮助你提前适应真实面试场景，提升应答能力。
      </p>
    </section>

    <!-- 面试设置卡片 -->
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
import { ChatDotRound } from "@element-plus/icons-vue";
import { useInterviewStore } from "@/stores/interview";
import { useResumeStore } from "@/stores/resume";
import { ElMessage } from "element-plus";

const router = useRouter();
const interview = useInterviewStore();
const resumeStore = useResumeStore();

// ===== 面试逻辑 =====
const resumes = computed(() => resumeStore.list);
const resumeId = ref<number | null>(null);
const jobTitle = ref("后端开发工程师");
const loading = ref(false);

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

.blank-area {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 80px;
}
</style>
