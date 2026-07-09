// src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserProfile } from '@/types/api'
import { ElMessage } from 'element-plus'

// Token存储Key
const TOKEN_KEY = 'access_token'
const USER_KEY = 'user_info'

// 辅助函数：从localStorage获取用户信息
function getStoredUserInfo(): UserProfile | null {
  try {
    const data = localStorage.getItem(USER_KEY)
    if (data) {
      return JSON.parse(data) as UserProfile
    }
    return null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  // State - 使用辅助函数初始化
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const userInfo = ref<UserProfile | null>(getStoredUserInfo())
  const isRefreshing = ref<boolean>(false)

  // Getters
  const isLoggedIn = computed<boolean>(() => {
    return !!accessToken.value && !!userInfo.value
  })

  const userId = computed<number | null>(() => {
    return userInfo.value?.id || null
  })

  // Actions
  function setToken(token: string): void {
    accessToken.value = token
    localStorage.setItem(TOKEN_KEY, token)
  }

  function setUserInfo(user: UserProfile): void {
    userInfo.value = user
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }

  function clearAuth(): void {
    accessToken.value = null
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  // 登录
  async function login(email: string, password: string) {
    try {
      const res = await authApi.login({ email, password })
      if (res.code === 200) {
        setToken(res.data.accessToken)
        // 登录后获取用户信息
        await fetchUserInfo()
        ElMessage.success(res.message || '登录成功')
        return { success: true, data: res.data }
      } else {
        ElMessage.error(res.message || '登录失败')
        return { success: false, message: res.message }
      }
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败，请稍后重试')
      return { success: false, message: error.message }
    }
  }

  // 注册
  async function register(email: string, password: string, code: string) {
    try {
      const res = await authApi.register({ email, password, code })
      if (res.code === 200) {
        setToken(res.data.accessToken)
        await fetchUserInfo()
        ElMessage.success(res.message || '注册成功')
        return { success: true, data: res.data }
      } else {
        ElMessage.error(res.message || '注册失败')
        return { success: false, message: res.message }
      }
    } catch (error: any) {
      ElMessage.error(error.message || '注册失败，请稍后重试')
      return { success: false, message: error.message }
    }
  }

  // 发送验证码
  async function sendCode(email: string) {
    try {
      const res = await authApi.sendCode({ email })
      if (res.code === 200) {
        ElMessage.success(res.message || '验证码已发送')
        return { success: true, expireSeconds: res.data.expireSeconds }
      } else {
        ElMessage.error(res.message || '发送失败')
        return { success: false, message: res.message }
      }
    } catch (error: any) {
      ElMessage.error(error.message || '发送失败，请稍后重试')
      return { success: false, message: error.message }
    }
  }

  // 重置密码
  async function resetPassword(email: string, password: string, code: string) {
    try {
      const res = await authApi.resetPassword({ email, password, code })
      if (res.code === 200) {
        ElMessage.success(res.message || '密码重置成功')
        return { success: true }
      } else {
        ElMessage.error(res.message || '重置失败')
        return { success: false, message: res.message }
      }
    } catch (error: any) {
      ElMessage.error(error.message || '重置失败，请稍后重试')
      return { success: false, message: error.message }
    }
  }

  // 获取用户信息
  async function fetchUserInfo() {
    try {
      const res = await authApi.getProfile()
      if (res.code === 200) {
        setUserInfo(res.data)
        return { success: true, data: res.data }
      }
      return { success: false, message: res.message }
    } catch (error: any) {
      return { success: false, message: error.message }
    }
  }

  // 更新用户信息
  async function updateProfile(data: Partial<UserProfile>) {
    try {
      const res = await authApi.updateProfile(data)
      if (res.code === 200) {
        setUserInfo(res.data)
        ElMessage.success(res.message || '更新成功')
        return { success: true, data: res.data }
      } else {
        ElMessage.error(res.message || '更新失败')
        return { success: false, message: res.message }
      }
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败，请稍后重试')
      return { success: false, message: error.message }
    }
  }

  // 刷新Token
  async function refreshToken(): Promise<string | null> {
    // 如果正在刷新，等待当前刷新完成
    if (isRefreshing.value) {
      return new Promise((resolve) => {
        const check = () => {
          if (!isRefreshing.value) {
            resolve(accessToken.value)
          } else {
            setTimeout(check, 100)
          }
        }
        check()
      })
    }

    isRefreshing.value = true
    try {
      const res = await authApi.refresh()
      if (res.code === 200) {
        setToken(res.data.accessToken)
        return res.data.accessToken
      }
      return null
    } catch (error) {
      clearAuth()
      return null
    } finally {
      isRefreshing.value = false
    }
  }

  // 登出
  function logout(): void {
    clearAuth()
    ElMessage.success('已退出登录')
  }

  return {
    // State
    accessToken,
    userInfo,
    isRefreshing,
    // Getters
    isLoggedIn,
    userId,
    // Actions
    setToken,
    setUserInfo,
    clearAuth,
    login,
    register,
    sendCode,
    resetPassword,
    fetchUserInfo,
    updateProfile,
    refreshToken,
    logout
  }
})