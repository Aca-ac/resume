<!-- src/views/ResetPassword.vue -->
<template>
  <div class="auth-container">
    <div class="auth-card">
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
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const resetFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)
const codeCountdown = ref(0)
let countdownTimer: number | null = null

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
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  width: 420px;
  padding: 40px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  text-align: center;
  margin: 0 0 8px;
}

.auth-subtitle {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  margin: 0 0 32px;
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
  background: #f3f4f6;
  border: none;
  color: #374151;
  font-size: 14px;
}

.code-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.code-btn:disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

.auth-btn {
  width: 100%;
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  font-weight: 500;
  height: 48px;
}

.auth-btn:hover {
  opacity: 0.9;
}

.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #6b7280;
}

.auth-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.auth-link:hover {
  text-decoration: underline;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 48px;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2);
}
</style>