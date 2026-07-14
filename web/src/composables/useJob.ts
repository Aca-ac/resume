// src/composables/useJob.ts

import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useJobStore } from '@/stores/job';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { JobCreateRequest, JobUpdateRequest, JobSearchRequest } from '@/types/job';

/**
 * 岗位管理组合式函数
 */
export function useJob() {
    const store = useJobStore();
    const router = useRouter();

    // 本地状态
    const creating = ref(false);
    const editing = ref(false);

    // 计算属性 - 直接从 store 暴露
    const isLoading = computed(() => store.loading);
    const isSearching = computed(() => store.searchLoading);
    const isCommenting = computed(() => store.commentLoading);
    const error = computed(() => store.error);

    // 数据 - 直接从 store 暴露 refs
    const myJobs = computed(() => store.myJobs);
    const myJobsTotal = computed(() => store.myJobsTotal);
    const currentJob = computed(() => store.currentJob);
    const communityJobs = computed(() => store.communityJobs);
    const communityTotal = computed(() => store.communityTotal);
    const comments = computed(() => store.comments);
    const commentsTotal = computed(() => store.commentsTotal);
    const searchResult = computed(() => store.searchResult);
    const isJobOwner = computed(() => store.isJobOwner);

    /**
     * 创建岗位
     */
    async function handleCreateJob(data: JobCreateRequest) {
        creating.value = true;
        try {
            const result = await store.createJob(data);
            if (result) {
                ElMessage.success('岗位创建成功');
                router.push('/jobs');
            }
            return result;
        } catch (err) {
            ElMessage.error('创建失败，请重试');
            return null;
        } finally {
            creating.value = false;
        }
    }

    /**
     * 更新岗位
     */
    async function handleUpdateJob(id: number, data: JobUpdateRequest) {
        editing.value = true;
        try {
            const result = await store.updateJob(id, data);
            if (result) {
                ElMessage.success('岗位更新成功');
                router.push(`/jobs/${id}`);
            }
            return result;
        } catch (err) {
            ElMessage.error('更新失败，请重试');
            return null;
        } finally {
            editing.value = false;
        }
    }

    /**
     * 删除岗位（带确认）
     */
    async function handleDeleteJob(id: number, jobName: string) {
        try {
            await ElMessageBox.confirm(
                `确定要删除岗位「${jobName}」吗？删除后相关评论也会被删除。`,
                '删除确认',
                {
                    confirmButtonText: '确定删除',
                    cancelButtonText: '取消',
                    type: 'warning',
                }
            );
            const success = await store.deleteJob(id);
            if (success) {
                ElMessage.success('删除成功');
                router.push('/jobs');
            }
            return success;
        } catch (err) {
            if (err !== 'cancel') {
                ElMessage.error('删除失败，请重试');
            }
            return false;
        }
    }

    /**
     * AI搜索岗位
     */
    async function handleSearchJob(data: JobSearchRequest) {
        const result = await store.searchJob(data);
        if (result) {
            ElMessage.success(`已搜索到「${result.jobName}」的JD信息`);
        } else {
            ElMessage.error('搜索失败，请稍后重试');
        }
        return result;
    }

    /**
     * Fork岗位（带确认）
     */
    async function handleForkJob(id: number, jobName: string) {
        try {
            await ElMessageBox.confirm(
                `确定要Fork岗位「${jobName}」吗？将创建独立副本。`,
                'Fork确认',
                {
                    confirmButtonText: '确定Fork',
                    cancelButtonText: '取消',
                    type: 'info',
                }
            );
            const result = await store.forkJob(id);
            if (result) {
                ElMessage.success('Fork成功，已添加到我的岗位');
                router.push(`/jobs/${result.id}`);
            }
            return result;
        } catch (err) {
            if (err !== 'cancel') {
                ElMessage.error('Fork失败，请重试');
            }
            return null;
        }
    }

    /**
     * 刷新我的岗位列表
     */
    async function refreshMyJobs() {
        await store.fetchMyJobs();
    }

    /**
     * 刷新社区岗位列表
     */
    async function refreshCommunityJobs() {
        await store.fetchCommunityJobs();
    }

    /**
     * 获取岗位详情
     */
    async function fetchJobDetail(id: number) {
        return await store.fetchJobDetail(id);
    }

    /**
     * 获取社区岗位详情
     */
    async function fetchCommunityJobDetail(id: number) {
        return await store.fetchCommunityJobDetail(id);
    }

    /**
     * 获取评论列表
     */
    async function fetchComments(jobId: number, page?: number, size?: number) {
        return await store.fetchComments(jobId, page, size);
    }

    /**
     * 创建评论
     */
    async function createComment(jobId: number, data: CommentCreateRequest) {
        return await store.createComment(jobId, data);
    }

    /**
     * 搜索社区岗位
     */
    async function searchCommunityJobs(keyword: string, page?: number, size?: number) {
        return await store.searchCommunityJobs(keyword, page, size);
    }

    /**
     * 清空搜索结果
     */
    function clearSearchResult() {
        store.clearSearchResult();
    }

    /**
     * 重置错误
     */
    function resetError() {
        store.resetError();
    }

    return {
        // 状态
        creating,
        editing,
        isLoading,
        isSearching,
        isCommenting,
        error,

        // 数据（直接从store暴露的computed）
        myJobs,
        myJobsTotal,
        currentJob,
        communityJobs,
        communityTotal,
        comments,
        commentsTotal,
        searchResult,
        isJobOwner,

        // 方法
        handleCreateJob,
        handleUpdateJob,
        handleDeleteJob,
        handleSearchJob,
        handleForkJob,
        refreshMyJobs,
        refreshCommunityJobs,
        fetchJobDetail,
        fetchCommunityJobDetail,
        fetchComments,
        createComment,
        searchCommunityJobs,
        clearSearchResult,
        resetError,
    };
}

// 导入类型
import type { CommentCreateRequest } from '@/types/job';