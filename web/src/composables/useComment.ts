// src/composables/useComment.ts

import { ref, computed } from 'vue';
import { useJobStore } from '@/stores/job';
import { ElMessage } from 'element-plus';
import type { CommentCreateRequest } from '@/types/job';

/**
 * 评论管理组合式函数
 */
export function useComment(jobId: number) {
    const store = useJobStore();

    // 本地状态
    const commentContent = ref('');
    const submitting = ref(false);

    // 计算属性
    const comments = computed(() => store.comments);
    const total = computed(() => store.commentsTotal);
    const isLoading = computed(() => store.commentLoading);

    /**
     * 加载评论列表
     */
    async function loadComments(page?: number, size?: number) {
        await store.fetchComments(jobId, page, size);
    }

    /**
     * 提交评论
     */
    async function submitComment() {
        const content = commentContent.value.trim();
        if (!content) {
            ElMessage.warning('请输入评论内容');
            return false;
        }

        if (content.length > 5000) {
            ElMessage.warning('评论内容不能超过5000字符');
            return false;
        }

        submitting.value = true;
        try {
            const result = await store.createComment(jobId, { content });
            if (result) {
                ElMessage.success('评论成功');
                commentContent.value = '';
                return true;
            }
            return false;
        } catch (err) {
            ElMessage.error('评论失败，请重试');
            return false;
        } finally {
            submitting.value = false;
        }
    }

    /**
     * 重置评论输入
     */
    function resetComment() {
        commentContent.value = '';
    }

    return {
        // 状态
        commentContent,
        submitting,
        isLoading,
        comments,
        total,

        // 方法
        loadComments,
        submitComment,
        resetComment,
    };
}