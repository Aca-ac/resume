<!-- src/views/Register.vue -->
<template>
  <div class="auth-container" role="main" aria-labelledby="register-title">
    <!-- 动态背景层 - 纯装饰，对辅助技术隐藏 -->
    <div class="bg-layer" aria-hidden="true">
      <div class="bg-gradient"></div>
      <div class="bubble" v-for="i in 12" :key="i" :style="getBubbleStyle(i)"></div>
    </div>

    <div class="auth-card">
      <!-- 左侧图片区域 -->
      <div class="auth-left" aria-hidden="true">
        <img
            :src="registerImage"
            alt="注册插图：开启简历优化之旅"
            class="auth-image"
            decoding="async"
            loading="lazy"
        />
      </div>

      <!-- 右侧表单区域 -->
      <div class="auth-right">
        <h1 id="register-title" class="auth-title">注册账号</h1>
        <p class="auth-subtitle">使用邮箱注册，开启简历优化之旅</p>

        <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="rules"
            label-width="0"
            @submit.prevent="handleRegister"
            novalidate
        >
          <!-- 邮箱 -->
          <el-form-item prop="email">
            <label for="email-input" class="sr-only">邮箱地址</label>
            <el-input
                id="email-input"
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                size="large"
                prefix-icon="Message"
                autocomplete="email"
                type="email"
                aria-describedby="email-error email-hint"
                aria-required="true"
                @keydown.enter.prevent="handleRegister"
                @input="handleEmailInput"
            />
            <div id="email-hint" class="hint-text">请输入有效的邮箱地址</div>
            <div id="email-error" role="alert" aria-live="polite">
              <span v-if="emailError" class="error-message">{{ emailError }}</span>
            </div>
          </el-form-item>

          <!-- 验证码 -->
          <el-form-item prop="code">
            <label for="code-input" class="sr-only">验证码</label>
            <div class="code-input-wrapper">
              <el-input
                  id="code-input"
                  v-model="registerForm.code"
                  placeholder="请输入验证码"
                  size="large"
                  prefix-icon="Lock"
                  maxlength="6"
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  aria-describedby="code-error code-hint"
                  aria-required="true"
                  @keydown.enter.prevent="handleRegister"
                  @input="handleCodeInput"
              />
              <el-button
                  class="code-btn"
                  size="large"
                  :disabled="codeCountdown > 0 || !isEmailValid"
                  @click="handleSendCode"
                  :aria-busy="codeCountdown > 0"
                  :aria-label="codeCountdown > 0 ? `验证码已发送，剩余 ${codeCountdown} 秒` : '获取验证码'"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
            <div id="code-hint" class="hint-text">6位数字验证码</div>
            <div id="code-error" role="alert" aria-live="polite">
              <span v-if="codeError" class="error-message">{{ codeError }}</span>
            </div>
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password">
            <label for="password-input" class="sr-only">密码</label>
            <el-input
                id="password-input"
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码（6-20位，含字母和数字）"
                size="large"
                prefix-icon="Lock"
                show-password
                autocomplete="new-password"
                aria-describedby="password-error password-hint"
                aria-required="true"
                @keydown.enter.prevent="handleRegister"
                @input="handlePasswordInput"
            />
            <div id="password-hint" class="hint-text">密码需6-20位，同时包含字母和数字</div>
            <div id="password-error" role="alert" aria-live="polite">
              <span v-if="passwordError" class="error-message">{{ passwordError }}</span>
            </div>
          </el-form-item>

          <!-- 确认密码 -->
          <el-form-item prop="confirmPassword">
            <label for="confirm-password-input" class="sr-only">确认密码</label>
            <el-input
                id="confirm-password-input"
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                size="large"
                prefix-icon="Lock"
                show-password
                autocomplete="new-password"
                aria-describedby="confirm-password-error"
                aria-required="true"
                @keydown.enter.prevent="handleRegister"
                @input="handleConfirmPasswordInput"
            />
            <div id="confirm-password-error" role="alert" aria-live="polite">
              <span v-if="confirmPasswordError" class="error-message">{{ confirmPasswordError }}</span>
            </div>
          </el-form-item>

          <el-button
              type="primary"
              size="large"
              class="auth-btn"
              :loading="loading"
              @click="handleRegister"
              :aria-busy="loading"
              :disabled="loading"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form>

        <div class="auth-footer">
          已有账号？
          <router-link to="/login" class="auth-link" aria-label="立即登录已有账号">
            立即登录
          </router-link>
        </div>

        <!-- 注册状态提示 -->
        <div role="status" aria-live="polite" class="sr-only">
          {{ loading ? '正在注册，请稍候' : '' }}
          {{ codeCountdown > 0 ? `验证码已发送，剩余 ${codeCountdown} 秒` : '' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const registerFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)
const codeCountdown = ref(0)
let countdownTimer: number | null = null

// 手动错误状态
const emailError = ref('')
const codeError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')

// 左侧图片
const registerImage = new URL('@/assets/resumepicture/picture011.jpg', import.meta.url).href

const registerForm = ref({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const isEmailValid = computed(() => {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return emailRegex.test(registerForm.value.email)
})

// 实时验证函数
const validateEmail = (value: string) => {
  if (!value) {
    emailError.value = '请输入邮箱地址'
    return false
  }
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!emailRegex.test(value)) {
    emailError.value = '请输入正确的邮箱格式（例如：user@example.com）'
    return false
  }
  emailError.value = ''
  return true
}

const validateCode = (value: string) => {
  if (!value) {
    codeError.value = '请输入验证码'
    return false
  }
  if (!/^\d{6}$/.test(value)) {
    codeError.value = '验证码为6位数字'
    return false
  }
  codeError.value = ''
  return true
}

const validatePassword = (value: string) => {
  if (!value) {
    passwordError.value = '请输入密码'
    return false
  }
  if (value.length < 6 || value.length > 20) {
    passwordError.value = '密码长度必须在6-20位之间'
    return false
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/.test(value)) {
    passwordError.value = '密码需同时包含字母和数字'
    return false
  }
  passwordError.value = ''
  return true
}

const validateConfirmPassword = (value: string) => {
  if (!value) {
    confirmPasswordError.value = '请再次输入密码'
    return false
  }
  if (value !== registerForm.value.password) {
    confirmPasswordError.value = '两次密码输入不一致'
    return false
  }
  confirmPasswordError.value = ''
  return true
}

// 输入处理函数
const handleEmailInput = (value: string) => {
  if (value) validateEmail(value)
}

const handleCodeInput = (value: string) => {
  // 只允许数字输入
  const numericValue = value.replace(/\D/g, '')
  if (value !== numericValue) {
    registerForm.value.code = numericValue
  }
  if (numericValue) validateCode(numericValue)
}

const handlePasswordInput = (value: string) => {
  if (value) validatePassword(value)
  // 如果确认密码已有值，也验证它
  if (registerForm.value.confirmPassword) {
    validateConfirmPassword(registerForm.value.confirmPassword)
  }
}

const handleConfirmPasswordInput = (value: string) => {
  if (value || registerForm.value.password) {
    validateConfirmPassword(value)
  }
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码为6位数字', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' }
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
        if (value !== registerForm.value.password) {
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
    document.getElementById('email-input')?.focus()
    return
  }

  // 清空旧的验证码错误
  codeError.value = ''

  const result = await authStore.sendCode(registerForm.value.email)
  if (result.success) {
    ElMessage.success('验证码已发送到您的邮箱')
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
    // 聚焦到验证码输入框
    nextTick(() => {
      document.getElementById('code-input')?.focus()
    })
  } else {
    ElMessage.error(result.message || '发送验证码失败，请稍后重试')
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  // 手动验证所有字段
  const isEmailValid = validateEmail(registerForm.value.email)
  const isCodeValid = validateCode(registerForm.value.code)
  const isPasswordValid = validatePassword(registerForm.value.password)
  const isConfirmValid = validateConfirmPassword(registerForm.value.confirmPassword)

  if (!isEmailValid || !isCodeValid || !isPasswordValid || !isConfirmValid) {
    // 触发表单验证以显示所有错误
    try {
      await registerFormRef.value.validate()
    } catch {
      // 聚焦到第一个错误字段
      const firstError = document.querySelector('.el-form-item.is-error input, .el-form-item.is-error .el-input__inner')
      if (firstError) {
        (firstError as HTMLElement).focus()
      }
    }
    return
  }

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const result = await authStore.register(
          registerForm.value.email,
          registerForm.value.password,
          registerForm.value.code
      )

      if (result.success) {
        ElMessage.success('注册成功！欢迎加入')
        router.push('/dashboard')
      } else {
        ElMessage.error(result.message || '注册失败，请检查信息后重试')
        // 聚焦到邮箱输入框以便用户重试
        document.getElementById('email-input')?.focus()
      }
    } catch (error: any) {
      ElMessage.error(error.message || '注册过程中发生错误，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

// 键盘快捷键：按 Escape 清空当前聚焦的输入
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    const target = event.target as HTMLInputElement
    if (target && target.tagName === 'INPUT') {
      const field = target.id.replace('-input', '')
      const formKey = field === 'confirmPassword' ? 'confirmPassword' : field
      if (formKey in registerForm.value) {
        registerForm.value[formKey as keyof typeof registerForm.value] = ''
        // 清除对应的错误
        const errorKey = field === 'confirmPassword' ? 'confirmPasswordError' : `${field}Error`
        if (errorKey in this) {
          // 使用 ref 方式清除
        }
      }
    }
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

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
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
  color: #1a3a2a;
  text-align: center;
  margin: 0 0 6px;
}

.auth-subtitle {
  font-size: 14px;
  color: #4a6a5a;
  text-align: center;
  margin: 0 0 30px;
}

/* ===== 验证码输入区域 ===== */
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
  border: 1px solid rgba(100, 163, 134, 0.2);
  color: #1a3a2a;
  font-size: 14px;
  border-radius: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.code-btn:hover:not(:disabled) {
  background: rgba(200, 220, 210, 0.8);
  border-color: rgba(45, 107, 79, 0.3);
}
.code-btn:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
}
.code-btn:disabled {
  color: #8aaa9a;
  cursor: not-allowed;
  opacity: 0.6;
}

/* ===== 登录按钮 - 增强对比度 ===== */
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
  color: #4a6a5a;
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

/* ===== 输入框样式 - 增强对比度 ===== */
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
:deep(.el-input__wrapper:hover:not(.is-disabled)) {
  box-shadow: 0 0 0 2px rgba(45, 107, 79, 0.3);
}

/* ===== 错误信息和提示 ===== */
.error-message {
  display: block;
  color: #c62828;
  font-size: 13px;
  margin-top: 4px;
  font-weight: 500;
}

.hint-text {
  display: block;
  color: #4a6b5a;
  font-size: 13px;
  margin-top: 4px;
}

/* ===== 响应式适配 ===== */
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

  .code-btn {
    width: 100px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .auth-right {
    padding: 20px 16px;
  }

  .auth-title {
    font-size: 22px;
  }

  .auth-subtitle {
    font-size: 13px;
    margin-bottom: 20px;
  }

  .code-btn {
    width: 80px;
    font-size: 12px;
  }

  .code-input-wrapper {
    gap: 8px;
  }

  :deep(.el-input__inner) {
    font-size: 14px;
  }
  :deep(.el-input__wrapper) {
    height: 42px;
  }
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  .auth-btn {
    height: 42px;
    font-size: 15px;
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
  .code-btn {
    transition: none;
  }
}
</style>