import { defineStore } from 'pinia'
import { getUserProfileApi, updateUserProfileApi } from '@/api/user'
import type { ApiResponse, UserProfile, UpdateProfileRequest } from '@/types/api'
import { ElMessage } from 'element-plus'

interface UserState {
    // 用户完整信息
    profile: UserProfile | null
    // 标记是否已经请求过用户信息，避免重复调用接口
    hasLoaded: boolean
}

export const useUserStore = defineStore('user', {
    state: (): UserState => ({
        profile: null,
        hasLoaded: false
    }),
    getters: {
        // 判断是否存在用户信息
        hasUserInfo: (state) => !!state.profile,
        // 快捷获取昵称
        nickname: (state) => state.profile?.nickname || '',
        // 快捷获取邮箱
        email: (state) => state.profile?.email || ''
    },
    actions: {
        /**
         * 获取/刷新个人信息
         * 页面初始化、更新信息后调用
         */
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

        /**
         * 编辑更新个人信息
         * @param form 表单修改字段
         */
        async updateProfile(form: UpdateProfileRequest) {
            const res: ApiResponse<UserProfile> = await updateUserProfileApi(form)
            // 更新本地缓存的用户信息
            this.profile = res.data
            ElMessage.success('个人信息修改成功')
            return res.data
        },

        /**
         * 清空用户信息（登录失效/退出登录时调用）
         */
        clearUserInfo() {
            this.$reset()
        }
    }
})
