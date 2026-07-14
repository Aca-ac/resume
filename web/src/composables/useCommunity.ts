// src/composables/useCommunity.ts

import { ref, computed } from 'vue';
import { useJobStore } from '@/stores/job';
import { ElMessage } from 'element-plus';
import type { CommunityJobVO } from '@/types/job';

/**
 * 防抖函数
 */
function debounce<T extends (...args: any[]) => any>(
    fn: T,
    delay: number
): (...args: Parameters<T>) => void {
    let timer: ReturnType<typeof setTimeout> | null = null;
    return function (this: any, ...args: Parameters<T>) {
        if (timer) clearTimeout(timer);
        timer = setTimeout(() => {
            fn.apply(this, args);
            timer = null;
        }, delay);
    };
}

/**
 * 社区岗位组合式函数
 */
export function useCommunity() {
    const store = useJobStore();

    // 本地状态
    const searchKeyword = ref('');
    const searching = ref(false);

    // 计算属性
    const jobs = computed(() => store.communityJobs as CommunityJobVO[]);
    const total = computed(() => store.communityTotal);
    const isLoading = computed(() => store.loading);
    const currentPage = computed(() => store.communityPage);
    const pageSize = computed(() => store.communitySize);

    /**
     * 加载社区岗位列表
     */
    async function loadJobs(page?: number, size?: number) {
        await store.fetchCommunityJobs(page, size);
    }

    /**
     * 搜索社区岗位
     */
    async function searchJobs(keyword: string, page?: number, size?: number) {
        if (!keyword.trim()) {
            await loadJobs(page, size);
            return;
        }

        searching.value = true;
        try {
            await store.searchCommunityJobs(keyword.trim(), page, size);
        } catch (err) {
            ElMessage.error('搜索失败，请重试');
        } finally {
            searching.value = false;
        }
    }

    /**
     * 防抖搜索
     */
    const debouncedSearch = debounce((keyword: string) => {
        searchJobs(keyword, 1);
    }, 300);

    /**
     * 处理搜索输入
     */
    function handleSearchInput(keyword: string) {
        searchKeyword.value = keyword;
        debouncedSearch(keyword);
    }

    /**
     * 清空搜索
     */
    function clearSearch() {
        searchKeyword.value = '';
        loadJobs(1);
    }

    /**
     * 页码变化
     */
    function onPageChange(page: number) {
        if (searchKeyword.value.trim()) {
            searchJobs(searchKeyword.value, page);
        } else {
            loadJobs(page);
        }
    }

    return {
        // 状态
        searchKeyword,
        searching,
        isLoading,
        jobs,
        total,
        currentPage,
        pageSize,

        // 方法
        loadJobs,
        searchJobs,
        handleSearchInput,
        clearSearch,
        onPageChange,
    };
}