<!-- src/views/ResetPassword.vue -->
<template>
  <div class="auth-container">
    <!-- 动态背景层 -->
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="bubble" v-for="i in 12" :key="i" :style="getBubbleStyle(i)"></div>
    </div>

    <div class="auth-card">
      <!-- 左侧图片区域 -->
      <div class="auth-left">
        <img :src="resetImage" alt="重置密码插图" class="auth-image" />
      </div>

      <!-- 右侧表单区域 -->
      <div class="auth-right">
        <h1 class="auth-title">重置密码</h1>
        <p class="auth-subtitle">通过邮箱验证码重置您的密码</p>

        <el-form
            ref="resetFormRef"
            :model="resetForm"
            :rules="rules"
            label-width="0"
            @submit.prevent="handleReset"
        >
          <el-form-item prop="email">
            <el-input
                v-model="resetForm.email"
                placeholder="请输入邮箱"
                size="large"
                prefix-icon="Message"
                autocomplete="email"
            />
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-input-wrapper">
              <el-input
                  v-model="resetForm.code"
                  placeholder="请输入验证码"
                  size="large"
                  prefix-icon="Lock"
                  maxlength="6"
              />
              <el-button
                  class="code-btn"
                  size="large"
                  :disabled="codeCountdown > 0 || !isEmailValid"
                  @click="handleSendCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
                v-model="resetForm.password"
                type="password"
                placeholder="请输入新密码（6-20位，含字母和数字）"
                size="large"
                prefix-icon="Lock"
                show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
                v-model="resetForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                size="large"
                prefix-icon="Lock"
                show-password
            />
          </el-form-item>

          <el-button
              type="primary"
              size="large"
              class="auth-btn"
              :loading="loading"
              @click="handleReset"
          >
            重置密码
          </el-button>
        </el-form>

        <div class="auth-footer">
          想起密码了？
          <router-link to="/login" class="auth-link">返回登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const resetFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)
const codeCountdown = ref(0)
let countdownTimer: number | null = null

const resetImage = new URL('@/assets/resumepicture/picture011.jpg', import.meta.url).href

const resetForm = ref({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const isEmailValid = computed(() => {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return emailRegex.test(resetForm.value.email)
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20位之间', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/,
      message: '密码需同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_: any, value: string, callback: any) => {
        if (value !== resetForm.value.password) {
          callback(new Error('两次密码输入不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleSendCode = async () => {
  if (!isEmailValid.value) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  const result = await authStore.sendCode(resetForm.value.email)
  if (result.success) {
    codeCountdown.value = 60
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
    countdownTimer = window.setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(countdownTimer!)
        countdownTimer = null
      }
    }, 1000)
  }
}

const handleReset = async () => {
  if (!resetFormRef.value) return

  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const result = await authStore.resetPassword(
          resetForm.value.email,
          resetForm.value.password,
          resetForm.value.code
      )

      if (result.success) {
        router.push('/login')
      }
    } finally {
      loading.value = false
    }
  })
}

// ===== 气泡动画 =====
const getBubbleStyle = (index: number) => {
  const size = 40 + Math.random() * 120
  const left = Math.random() * 100
  const duration = 15 + Math.random() * 25
  const delay = Math.random() * 20
  const opacity = 0.15 + Math.random() * 0.25
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity: opacity,
    background: `radial-gradient(circle, rgba(100, 163, 134, ${opacity * 0.5}), rgba(205, 226, 232, ${opacity * 0.3}))`
  }
}
</script>

<style scoped>
.auth-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  overflow: hidden;
}

/* ===== 动态背景层 ===== */
.bg-layer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 50%, #FBFCCD 100%);
  background-size: 400% 400%;
  animation: gradientMove 12s ease-in-out infinite;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
  100% { background-position: 0% 50%; }
}

/* ===== 气泡 ===== */
.bubble {
  position: absolute;
  bottom: -100px;
  border-radius: 50%;
  will-change: transform;
  animation: bubbleRise linear infinite;
  filter: blur(8px);
  pointer-events: none;
}

@keyframes bubbleRise {
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
    transform: translateY(-120vh) scale(1.2) rotate(720deg);
    opacity: 0;
  }
}

/* ===== 卡片 ===== */
.auth-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: 860px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(100, 163, 134, 0.20);
  overflow: hidden;
  min-height: 520px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* ===== 左侧图片 ===== */
.auth-left {
  flex: 0 0 45%;
  background: transparent;
  display: flex;
  align-items: stretch;
  padding: 0;
  overflow: hidden;
}
.auth-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ===== 右侧表单 ===== */
.auth-right {
  flex: 1;
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-title {
  font-size: 26px;
  font-weight: 700;
  color: #2c4d3d;
  text-align: center;
  margin: 0 0 6px;
}

.auth-subtitle {
  font-size: 14px;
  color: #6b8a7a;
  text-align: center;
  margin: 0 0 30px;
}

.code-input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}
.code-input-wrapper .el-input {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  width: 120px;
  background: rgba(232, 240, 236, 0.7);
  border: none;
  color: #2c4d3d;
  font-size: 14px;
  border-radius: 10px;
}
.code-btn:hover:not(:disabled) {
  background: rgba(212, 228, 220, 0.8);
}
.code-btn:disabled {
  color: #9ab0a4;
  cursor: not-allowed;
}

.auth-btn {
  width: 100%;
  margin-top: 4px;
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
  font-size: 16px;
  font-weight: 500;
  height: 48px;
  border-radius: 10px;
  color: #fff;
}
.auth-btn:hover {
  opacity: 0.9;
}

.auth-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: #6b8a7a;
}

.auth-link {
  color: #64A386;
  text-decoration: none;
  font-weight: 600;
}
.auth-link:hover {
  text-decoration: underline;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 48px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.15);
  background: rgba(255, 255, 255, 0.6);
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.25);
}

@media (max-width: 768px) {
  .auth-card {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
    min-height: auto;
  }
  .auth-left {
    flex: 0 0 auto;
    min-height: 180px;
    max-height: 220px;
  }
  .auth-image {
    object-fit: cover;
  }
  .auth-right {
    padding: 28px 24px;
  }
}
</style>

