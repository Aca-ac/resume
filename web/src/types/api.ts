// src/types/api.ts

// 通用API响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 发送验证码请求
export interface SendCodeRequest {
  email: string
}

// 发送验证码响应
export interface SendCodeResponse {
  expireSeconds: number
}

// 注册请求
export interface RegisterRequest {
  email: string
  password: string
  code: string
}

// 注册响应
export interface RegisterResponse {
  accessToken: string
  expiresInSeconds: number
  userId: number
}

// 登录请求
export interface LoginRequest {
  email: string
  password: string
}

// 登录响应
export interface LoginResponse {
  accessToken: string
  expiresInSeconds: number
  userId: number
}

// 刷新Token响应
export interface RefreshResponse {
  accessToken: string
  expiresIn: number
}

// 重置密码请求
export interface ResetPasswordRequest {
  email: string
  password: string
  code: string
}

// 用户信息
export interface UserProfile {
  id: number
  email: string
  nickname: string
  name: string
  phone: string
  birthDate: string
  education: string
  workYears: number
  city: string
  lastLoginAt: string
}

// 更新个人信息请求
export interface UpdateProfileRequest {
  nickname?: string
  name?: string
  phone?: string
  birthDate?: string
  education?: string
  workYears?: number
  city?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}