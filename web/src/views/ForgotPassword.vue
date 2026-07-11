<!-- src/views/ForgotPassword.vue -->
<template>
  <div class="auth-container">
    <div class="auth-card">
      <!-- 左侧图片区域 -->
      <div class="auth-left">
        <img :src="forgotImage" alt="找回密码插图" class="auth-image" />
      </div>

      <!-- 右侧表单区域 - 保留所有原有功能 -->
      <div class="auth-right">
        <h1 class="auth-title">忘记密码</h1>
        <p class="auth-subtitle">输入您的邮箱，我们将发送重置链接</p>

        <!-- 完全保留原有的表单逻辑 -->
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
  </div>
</template>

<script setup lang="ts">
// 完全保留原有的 script 逻辑，不做任何修改
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const forgotFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)

// 左侧图片 - 请替换为你的实际图片路径
const forgotImage = new URL('@/assets/resumepicture/picture010.jpg', import.meta.url).href

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
/* ===== 新的绿色主题风格 ===== */
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
  padding: 20px;
}

.auth-card {
  display: flex;
  width: 860px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(100, 163, 134, 0.20);
  overflow: hidden;
}

/* ===== 左侧图片区域 ===== */
.auth-left {
  flex: 0 0 45%;
  background: linear-gradient(135deg, #CDE2E8 0%, #BCDDBE 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.auth-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  max-height: 420px;
}

/* ===== 右侧表单区域 ===== */
.auth-right {
  flex: 1;
  padding: 48px 44px;
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

/* ===== 保留原有的输入框样式，只修改颜色主题 ===== */
:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 48px;
  box-shadow: 0 0 0 1px #dce8e2;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.25);
}

/* ===== 响应式适配 ===== */
@media (max-width: 768px) {
  .auth-card {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
  }
  .auth-left {
    flex: 0 0 auto;
    padding: 20px;
    min-height: 160px;
  }
  .auth-image {
    max-height: 140px;
  }
  .auth-right {
    padding: 28px 24px;
  }
}
</style>

