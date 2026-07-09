<!-- src/views/ForgotPassword.vue -->
<template>
  <div class="auth-container">
    <div class="auth-card">
      <h1 class="auth-title">忘记密码</h1>
      <p class="auth-subtitle">输入您的邮箱，我们将发送重置链接</p>

      <el-form
          ref="forgotFormRef"
          :model="forgotForm"
          :rules="rules"
          label-width="0"
          @submit.prevent="handleSubmit"
      >
        <el-form-item prop="email">
          <el-input
              v-model="forgotForm.email"
              placeholder="请输入邮箱"
              size="large"
              prefix-icon="Message"
          />
        </el-form-item>

        <el-button
            type="primary"
            size="large"
            class="auth-btn"
            :loading="loading"
            @click="handleSubmit"
        >
          发送重置邮件
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const forgotFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)

const forgotForm = ref({
  email: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!forgotFormRef.value) return

  await forgotFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const result = await authStore.sendCode(forgotForm.value.email)
      if (result.success) {
        // 跳转到重置密码页面，带上邮箱参数
        router.push({
          path: '/reset-password',
          query: { email: forgotForm.value.email }
        })
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