import request from '@/utils/request'
import type { ApiResponse, UpdateProfileRequest, UserProfile } from '@/types/api'

export function getUserProfileApi(): Promise<ApiResponse<UserProfile>> {
  return request.get<ApiResponse<UserProfile>>('/v1/user/profile')
}

export function updateUserProfileApi(data: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> {
  return request.put<ApiResponse<UserProfile>>('/v1/user/profile', data)
}
