<!-- src/views/Profile.vue -->
<template>
  <div class="profile-container">
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
              size="small"
              @click="handleEdit"
          >
            {{ isEditing ? '取消编辑' : '编辑信息' }}
          </el-button>
        </div>
      </template>

      <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="rules"
          label-width="120px"
          label-position="right"
          :disabled="!isEditing"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户ID" prop="id">
              <el-input v-model="profileForm.id" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input
                  v-model="profileForm.nickname"
                  placeholder="请输入昵称"
                  maxlength="100"
                  show-word-limit
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
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                  v-model="profileForm.phone"
                  placeholder="请输入手机号"
                  maxlength="11"
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
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最高学历" prop="education">
              <el-select
                  v-model="profileForm.education"
                  placeholder="请选择最高学历"
                  style="width: 100%"
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
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所在城市" prop="city">
              <el-input
                  v-model="profileForm.city"
                  placeholder="请输入所在城市"
                  maxlength="50"
                  show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最后登录" prop="lastLoginAt">
              <el-input v-model="profileForm.lastLoginAt" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="isEditing" label-width="0">
          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="handleSave">
              保存修改
            </el-button>
            <el-button @click="handleCancel">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
      // 更新表单数据
      Object.assign(profileForm, result.data)
    } else {
      // 如果获取失败，尝试使用 store 中已有的数据
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
    // 取消编辑
    handleCancel()
  } else {
    // 进入编辑模式
    // 保存当前数据作为备份，以便取消时恢复
    isEditing.value = true
  }
}

// 取消编辑
const handleCancel = () => {
  isEditing.value = false
  // 重新加载数据，恢复原始值
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
    // 准备更新数据（只包含可修改的字段）
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
      // 重新加载更新后的数据
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

// 页面加载时获取用户信息
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  max-width: 900px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.profile-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a2e;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item.is-disabled .el-input__wrapper) {
  background-color: #f5f7fa;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-date-editor) {
  width: 100%;
}

:deep(.el-card__header) {
  border-bottom: 1px solid #ebeef5;
  padding: 16px 20px;
}

:deep(.el-card__body) {
  padding: 24px 20px;
}
</style>