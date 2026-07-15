<!-- src/views/Login.vue -->
<template>
  <div class="auth-container" role="main" aria-labelledby="auth-title">
    <!-- 动态背景层 - 纯装饰，对辅助技术隐藏 -->
    <div class="bg-layer" aria-hidden="true">
      <div class="bg-gradient"></div>
      <div class="bubble" v-for="i in 12" :key="i" :style="getBubbleStyle(i)"></div>
    </div>

    <div class="auth-card">
      <!-- 左侧图片区域 -->
      <div class="auth-left" aria-hidden="true">
        <img
            :src="loginImage"
            alt="登录插图：简历与面试管理"
            class="auth-image"
            decoding="async"
            loading="lazy"
        />
      </div>

      <!-- 右侧表单区域 -->
      <div class="auth-right">
        <h1 id="auth-title" class="auth-title">欢迎回来</h1>
        <p class="auth-subtitle">登录您的账号，管理简历与面试</p>

        <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            label-width="0"
            @submit.prevent="handleLogin"
            novalidate
        >
          <el-form-item prop="email">
            <label for="email-input" class="sr-only">邮箱地址</label>
            <el-input
                id="email-input"
                v-model="loginForm.email"
                placeholder="请输入邮箱"
                size="large"
                prefix-icon="Message"
                autocomplete="email"
                type="email"
                aria-describedby="email-error"
                aria-required="true"
                @keydown.enter.prevent="handleLogin"
            />
            <div id="email-error" role="alert" aria-live="polite">
              <span v-if="emailError" class="error-message">{{ emailError }}</span>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <label for="password-input" class="sr-only">密码</label>
            <el-input
                id="password-input"
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                prefix-icon="Lock"
                show-password
                autocomplete="current-password"
                aria-describedby="password-error password-hint"
                aria-required="true"
                @keydown.enter.prevent="handleLogin"
            />
            <div id="password-hint" class="hint-text">密码长度在6-20位之间</div>
            <div id="password-error" role="alert" aria-live="polite">
              <span v-if="passwordError" class="error-message">{{ passwordError }}</span>
            </div>
          </el-form-item>

          <div class="form-options">
            <router-link
                to="/reset-password"
                class="forgot-link"
                aria-label="忘记密码？点击重置"
            >
              忘记密码？
            </router-link>
          </div>

          <el-button
              type="primary"
              size="large"
              class="auth-btn"
              :loading="loading"
              @click="handleLogin"
              :aria-busy="loading"
              :disabled="loading"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <div class="auth-footer">
          还没有账号？
          <router-link to="/register" class="auth-link" aria-label="立即注册新账号">
            立即注册
          </router-link>
        </div>

        <!-- 登录状态提示 -->
        <div role="status" aria-live="polite" class="sr-only">
          {{ loading ? '正在登录，请稍候' : '' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)

// 手动错误状态（用于实时反馈）
const emailError = ref('')
const passwordError = ref('')

const loginImage = new URL('@/assets/resumepicture/picture010.jpg', import.meta.url).href

const loginForm = reactive({
  email: '',
  password: ''
})

// 表单验证规则（增强错误信息）
const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式（例如：user@example.com）', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20位之间', trigger: 'blur' }
  ]
}

// 实时验证邮箱
const validateEmail = (value: string) => {
  if (!value) {
    emailError.value = '请输入邮箱地址'
    return false
  }
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!emailRegex.test(value)) {
    emailError.value = '请输入正确的邮箱格式'
    return false
  }
  emailError.value = ''
  return true
}

// 实时验证密码
const validatePassword = (value: string) => {
  if (!value) {
    passwordError.value = '请输入密码'
    return false
  }
  if (value.length < 6 || value.length > 20) {
    passwordError.value = '密码长度必须在6-20位之间'
    return false
  }
  passwordError.value = ''
  return true
}

// 监听输入进行实时验证
const handleEmailInput = (value: string) => {
  if (value) validateEmail(value)
}

const handlePasswordInput = (value: string) => {
  if (value) validatePassword(value)
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  // 先进行手动验证
  const isEmailValid = validateEmail(loginForm.email)
  const isPasswordValid = validatePassword(loginForm.password)

  if (!isEmailValid || !isPasswordValid) {
    // 触发表单验证以显示所有错误
    await loginFormRef.value.validate()
    return
  }

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const result = await authStore.login(
          loginForm.email,
          loginForm.password
      )

      if (result.success) {
        const redirect = route.query.redirect as string
        ElMessage.success('登录成功！')
        router.push(redirect || '/dashboard')
      } else {
        ElMessage.error(result.message || '登录失败，请检查邮箱和密码')
        // 聚焦到邮箱输入框以便用户重试
        document.getElementById('email-input')?.focus()
      }
    } catch (error) {
      ElMessage.error('登录过程中发生错误，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

// 键盘快捷键：按 Escape 清除表单
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    loginForm.email = ''
    loginForm.password = ''
    emailError.value = ''
    passwordError.value = ''
    document.getElementById('email-input')?.focus()
  }
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

onMounted(() => {
  // 如果 URL 有 redirect 参数，聚焦到邮箱
  if (route.query.redirect) {
    setTimeout(() => {
      document.getElementById('email-input')?.focus()
    }, 100)
  }

  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ===== 屏幕阅读器专用 ===== */
.sr-only {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  border: 0 !important;
  white-space: nowrap !important;
}

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
  color: #4a6b5a;
  text-align: center;
  margin: 0 0 30px;
}

.form-options {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 22px;
}

/* 忘记密码链接 - 增强对比度 */
.forgot-link {
  font-size: 14px;
  color: #2d6b4f;
  text-decoration: underline;
  text-underline-offset: 2px;
  font-weight: 500;
}
.forgot-link:hover,
.forgot-link:focus {
  color: #1a4d36;
  text-decoration-thickness: 2px;
}
.forgot-link:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
  border-radius: 2px;
}

/* 登录按钮 - 增强对比度 */
.auth-btn {
  width: 100%;
  margin-top: 4px;
  background: linear-gradient(135deg, #2d6b4f 0%, #1a4d36 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  height: 48px;
  border-radius: 10px;
  color: #ffffff;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s, box-shadow 0.2s;
}
.auth-btn:hover:not(:disabled) {
  opacity: 0.92;
  box-shadow: 0 4px 16px rgba(45, 107, 79, 0.35);
}
.auth-btn:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
}
.auth-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: #4a6b5a;
}

.auth-link {
  color: #2d6b4f;
  text-decoration: underline;
  text-underline-offset: 2px;
  font-weight: 600;
}
.auth-link:hover,
.auth-link:focus {
  color: #1a4d36;
  text-decoration-thickness: 2px;
}
.auth-link:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
  border-radius: 2px;
}

/* ===== 表单样式 - 增强对比度 ===== */
:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 48px;
  box-shadow: 0 0 0 1px #8aaa9a;
  background: rgba(255, 255, 255, 0.85);
  transition: box-shadow 0.2s;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px #2d6b4f;
}
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px #c62828;
}
:deep(.el-input__inner) {
  color: #1a2a1f;
  font-size: 15px;
}

/* 错误信息 */
.error-message {
  display: block;
  color: #c62828;
  font-size: 13px;
  margin-top: 4px;
  font-weight: 500;
}

/* 提示文本 */
.hint-text {
  display: block;
  color: #4a6b5a;
  font-size: 13px;
  margin-top: 4px;
}

/* ===== 响应式 ===== */
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

/* ===== 减少动画偏好 ===== */
@media (prefers-reduced-motion: reduce) {
  .bg-gradient {
    animation: none;
  }
  .bubble {
    animation: none !important;
    display: none !important;
  }
  .auth-btn {
    transition: none;
  }
}
</style>