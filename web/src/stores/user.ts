import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { ApiResponse, UserProfile, UpdateProfileRequest } from '@/types/api'
import { ElMessage } from 'element-plus'

interface UserState {
    profile: UserProfile | null
    hasLoaded: boolean
}

export const useUserStore = defineStore('user', {
    state: (): UserState => ({
        profile: null,
        hasLoaded: false
    }),
    getters: {
        hasUserInfo: (state) => !!state.profile,
        nickname: (state) => state.profile?.nickname || '',
        email: (state) => state.profile?.email || ''
    },
    actions: {
        async loadProfile() {
            const res: ApiResponse<UserProfile> = await authApi.getProfile()
            if (res.code !== 200 || !res.data) {
                throw new Error(res.message || '获取个人信息失败')
            }
            this.profile = res.data
            this.hasLoaded = true
            return res.data
        },

        async updateProfile(form: UpdateProfileRequest) {
            const res: ApiResponse<UserProfile> = await authApi.updateProfile(form)
            if (res.code !== 200 || !res.data) {
                throw new Error(res.message || '更新个人信息失败')
            }
            this.profile = res.data
            ElMessage.success('个人信息修改成功')
            return res.data
        },

        clearUserInfo() {
            this.$reset()
        }
    }
})
