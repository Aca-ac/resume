<!-- src/views/Profile.vue -->
<template>
  <div class="profile-container" role="main" aria-labelledby="page-title">
    <!-- 动态背景层 - 纯装饰，对辅助技术隐藏 -->
    <div class="bg-layer" aria-hidden="true">
      <div class="bg-gradient"></div>
      <div class="bubble" v-for="i in 12" :key="i" :style="getBubbleStyle(i)"></div>
    </div>

    <!-- 整体居中容器 -->
    <div class="profile-wrapper">
      <!-- 返回主页按钮（右上角） -->
      <div class="header-top">
        <div class="header-title-group">
          <h1 id="page-title" class="page-title">个人中心</h1>
          <p class="subtitle">查看和修改您的个人信息</p>
        </div>
        <el-button
            class="back-home-btn"
            link
            @click="goHome"
            aria-label="返回主页"
        >
          <el-icon aria-hidden="true"><ArrowLeft /></el-icon>
          返回主页
        </el-button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state" role="status" aria-live="polite">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-card v-else class="profile-card" shadow="hover" aria-label="个人信息卡片">
        <template #header>
          <div class="card-header">
            <span class="card-title" id="form-title">基本信息</span>
            <el-button
                type="primary"
                size="default"
                @click="handleEdit"
                :aria-expanded="isEditing"
                :aria-controls="isEditing ? 'profile-form' : undefined"
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
              id="profile-form"
              :aria-labelledby="isEditing ? 'form-title' : undefined"
              novalidate
          >
            <!-- 只读信息区域 -->
            <fieldset class="readonly-fieldset" :disabled="true" aria-label="账户信息">
              <legend class="sr-only">账户信息</legend>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                    <label for="email-display" class="sr-only">邮箱地址</label>
                    <el-input id="email-display" v-model="profileForm.email" disabled size="large" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="用户ID" prop="id">
                    <label for="id-display" class="sr-only">用户ID</label>
                    <el-input id="id-display" v-model="profileForm.id" disabled size="large" />
                  </el-form-item>
                </el-col>
              </el-row>
            </fieldset>

            <!-- 可编辑信息区域 -->
            <fieldset class="editable-fieldset" :disabled="!isEditing" aria-label="可编辑个人信息">
              <legend class="sr-only">可编辑个人信息</legend>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="昵称" prop="nickname">
                    <label for="nickname-input" class="sr-only">昵称</label>
                    <el-input
                        id="nickname-input"
                        v-model="profileForm.nickname"
                        placeholder="请输入昵称"
                        maxlength="100"
                        show-word-limit
                        size="large"
                        aria-describedby="nickname-hint"
                        aria-required="false"
                    />
                    <div id="nickname-hint" class="hint-text">最多100个字符</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="姓名" prop="name">
                    <label for="name-input" class="sr-only">姓名</label>
                    <el-input
                        id="name-input"
                        v-model="profileForm.name"
                        placeholder="请输入姓名"
                        maxlength="50"
                        show-word-limit
                        size="large"
                        aria-describedby="name-hint"
                        aria-required="false"
                    />
                    <div id="name-hint" class="hint-text">最多50个字符</div>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="手机号" prop="phone">
                    <label for="phone-input" class="sr-only">手机号</label>
                    <el-input
                        id="phone-input"
                        v-model="profileForm.phone"
                        placeholder="请输入手机号"
                        maxlength="11"
                        size="large"
                        type="tel"
                        aria-describedby="phone-error phone-hint"
                        aria-required="false"
                    />
                    <div id="phone-hint" class="hint-text">11位手机号码</div>
                    <div id="phone-error" role="alert" aria-live="polite">
                      <span v-if="phoneError" class="error-message">{{ phoneError }}</span>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="出生日期" prop="birthDate">
                    <label for="birthdate-input" class="sr-only">出生日期</label>
                    <el-date-picker
                        id="birthdate-input"
                        v-model="profileForm.birthDate"
                        type="date"
                        placeholder="请选择出生日期"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        style="width: 100%"
                        size="large"
                        aria-describedby="birthdate-error"
                        aria-required="false"
                    />
                    <div id="birthdate-error" role="alert" aria-live="polite">
                      <span v-if="birthDateError" class="error-message">{{ birthDateError }}</span>
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="最高学历" prop="education">
                    <label for="education-select" class="sr-only">最高学历</label>
                    <el-select
                        id="education-select"
                        v-model="profileForm.education"
                        placeholder="请选择最高学历"
                        style="width: 100%"
                        size="large"
                        aria-describedby="education-error"
                        aria-required="false"
                    >
                      <el-option label="高中" value="高中" />
                      <el-option label="大专" value="大专" />
                      <el-option label="本科" value="本科" />
                      <el-option label="硕士" value="硕士" />
                      <el-option label="博士" value="博士" />
                    </el-select>
                    <div id="education-error" role="alert" aria-live="polite">
                      <span v-if="educationError" class="error-message">{{ educationError }}</span>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="工作年限" prop="workYears">
                    <label for="workyears-input" class="sr-only">工作年限</label>
                    <el-input-number
                        id="workyears-input"
                        v-model="profileForm.workYears"
                        :min="0"
                        :max="50"
                        placeholder="请输入工作年限"
                        style="width: 100%"
                        size="large"
                        aria-describedby="workyears-hint workyears-error"
                        aria-required="false"
                    />
                    <div id="workyears-hint" class="hint-text">0-50年</div>
                    <div id="workyears-error" role="alert" aria-live="polite">
                      <span v-if="workYearsError" class="error-message">{{ workYearsError }}</span>
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="所在城市" prop="city">
                    <label for="city-input" class="sr-only">所在城市</label>
                    <el-input
                        id="city-input"
                        v-model="profileForm.city"
                        placeholder="请输入所在城市"
                        maxlength="50"
                        show-word-limit
                        size="large"
                        aria-describedby="city-hint"
                        aria-required="false"
                    />
                    <div id="city-hint" class="hint-text">最多50个字符</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="最后登录" prop="lastLoginAt">
                    <label for="lastlogin-display" class="sr-only">最后登录时间</label>
                    <el-input id="lastlogin-display" v-model="profileForm.lastLoginAt" disabled size="large" />
                  </el-form-item>
                </el-col>
              </el-row>
            </fieldset>

            <!-- 表单操作按钮 -->
            <div v-if="isEditing" class="form-actions" role="group" aria-label="表单操作">
              <el-button
                  type="primary"
                  size="large"
                  :loading="saving"
                  @click="handleSave"
                  :aria-busy="saving"
                  :disabled="saving"
              >
                {{ saving ? '保存中...' : '保存修改' }}
              </el-button>
              <el-button size="large" @click="handleCancel" :disabled="saving">
                取消
              </el-button>
            </div>

            <!-- 表单状态提示 -->
            <div role="status" aria-live="polite" class="sr-only">
              {{ isEditing ? '当前处于编辑模式' : '当前处于查看模式' }}
              {{ saving ? '正在保存...' : '' }}
            </div>
          </el-form>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()
const profileFormRef = ref()
const isEditing = ref(false)
const saving = ref(false)
const loading = ref(true)

// 手动错误状态
const phoneError = ref('')
const birthDateError = ref('')
const educationError = ref('')
const workYearsError = ref('')

// 原始数据备份（用于取消时恢复）
const originalFormData = ref<UserProfile | null>(null)

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

// 表单验证规则（增强错误信息）
const rules = {
  nickname: [
    { max: 100, message: '昵称不能超过100个字符', trigger: 'blur' }
  ],
  name: [
    { max: 50, message: '姓名不能超过50个字符', trigger: 'blur' }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的11位手机号',
      trigger: 'blur'
    }
  ],
  birthDate: [
    {
      validator: (_: any, value: string, callback: any) => {
        if (value) {
          const date = new Date(value)
          if (isNaN(date.getTime())) {
            callback(new Error('请输入正确的日期格式（YYYY-MM-DD）'))
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
          callback(new Error('请选择正确的学历选项'))
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  workYears: [
    { type: 'number', min: 0, max: 50, message: '工作年限应在0-50年之间', trigger: 'blur' }
  ],
  city: [
    { max: 50, message: '城市名称不能超过50个字符', trigger: 'blur' }
  ]
}

// 实时验证函数
const validatePhone = (value: string) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    phoneError.value = '请输入正确的11位手机号'
    return false
  }
  phoneError.value = ''
  return true
}

const validateWorkYears = (value: number) => {
  if (value !== undefined && value !== null && (value < 0 || value > 50)) {
    workYearsError.value = '工作年限应在0-50年之间'
    return false
  }
  workYearsError.value = ''
  return true
}

// 监听输入进行实时验证
watch(() => profileForm.phone, (val) => {
  if (isEditing.value && val) validatePhone(val)
})

watch(() => profileForm.workYears, (val) => {
  if (isEditing.value && val !== undefined && val !== null) validateWorkYears(val)
})

// 返回主页
const goHome = () => {
  router.push('/dashboard')
}

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const result = await authStore.fetchUserInfo()
    if (result.success && result.data) {
      Object.assign(profileForm, result.data)
    } else if (authStore.userInfo) {
      Object.assign(profileForm, authStore.userInfo)
    } else {
      ElMessage.warning('无法获取用户信息，请刷新重试')
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

// 备份当前数据
const backupFormData = () => {
  originalFormData.value = JSON.parse(JSON.stringify(profileForm))
}

// 编辑按钮点击
const handleEdit = () => {
  if (isEditing.value) {
    handleCancel()
  } else {
    isEditing.value = true
    backupFormData()
    // 聚焦到第一个可编辑字段
    nextTick(() => {
      document.getElementById('nickname-input')?.focus()
    })
  }
}

// 取消编辑
const handleCancel = () => {
  if (originalFormData.value) {
    Object.assign(profileForm, originalFormData.value)
    originalFormData.value = null
  }
  // 清除错误状态
  phoneError.value = ''
  birthDateError.value = ''
  educationError.value = ''
  workYearsError.value = ''
  isEditing.value = false
}

// 保存修改
const handleSave = async () => {
  if (!profileFormRef.value) return

  // 手动验证
  const isPhoneValid = validatePhone(profileForm.phone)
  const isWorkYearsValid = validateWorkYears(profileForm.workYears)

  try {
    await profileFormRef.value.validate()
  } catch {
    // 聚焦到第一个错误字段
    const firstError = document.querySelector('.el-form-item.is-error input, .el-form-item.is-error .el-input__inner')
    if (firstError) {
      (firstError as HTMLElement).focus()
    }
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
      originalFormData.value = null
      isEditing.value = false
      await loadUserInfo()
      ElMessage.success('个人信息更新成功')
      // 聚焦到"编辑信息"按钮
      ;(document.querySelector('.card-header .el-button') as HTMLElement | null)?.focus()
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

// 键盘快捷键：Escape 取消编辑
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isEditing.value) {
    handleCancel()
    ;(document.querySelector('.card-header .el-button') as HTMLElement | null)?.focus()
  }
}

onMounted(() => {
  loadUserInfo()
  document.addEventListener('keydown', handleKeydown)
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

/* ===== 加载状态 ===== */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  color: #2c4d3d;
  font-size: 16px;
}

.loading-state .is-loading {
  font-size: 28px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 内容外层容器 ===== */
.profile-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 820px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ===== 顶部标题区域 ===== */
.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
  padding: 0 4px;
}

.header-title-group {
  text-align: center;
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a3a2a;
  margin: 0 0 4px;
  letter-spacing: 2px;
}

.subtitle {
  font-size: 14px;
  color: #4a6a5a;
  margin: 0;
}

/* ===== 返回主页按钮 ===== */
.back-home-btn {
  color: #2d6b4f;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 14px;
  border-radius: 20px;
  transition: all 0.2s ease;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  white-space: nowrap;
}

.back-home-btn:hover,
.back-home-btn:focus {
  background: rgba(255, 255, 255, 0.7);
  color: #1a3a2a;
  transform: translateX(-2px);
}
.back-home-btn:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
  border-radius: 20px;
}
.back-home-btn .el-icon {
  font-size: 16px;
}

/* ===== 卡片 ===== */
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
  color: #1a3a2a;
}

/* ===== Fieldset 样式 ===== */
.readonly-fieldset,
.editable-fieldset {
  border: none;
  margin: 0;
  padding: 0;
}

.readonly-fieldset:disabled {
  opacity: 1;
}

.editable-fieldset:disabled {
  opacity: 0.7;
}

/* ===== 表单样式 ===== */
.form-center-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
}
:deep(.el-form) {
  width: 92%;
}

/* ===== 按钮样式 - 增强对比度 ===== */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #2d6b4f 0%, #1a4d36 100%);
  border: none;
  color: #ffffff;
  font-weight: 500;
}
:deep(.el-button--primary:hover:not(:disabled)) {
  opacity: 0.9;
  box-shadow: 0 4px 12px rgba(45, 107, 79, 0.3);
}
:deep(.el-button--primary:focus-visible) {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
}
:deep(.el-button--primary:disabled) {
  opacity: 0.6;
  cursor: not-allowed;
}

.card-header .el-button {
  background: linear-gradient(135deg, #2d6b4f 0%, #1a4d36 100%);
  border: none;
  color: #ffffff;
}
.card-header .el-button:hover {
  opacity: 0.9;
}
.card-header .el-button:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
}

/* ===== 表单输入框 ===== */
:deep(.el-form-item) {
  margin-bottom: 10px;
}

:deep(.el-form-item__label) {
  color: #2d4a3a;
  font-weight: 500;
  font-size: 14px;
  padding-right: 10px;
}

:deep(.el-form-item.is-disabled .el-input__wrapper) {
  background-color: rgba(245, 247, 250, 0.6);
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.2);
  background: rgba(255, 255, 255, 0.6);
  padding: 0 14px;
  transition: box-shadow 0.2s;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px #2d6b4f;
}
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 2px #c62828;
}
:deep(.el-input__wrapper:hover:not(.is-disabled)) {
  box-shadow: 0 0 0 2px rgba(45, 107, 79, 0.3);
}

:deep(.el-input__inner) {
  font-size: 14px;
  height: 34px;
  color: #1a2a1f;
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
  font-weight: 500;
}
.form-actions .el-button:focus-visible {
  outline: 3px solid #2d6b4f;
  outline-offset: 2px;
}

/* ===== 禁用状态 ===== */
:deep(.el-form-item.is-disabled .el-input__wrapper) {
  background: rgba(245, 247, 250, 0.5);
  box-shadow: 0 0 0 1px rgba(100, 163, 134, 0.08);
}
:deep(.el-form-item.is-disabled .el-input__inner) {
  color: #4a6a5a;
  font-size: 14px;
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

  .header-top {
    gap: 8px;
    flex-wrap: wrap;
  }

  .back-home-btn {
    font-size: 13px;
    padding: 4px 12px;
  }
  .back-home-btn .el-icon {
    font-size: 14px;
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

  .header-top {
    flex-direction: column;
    gap: 4px;
    align-items: center;
  }

  .header-title-group {
    order: 1;
  }

  .back-home-btn {
    order: 2;
    font-size: 12px;
    padding: 3px 10px;
    align-self: flex-end;
  }
  .back-home-btn .el-icon {
    font-size: 12px;
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

/* ===== 减少动画偏好 ===== */
@media (prefers-reduced-motion: reduce) {
  .bg-gradient {
    animation: none;
  }
  .bubble {
    animation: none !important;
    display: none !important;
  }
  .loading-state .is-loading {
    animation: none;
  }
  .back-home-btn {
    transition: none;
  }
}
</style>