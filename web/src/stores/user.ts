import { defineStore } from 'pinia'
import { getUserProfileApi, updateUserProfileApi } from '@/api/user'
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
            try {
                const res: ApiResponse<UserProfile> = await getUserProfileApi()
                this.profile = res.data
                this.hasLoaded = true
                return res.data
            } catch (err) {
                this.profile = null
                this.hasLoaded = false
                throw err
            }
        },

        async updateProfile(form: UpdateProfileRequest) {
            const res: ApiResponse<UserProfile> = await updateUserProfileApi(form)
            this.profile = res.data
            ElMessage.success('个人信息修改成功')
            return res.data
        },

        clearUserInfo() {
            this.$reset()
        }
    }
})
