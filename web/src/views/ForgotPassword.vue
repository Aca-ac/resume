<!-- src/views/ForgotPassword.vue -->
<template>
  <div class="auth-container">
    <div class="auth-card">
      <!-- 左侧图片区域 - 添加装饰性角色 -->
      <div class="auth-left" role="presentation" aria-hidden="true">
        <img
            :src="forgotImage"
            alt=""
            class="auth-image"
            role="presentation"
            aria-hidden="true"
        />
      </div>

      <!-- 右侧表单区域 -->
      <div class="auth-right">
        <h1 class="auth-title">忘记密码</h1>
        <p class="auth-subtitle">输入您的邮箱，我们将发送重置链接</p>

        <!-- 表单 - 添加正确的语义和标签 -->
        <el-form
            ref="forgotFormRef"
            :model="forgotForm"
            :rules="rules"
            label-width="0"
            @submit.prevent="handleSubmit"
            novalidate
        >
          <el-form-item prop="email">
            <el-input
                v-model="forgotForm.email"
                placeholder="请输入邮箱"
                size="large"
                prefix-icon="Message"
                type="email"
                id="email-input"
                name="email"
                autocomplete="email"
                aria-label="电子邮箱地址"
                aria-describedby="email-description"
            />
            <!-- 辅助说明文字，用于提供额外上下文 -->
            <span id="email-description" class="sr-only">
              请输入您的注册邮箱地址，我们将向该邮箱发送密码重置链接
            </span>
          </el-form-item>

          <!-- 错误信息显示区域 - 确保屏幕阅读器可访问 -->
          <div
              v-if="formError"
              class="form-error-message"
              role="alert"
              aria-live="polite"
              aria-atomic="true"
          >
            <span class="error-icon" aria-hidden="true">⚠️</span>
            {{ formError }}
          </div>

          <el-button
              type="primary"
              size="large"
              class="auth-btn"
              :loading="loading"
              @click="handleSubmit"
              :aria-label="loading ? '正在发送重置邮件...' : '发送重置邮件'"
              :disabled="loading"
          >
            <span v-if="!loading">发送重置邮件</span>
            <span v-else>发送中...</span>
          </el-button>
        </el-form>

        <div class="auth-footer">
          想起密码了？
          <router-link to="/login" class="auth-link" aria-label="返回登录页面">
            返回登录
          </router-link>
        </div>

        <!-- 成功消息 - 当操作成功时显示 -->
        <div
            v-if="successMessage"
            class="success-message"
            role="status"
            aria-live="polite"
            aria-atomic="true"
        >
          <span class="success-icon" aria-hidden="true">✓</span>
          {{ successMessage }}
        </div>
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

const forgotFormRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)
const formError = ref('')
const successMessage = ref('')

// 左侧图片 - 装饰性图片使用空alt
const forgotImage = new URL('@/assets/resumepicture/picture010.jpg', import.meta.url).href

const forgotForm = ref({
  email: ''
})

// 验证规则 - 添加更详细的错误消息
const rules = {
  email: [
    {
      required: true,
      message: '请输入邮箱地址',
      trigger: 'blur'
    },
    {
      type: 'email',
      message: '请输入正确的邮箱格式（例如：user@example.com）',
      trigger: 'blur'
    }
  ]
}

const handleSubmit = async () => {
  // 清除之前的状态
  formError.value = ''
  successMessage.value = ''

  if (!forgotFormRef.value) return

  try {
    // 手动验证表单
    const valid = await new Promise<boolean>((resolve) => {
      forgotFormRef.value?.validate((valid) => {
        resolve(valid)
      })
    })

    if (!valid) {
      // 聚焦到第一个错误字段
      const firstErrorInput = document.querySelector('.el-form-item.is-error input') as HTMLElement
      if (firstErrorInput) {
        firstErrorInput.focus()
      }
      return
    }

    loading.value = true

    const result = await authStore.sendCode(forgotForm.value.email)

    if (result.success) {
      successMessage.value = `重置链接已发送至 ${forgotForm.value.email}，请查收邮件`

      // 延迟跳转，让用户看到成功消息
      setTimeout(() => {
        router.push({
          path: '/reset-password',
          query: { email: forgotForm.value.email }
        })
      }, 2000)
    } else {
      formError.value = result.message || '发送失败，请稍后重试'
      // 聚焦到错误区域
      const errorElement = document.querySelector('.form-error-message') as HTMLElement
      if (errorElement) {
        errorElement.focus({ preventScroll: true })
      }
    }
  } catch (error: any) {
    formError.value = error?.message || '网络错误，请检查您的连接后重试'
    const errorElement = document.querySelector('.form-error-message') as HTMLElement
    if (errorElement) {
      errorElement.focus({ preventScroll: true })
    }
    console.error('Forgot password error:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 可访问性优先的绿色主题 ===== */

/* 屏幕阅读器专用 - 隐藏但有意义的内容 */
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

/* ===== 表单错误消息 - 可访问性增强 ===== */
.form-error-message {
  background-color: #fef2f2;
  color: #b91c1c;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  border-left: 4px solid #dc2626;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  /* 确保焦点可见 */
  outline: 2px solid transparent;
  outline-offset: 2px;
}

.form-error-message:focus-visible {
  outline-color: #dc2626;
}

.error-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* ===== 成功消息 ===== */
.success-message {
  background-color: #ecfdf5;
  color: #065f46;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;
  border-left: 4px solid #059669;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.success-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* ===== 按钮样式 - 高对比度 ===== */
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
  /* 确保按钮有足够的对比度 - 白色文字在绿色背景上约为4.5:1 */
  transition: opacity 0.2s, transform 0.1s, box-shadow 0.2s;
  cursor: pointer;
}

.auth-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.auth-btn:focus-visible {
  outline: 3px solid #2c4d3d;
  outline-offset: 2px;
  box-shadow: 0 0 0 4px rgba(100, 163, 134, 0.3);
}

.auth-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== 输入框样式 - 高对比度焦点指示 ===== */
:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 48px;
  box-shadow: 0 0 0 1px #6b8a7a;
  transition: box-shadow 0.2s;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4c8a6e, 0 0 0 4px rgba(76, 138, 110, 0.15);
}

:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.15);
}

/* 错误状态下的输入框要有视觉指示 */
:deep(.el-form-item.is-error .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #dc2626, 0 0 0 4px rgba(220, 38, 38, 0.25);
}

/* ===== 链接样式 - 高对比度 ===== */
.auth-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: #4a5e51;
}

.auth-link {
  color: #2c6b4f;
  text-decoration: underline;
  font-weight: 600;
}

.auth-link:hover {
  color: #1a4d36;
  text-decoration: underline;
}

.auth-link:focus-visible {
  outline: 2px solid #2c6b4f;
  outline-offset: 2px;
  border-radius: 4px;
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

/* ===== 高对比度模式支持 ===== */
@media (prefers-contrast: high) {
  .auth-btn {
    background: #2c6b4f;
    border: 2px solid #1a4d36;
  }

  .auth-btn:hover:not(:disabled) {
    background: #1a4d36;
  }

  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 2px #2c4d3d;
  }

  .auth-link {
    color: #1a4d36;
  }

  .form-error-message {
    border: 2px solid #dc2626;
  }

  .success-message {
    border: 2px solid #059669;
  }
}

/* ===== 减少动画偏好 ===== */
@media (prefers-reduced-motion: reduce) {
  .auth-btn {
    transition: none;
  }

  .auth-btn:active:not(:disabled) {
    transform: none;
  }
}

/* ===== 确保所有交互元素有足够的点击区域 ===== */
.auth-btn,
.auth-link {
  min-height: 44px;
  min-width: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>