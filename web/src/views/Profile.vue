<!-- src/views/Profile.vue -->
<template>
  <div class="profile-container">
    <!-- 动态背景层 -->
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="bubble" v-for="i in 12" :key="i" :style="getBubbleStyle(i)"></div>
    </div>

    <!-- 整体居中容器 -->
    <div class="profile-wrapper">
      <div class="profile-header">
        <h2 class="page-title">个人中心</h2>
        <p class="subtitle">查看和修改您的个人信息</p>
      </div>

      <el-card class="profile-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">基本信息</span>
            <el-button
                type="primary"
                size="default"
                @click="handleEdit"
            >
              {{ isEditing ? '取消编辑' : '编辑信息' }}
            </el-button>
          </div>
        </template>

        <!-- 表单外层居中容器 -->
        <div class="form-center-wrap">
          <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="rules"
              label-width="120px"
              label-position="right"
              :disabled="!isEditing"
          >
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" disabled size="large" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="用户ID" prop="id">
                  <el-input v-model="profileForm.id" disabled size="large" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="昵称" prop="nickname">
                  <el-input
                      v-model="profileForm.nickname"
                      placeholder="请输入昵称"
                      maxlength="100"
                      show-word-limit
                      size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input
                      v-model="profileForm.name"
                      placeholder="请输入姓名"
                      maxlength="50"
                      show-word-limit
                      size="large"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input
                      v-model="profileForm.phone"
                      placeholder="请输入手机号"
                      maxlength="11"
                      size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="出生日期" prop="birthDate">
                  <el-date-picker
                      v-model="profileForm.birthDate"
                      type="date"
                      placeholder="请选择出生日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      style="width: 100%"
                      size="large"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="最高学历" prop="education">
                  <el-select
                      v-model="profileForm.education"
                      placeholder="请选择最高学历"
                      style="width: 100%"
                      size="large"
                  >
                    <el-option label="高中" value="高中" />
                    <el-option label="大专" value="大专" />
                    <el-option label="本科" value="本科" />
                    <el-option label="硕士" value="硕士" />
                    <el-option label="博士" value="博士" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="工作年限" prop="workYears">
                  <el-input-number
                      v-model="profileForm.workYears"
                      :min="0"
                      :max="50"
                      placeholder="请输入工作年限"
                      style="width: 100%"
                      size="large"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="所在城市" prop="city">
                  <el-input
                      v-model="profileForm.city"
                      placeholder="请输入所在城市"
                      maxlength="50"
                      show-word-limit
                      size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最后登录" prop="lastLoginAt">
                  <el-input v-model="profileForm.lastLoginAt" disabled size="large" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item v-if="isEditing" label-width="0">
              <div class="form-actions">
                <el-button type="primary" size="large" :loading="saving" @click="handleSave">
                  保存修改
                </el-button>
                <el-button size="large" @click="handleCancel">取消</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/api'

const authStore = useAuthStore()
const profileFormRef = ref()
const isEditing = ref(false)
const saving = ref(false)
const loading = ref(true)

// 表单数据
const profileForm = reactive<UserProfile>({
  id: 0,
  email: '',
  nickname: '',
  name: '',
  phone: '',
  birthDate: '',
  education: '',
  workYears: 0,
  city: '',
  lastLoginAt: ''
})

// 表单验证规则
const rules = {
  nickname: [
    { max: 100, message: '昵称不能超过100个字符', trigger: 'blur' }
  ],
  name: [
    { max: 50, message: '姓名不能超过50个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  birthDate: [
    {
      validator: (_: any, value: string, callback: any) => {
        if (value) {
          const date = new Date(value)
          if (isNaN(date.getTime())) {
            callback(new Error('请输入正确的日期格式'))
          }
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  education: [
    {
      validator: (_: any, value: string, callback: any) => {
        const validEducations = ['高中', '大专', '本科', '硕士', '博士']
        if (value && !validEducations.includes(value)) {
          callback(new Error('请选择正确的学历'))
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  workYears: [
    { type: 'number', min: 0, max: 50, message: '工作年限应在0-50之间', trigger: 'blur' }
  ],
  city: [
    { max: 50, message: '城市名称不能超过50个字符', trigger: 'blur' }
  ]
}

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const result = await authStore.fetchUserInfo()
    if (result.success && result.data) {
      Object.assign(profileForm, result.data)
    } else {
      if (authStore.userInfo) {
        Object.assign(profileForm, authStore.userInfo)
      } else {
        ElMessage.warning('无法获取用户信息，请刷新重试')
      }
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// 编辑按钮点击
const handleEdit = () => {
  if (isEditing.value) {
    handleCancel()
  } else {
    isEditing.value = true
  }
}

// 取消编辑
const handleCancel = () => {
  isEditing.value = false
  loadUserInfo()
}

// 保存修改
const handleSave = async () => {
  if (!profileFormRef.value) return

  try {
    await profileFormRef.value.validate()
  } catch {
    ElMessage.warning('请完善表单信息')
    return
  }

  saving.value = true
  try {
    const updateData = {
      nickname: profileForm.nickname,
      name: profileForm.name,
      phone: profileForm.phone,
      birthDate: profileForm.birthDate,
      education: profileForm.education,
      workYears: profileForm.workYears,
      city: profileForm.city
    }

    const result = await authStore.updateProfile(updateData)

    if (result.success) {
      isEditing.value = false
      await loadUserInfo()
      ElMessage.success('个人信息更新成功')
    }
  } catch (error: any) {
    console.error('更新个人信息失败:', error)
    ElMessage.error(error.message || '更新个人信息失败，请稍后重试')
  } finally {
    saving.value = false
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
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  box-sizing: border-box;
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

/* ===== 内容外层容器 垂直水平居中 ===== */
.profile-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 820px;
  /* 取消固定高度，内容自适应且整体在视口中居中 */
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-header {
  text-align: center;
  flex-shrink: 0;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #2c4d3d;
  margin: 0 0 4px;
  letter-spacing: 2px;
}

.subtitle {
  font-size: 14px;
  color: #6b8a7a;
  margin: 0;
}

/* ===== 卡片 取消flex滚动布局 ===== */
.profile-card {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(100, 163, 134, 0.15);
  overflow: visible;
}

:deep(.profile-card .el-card__body) {
  padding: 14px 20px;
  /* 移除overflow-y:auto，不再出现滚动条 */
}

:deep(.profile-card .el-card__header) {
  padding: 12px 20px;
  border-bottom: 1px solid rgba(100, 163, 134, 0.10);
  background: transparent;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c4d3d;
}

/* 表单整体水平居中容器 */
.form-center-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
}
:deep(.el-form) {
  width: 92%;
}

/* ===== 按钮 ===== */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #64A386 0%, #4c8a6e 100%);
  border: none;
}
:deep(.el-button--primary:hover) {
  opacity: 0.9;
}

/* ===== 表单紧凑布局 缩小间距 ===== */
:deep(.el-form-item) {
  margin-bottom: 10px;
}

:deep(.el-form-item__label) {
  color: #4a6b5d;
  font-weight: 500;
  font-size: 14px;
  padding-right: 10px;
}

:deep(.el-form-item.is-disabled .el-input__wrapper) {
  background-color: rgba(245, 247, 250, 0.6);
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.12);
  background: rgba(255, 255, 255, 0.6);
  padding: 0 14px;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(100, 163, 134, 0.30);
}
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.25);
}

:deep(.el-input__inner) {
  font-size: 14px;
  height: 34px;
}

:deep(.el-input__wrapper .el-input__inner) {
  height: 34px;
}

:deep(.el-input-number) {
  width: 100%;
}
:deep(.el-input-number .el-input__wrapper) {
  padding-left: 36px;
}

:deep(.el-date-editor) {
  width: 100%;
}
:deep(.el-date-editor .el-input__wrapper) {
  padding: 0 10px 0 8px;
}

:deep(.el-select .el-input__wrapper) {
  padding-left: 10px;
}

:deep(.el-input-number .el-input-number__decrease),
:deep(.el-input-number .el-input-number__increase) {
  height: 32px;
  width: 32px;
}

/* ===== 表单操作按钮 ===== */
.form-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(100, 163, 134, 0.10);
  margin-top: 4px;
}

.form-actions .el-button {
  padding: 8px 30px;
  font-size: 15px;
  border-radius: 10px;
  min-width: 110px;
}

/* ===== 禁用状态 ===== */
:deep(.el-form-item.is-disabled .el-input__wrapper) {
  background: rgba(245, 247, 250, 0.5);
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.08);
}
:deep(.el-form-item.is-disabled .el-input__inner) {
  color: #6b8a7a;
  font-size: 14px;
}

/* 移除滚动条相关样式（不再需要滚动） */
:deep(.el-card__body::-webkit-scrollbar) {
  display: none;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
    height: 100dvh;
  }

  .profile-wrapper {
    max-width: 100%;
    gap: 10px;
  }

  .page-title {
    font-size: 24px;
  }

  .subtitle {
    font-size: 13px;
  }

  :deep(.el-col) {
    flex: 0 0 100%;
    max-width: 100%;
  }

  :deep(.profile-card .el-card__header) {
    padding: 10px 14px;
  }

  :deep(.profile-card .el-card__body) {
    padding: 10px 14px;
  }

  :deep(.el-form-item) {
    margin-bottom: 8px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }

  :deep(.el-input__inner) {
    height: 32px;
    font-size: 13px;
  }

  :deep(.el-input__wrapper) {
    padding: 0 10px;
  }

  .card-header {
    gap: 8px;
  }

  .card-header .el-button {
    font-size: 13px;
    padding: 5px 14px;
  }

  .form-actions {
    gap: 8px;
    padding-top: 10px;
  }

  .form-actions .el-button {
    padding: 7px 16px;
    font-size: 14px;
    flex: 1;
  }
}

@media (max-width: 480px) {
  .profile-container {
    padding: 6px;
  }

  .page-title {
    font-size: 20px;
  }

  .subtitle {
    font-size: 12px;
  }

  .profile-header {
    margin-bottom: 0;
  }

  :deep(.el-input__inner) {
    height: 30px;
    font-size: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 12px;
    padding-right: 6px;
  }

  :deep(.el-form-item) {
    margin-bottom: 6px;
  }

  :deep(.profile-card .el-card__body) {
    padding: 8px 10px;
  }

  .form-actions .el-button {
    padding: 6px 12px;
    font-size: 13px;
  }
}
</style>
