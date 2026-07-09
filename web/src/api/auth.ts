// src/api/auth.ts
import request from '@/utils/request'
import type {
  ApiResponse,
  SendCodeRequest,
  SendCodeResponse,
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  RefreshResponse,
  ResetPasswordRequest,
  UserProfile,
  UpdateProfileRequest
} from '@/types/api'

export const authApi = {
  // 发送验证码
  sendCode(data: SendCodeRequest): Promise<ApiResponse<SendCodeResponse>> {
    return request.post<ApiResponse<SendCodeResponse>>('/v1/auth/code/send', data)
  },

  // 注册
  register(data: RegisterRequest): Promise<ApiResponse<RegisterResponse>> {
    return request.post<ApiResponse<RegisterResponse>>('/v1/auth/register', data)
  },

  // 登录
  login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    return request.post<ApiResponse<LoginResponse>>('/v1/auth/login', data)
  },

  // 刷新Token
  refresh(): Promise<ApiResponse<RefreshResponse>> {
    return request.post<ApiResponse<RefreshResponse>>('/v1/auth/refresh')
  },

  // 重置密码
  resetPassword(data: ResetPasswordRequest): Promise<ApiResponse<void>> {
    return request.post<ApiResponse<void>>('/v1/auth/password/reset', data)
  },

  // 获取个人信息
  getProfile(): Promise<ApiResponse<UserProfile>> {
    return request.get<ApiResponse<UserProfile>>('/v1/user/profile')
  },

  // 更新个人信息
  updateProfile(data: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> {
    return request.put<ApiResponse<UserProfile>>('/v1/user/profile', data)
  }
}