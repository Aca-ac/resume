// src/stores/job.ts

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { jobApi } from '@/api/job';
import type {
    JobVO,
    JobSimple,
    CommunityJobVO,
    JobCreateRequest,
    JobUpdateRequest,
    JobSearchRequest,
    JobSearchResult,
    Comment,
    CommentCreateRequest,
    PageResult,
} from '@/types/job';
import { useAuthStore } from '@/stores/auth.ts';

export const useJobStore = defineStore('job', () => {
    // ==================== State ====================

    // 我的岗位列表
    const myJobs = ref<JobSimple[]>([]);
    const myJobsTotal = ref(0);
    const myJobsPage = ref(1);
    const myJobsSize = ref(20);

    // 当前查看的岗位详情
    const currentJob = ref<JobVO | null>(null);

    // 社区岗位列表
    const communityJobs = ref<CommunityJobVO[]>([]);
    const communityTotal = ref(0);
    const communityPage = ref(1);
    const communitySize = ref(20);

    // 社区搜索关键词
    const communityKeyword = ref('');

    // 评论列表
    const comments = ref<Comment[]>([]);
    const commentsTotal = ref(0);
    const commentsPage = ref(1);
    const commentsSize = ref(20);

    // AI搜索结果
    const searchResult = ref<JobSearchResult | null>(null);

    // 加载状态
    const loading = ref(false);
    const searchLoading = ref(false);
    const commentLoading = ref(false);

    // 错误信息
    const error = ref<string | null>(null);

    // ==================== Getters ====================

    const hasMyJobs = computed(() => myJobs.value.length > 0);
    const hasCommunityJobs = computed(() => communityJobs.value.length > 0);
    const hasComments = computed(() => comments.value.length > 0);
    const isJobOwner = computed(() => {
        if (!currentJob.value) return false;
        const authStore = useAuthStore();
        return currentJob.value.owner?.id === authStore.userInfo?.id;
    });

    // ==================== Actions ====================

    /**
     * 重置错误
     */
    function resetError() {
        error.value = null;
    }

    /**
     * 处理API错误
     */
    function handleError(err: any) {
        error.value = err?.message || '操作失败，请重试';
        console.error('[JobStore Error]', err);
    }

    // ----- 岗位CRUD -----

    /**
     * 创建岗位
     */
    async function createJob(data: JobCreateRequest): Promise<JobVO | null> {
        loading.value = true;
        resetError();
        try {
            const result = await jobApi.createJob(data);
            await fetchMyJobs();
            return result;
        } catch (err) {
            handleError(err);
            return null;
        } finally {
            loading.value = false;
        }
    }

    /**
     * 获取我的岗位列表
     */
    async function fetchMyJobs(page?: number, size?: number): Promise<void> {
        loading.value = true;
        resetError();
        try {
            const p = page ?? myJobsPage.value;
            const s = size ?? myJobsSize.value;
            const result = await jobApi.getJobList({ page: p, size: s });
            myJobs.value = result.content;
            myJobsTotal.value = result.total;
            myJobsPage.value = result.page;
            myJobsSize.value = result.size;
        } catch (err) {
            handleError(err);
        } finally {
            loading.value = false;
        }
    }

    /**
     * 获取岗位详情
     */
    async function fetchJobDetail(id: number): Promise<JobVO | null> {
        loading.value = true;
        resetError();
        try {
            const result = await jobApi.getJobDetail(id);
            currentJob.value = result;
            return result;
        } catch (err) {
            handleError(err);
            return null;
        } finally {
            loading.value = false;
        }
    }

    /**
     * 更新岗位
     */
    async function updateJob(id: number, data: JobUpdateRequest): Promise<JobVO | null> {
        loading.value = true;
        resetError();
        try {
            const result = await jobApi.updateJob(id, data);
            if (currentJob.value?.id === id) {
                currentJob.value = result;
            }
            await fetchMyJobs();
            return result;
        } catch (err) {
            handleError(err);
            return null;
        } finally {
            loading.value = false;
        }
    }

    /**
     * 删除岗位
     */
    async function deleteJob(id: number): Promise<boolean> {
        loading.value = true;
        resetError();
        try {
            await jobApi.deleteJob(id);
            myJobs.value = myJobs.value.filter(j => j.id !== id);
            if (currentJob.value?.id === id) {
                currentJob.value = null;
            }
            return true;
        } catch (err) {
            handleError(err);
            return false;
        } finally {
            loading.value = false;
        }
    }

    // ----- AI搜索 -----

    /**
     * AI搜索岗位
     */
    async function searchJob(data: JobSearchRequest): Promise<JobSearchResult | null> {
        searchLoading.value = true;
        resetError();
        try {
            const result = await jobApi.searchJob(data);
            searchResult.value = result;
            return result;
        } catch (err) {
            handleError(err);
            return null;
        } finally {
            searchLoading.value = false;
        }
    }

    /**
     * 清空搜索结果
     */
    function clearSearchResult() {
        searchResult.value = null;
    }

    // ----- 社区岗位 -----

    /**
     * 获取社区岗位列表
     */
    async function fetchCommunityJobs(page?: number, size?: number): Promise<void> {
        loading.value = true;
        resetError();
        try {
            const p = page ?? communityPage.value;
            const s = size ?? communitySize.value;
            const result = await jobApi.getCommunityJobs({ page: p, size: s });
            communityJobs.value = result.content;
            communityTotal.value = result.total;
            communityPage.value = result.page;
            communitySize.value = result.size;
        } catch (err) {
            handleError(err);
        } finally {
            loading.value = false;
        }
    }

    /**
     * 搜索社区岗位
     */
    async function searchCommunityJobs(keyword: string, page?: number, size?: number): Promise<void> {
        loading.value = true;
        resetError();
        try {
            communityKeyword.value = keyword;
            const p = page ?? communityPage.value;
            const s = size ?? communitySize.value;
            const result = await jobApi.searchCommunityJobs({ keyword, page: p, size: s });
            communityJobs.value = result.content;
            communityTotal.value = result.total;
            communityPage.value = result.page;
            communitySize.value = result.size;
        } catch (err) {
            handleError(err);
        } finally {
            loading.value = false;
        }
    }

    /**
     * 获取社区岗位详情 - 复用 getJobDetail，因为后端没有单独的社区详情接口
     */
    async function fetchCommunityJobDetail(id: number): Promise<JobVO | null> {
        return fetchJobDetail(id);
    }

    // ----- Fork -----

    /**
     * Fork岗位
     */
    async function forkJob(id: number): Promise<JobVO | null> {
        loading.value = true;
        resetError();
        try {
            const result = await jobApi.forkJob(id);
            await fetchMyJobs();
            return result;
        } catch (err) {
            handleError(err);
            return null;
        } finally {
            loading.value = false;
        }
    }

    // ----- 评论 -----

    /**
     * 获取评论列表
     */
    async function fetchComments(jobId: number, page?: number, size?: number): Promise<void> {
        commentLoading.value = true;
        resetError();
        try {
            const p = page ?? commentsPage.value;
            const s = size ?? commentsSize.value;
            const result = await jobApi.getComments(jobId, { page: p, size: s });
            comments.value = result.content;
            commentsTotal.value = result.total;
            commentsPage.value = result.page;
            commentsSize.value = result.size;
        } catch (err) {
            handleError(err);
        } finally {
            commentLoading.value = false;
        }
    }

    /**
     * 发表评论
     */
    async function createComment(jobId: number, data: CommentCreateRequest): Promise<Comment | null> {
        commentLoading.value = true;
        resetError();
        try {
            const result = await jobApi.createComment(jobId, data);
            await fetchComments(jobId);
            return result;
        } catch (err) {
            handleError(err);
            return null;
        } finally {
            commentLoading.value = false;
        }
    }

    // ----- 重置 -----

    /**
     * 重置所有状态
     */
    function reset() {
        myJobs.value = [];
        myJobsTotal.value = 0;
        currentJob.value = null;
        communityJobs.value = [];
        communityTotal.value = 0;
        comments.value = [];
        commentsTotal.value = 0;
        searchResult.value = null;
        loading.value = false;
        searchLoading.value = false;
        commentLoading.value = false;
        error.value = null;
    }

    return {
        // State
        myJobs,
        myJobsTotal,
        myJobsPage,
        myJobsSize,
        currentJob,
        communityJobs,
        communityTotal,
        communityPage,
        communitySize,
        communityKeyword,
        comments,
        commentsTotal,
        commentsPage,
        commentsSize,
        searchResult,
        loading,
        searchLoading,
        commentLoading,
        error,

        // Getters
        hasMyJobs,
        hasCommunityJobs,
        hasComments,
        isJobOwner,

        // Actions
        resetError,
        createJob,
        fetchMyJobs,
        fetchJobDetail,
        updateJob,
        deleteJob,
        searchJob,
        clearSearchResult,
        fetchCommunityJobs,
        searchCommunityJobs,
        fetchCommunityJobDetail,
        forkJob,
        fetchComments,
        createComment,
        reset,
    };
});