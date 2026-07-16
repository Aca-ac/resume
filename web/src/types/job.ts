// src/types/job.ts

/**
 * 岗位来源枚举
 */
export enum JobSource {
    MANUAL = 0,        // 手动创建
    AI_SEARCH = 1,     // AI搜索创建
    FORK = 2,          // Fork他人岗位
}

/**
 * 用户简要信息
 */
export interface OwnerVO {
    id: number;
    nickname: string;
    avatar?: string | null;
}

/**
 * 岗位简要信息（列表用）
 */
export interface JobSimple {
    id: number;
    jobName: string;
    source: JobSource;
    sourceUrls: string[];
    createdAt: string;
    owner?: OwnerVO;
}

/**
 * 社区岗位VO（社区列表用）
 */
export interface CommunityJobVO {
    id: number;
    jobName: string;
    source: JobSource;
    createdAt: string;
    owner?: OwnerVO;
}

/**
 * 岗位详细信息
 */
export interface JobVO {
    id: number;
    jobName: string;
    jdContent: string;
    source: JobSource;
    originalJobId: number | null;
    sourceUrls: string[];
    createdAt: string;
    updatedAt: string;
    owner: OwnerVO;
    commentCount?: number;
}

/**
 * 评论信息
 */
export interface Comment {
    id: number;
    jobId: number;
    content: string;
    createdAt: string;
    user: OwnerVO;
}

/**
 * 分页结果
 */
export interface PageResult<T> {
    content: T[];
    total: number;
    page: number;
    size: number;
}

/**
 * AI搜索结果
 */
export interface JobSearchResult {
    jobName: string;
    jdContent: string;
    sources: string[];
    webSearchSuccess?: boolean;
}

/**
 * 创建岗位请求
 */
export interface JobCreateRequest {
    jobName: string;
    jdContent?: string;
}

/**
 * 更新岗位请求
 */
export interface JobUpdateRequest {
    jobName?: string;
    jdContent?: string;
}

/**
 * AI搜索请求
 */
export interface JobSearchRequest {
    jobName: string;
    existingJd?: string;
}

/**
 * 创建评论请求
 */
export interface CommentCreateRequest {
    content: string;
}

/**
 * 岗位列表查询参数
 */
export interface JobListParams {
    page?: number;
    size?: number;
}

/**
 * 社区搜索参数
 */
export interface CommunitySearchParams {
    keyword: string;
    page?: number;
    size?: number;
}