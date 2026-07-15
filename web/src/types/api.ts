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

// ============ 岗位匹配推荐相关类型 ============

/**
 * 推荐来源枚举（用于 JobRecommendation.source）
 */
export type JobRecommendSource = 'NETWORK' | 'PLATFORM';

/**
 * 推荐来源数字枚举（用于历史记录）
 */
export enum RecommendSource {
  NETWORK = 1,  // 联网搜索
  PLATFORM = 2  // 平台内
}

/**
 * 推荐岗位详情
 */
export interface JobRecommendation {
  jobName: string;
  jdContent: string;
  matchScore: number;
  matchReason: string;
  source: JobRecommendSource;  // 'NETWORK' | 'PLATFORM'
  sourceUrl: string | null;
  sourceJobId: number | null;
}

/**
 * 改进建议
 */
export interface ImprovementSuggestion {
  problemDescription: string;
  suggestions: string[];
  priority: 'high' | 'medium' | 'low';
}

/**
 * 岗位推荐结果
 */
export interface MatchRecommendResult {
  resumeId: number;
  recommendations: JobRecommendation[];
  improvementSuggestion: ImprovementSuggestion | null;
  createdAt: string;
}

/**
 * 岗位推荐请求
 */
export interface MatchRecommendRequest {
  resumeId: number;
  enableWebSearch?: boolean;  // 默认 true
}

/**
 * 推荐记录列表项
 */
export interface MatchRecommendRecord {
  id: number;
  resumeId: number;
  jobName: string;
  matchScore: number;
  source: 1 | 2;  // 1-联网搜索，2-平台内
  createdAt: string;
}

/**
 * 推荐记录详情（继承列表项，增加详情字段）
 */
export interface MatchRecommendDetail extends MatchRecommendRecord {
  jobId: number | null;
  jdContent: string;
  matchReason: string;
  sourceUrl: string | null;
  sourceJobId: number | null;
  hasImprovementSuggestion: 0 | 1;
  improvementSuggestion: ImprovementSuggestion | null;
}

/**
 * 推荐历史列表响应
 */
export interface MatchRecommendHistoryResponse {
  content: MatchRecommendRecord[];
  total: number;
  page: number;
  size: number;
}

// ============ 其他已有类型 ============

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