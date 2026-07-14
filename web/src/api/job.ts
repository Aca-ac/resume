// src/api/job.ts

import request from '@/utils/request';
import type {
    JobVO,
    JobSimple,
    JobCreateRequest,
    JobUpdateRequest,
    JobSearchRequest,
    JobSearchResult,
    Comment,
    CommentCreateRequest,
    PageResult,
    JobListParams,
    CommunitySearchParams, CommunityJobVO,
} from '@/types/job';
interface ApiResult<T> {
    code: number;
    message: string;
    data: T;
}
// 注意：request.ts 中 baseURL 已经是 '/api'，所以这里不需要再加 /api
const BASE_URL = '/v1/jobs';

function unwrap<T>(res: ApiResult<T>): T {
    if (res.code !== 200) {
        throw new Error(res.message || "请求失败");
    }
    return res.data;
}
/**
 * 岗位管理API
 */
export const jobApi = {
    createJob(data: JobCreateRequest): Promise<JobVO> {
        return request.post<ApiResult<JobVO>>(BASE_URL, data).then(unwrap);
    },

    getJobList(params: JobListParams = {}): Promise<PageResult<JobSimple>> {
        return request.get<ApiResult<PageResult<JobSimple>>>(BASE_URL, { params }).then(unwrap);
    },

    getJobDetail(id: number): Promise<JobVO> {
        return request.get<ApiResult<JobVO>>(`${BASE_URL}/${id}`).then(unwrap);
    },

    updateJob(id: number, data: JobUpdateRequest): Promise<JobVO> {
        return request.put<ApiResult<JobVO>>(`${BASE_URL}/${id}`, data).then(unwrap);
    },

    deleteJob(id: number): Promise<void> {
        return request.delete<ApiResult<void>>(`${BASE_URL}/${id}`).then(unwrap);
    },

    searchJob(data: JobSearchRequest): Promise<JobSearchResult> {
        return request.post<ApiResult<JobSearchResult>>(`${BASE_URL}/search`, data).then(unwrap);
    },

    getCommunityJobs(params: JobListParams = {}): Promise<PageResult<CommunityJobVO>> {
        return request.get<ApiResult<PageResult<CommunityJobVO>>>(`${BASE_URL}/community`, { params }).then(unwrap);
    },

    searchCommunityJobs(params: CommunitySearchParams): Promise<PageResult<CommunityJobVO>> {
        return request.get<ApiResult<PageResult<CommunityJobVO>>>(`${BASE_URL}/community/search`, { params }).then(unwrap);
    },

    forkJob(id: number): Promise<JobVO> {
        return request.post<ApiResult<JobVO>>(`${BASE_URL}/${id}/fork`).then(unwrap);
    },

    getComments(jobId: number, params: JobListParams = {}): Promise<PageResult<Comment>> {
        return request.get<ApiResult<PageResult<Comment>>>(`${BASE_URL}/${jobId}/comments`, { params }).then(unwrap);
    },

    createComment(jobId: number, data: CommentCreateRequest): Promise<Comment> {
        return request.post<ApiResult<Comment>>(`${BASE_URL}/${jobId}/comments`, data).then(unwrap);
    },
};