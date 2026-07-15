// src/stores/matchRecommend.ts
import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { matchRecommendApi } from "@/api/matchRecommend";
import type {
    MatchRecommendResult,
    MatchRecommendRecord,
    MatchRecommendDetail,
    JobRecommendation,
    ImprovementSuggestion,
} from "@/types/api";

export const useMatchRecommendStore = defineStore("matchRecommend", () => {
    // ===== State =====
    const currentResult = ref<MatchRecommendResult | null>(null);
    const recommendations = ref<JobRecommendation[]>([]);
    const improvementSuggestion = ref<ImprovementSuggestion | null>(null);
    const history = ref<MatchRecommendRecord[]>([]);
    const total = ref<number>(0);
    const currentPage = ref<number>(1);
    const pageSize = ref<number>(20);
    const currentDetail = ref<MatchRecommendDetail | null>(null);
    const loading = ref<boolean>(false);
    const historyLoading = ref<boolean>(false);
    const detailLoading = ref<boolean>(false);

    // ===== Getters =====
    /** 是否有匹配结果 */
    const hasRecommendations = computed<boolean>(() => {
        return recommendations.value.length > 0;
    });

    /** 最高匹配度 */
    const topMatchScore = computed<number>(() => {
        if (recommendations.value.length === 0) return 0;
        return Math.max(...recommendations.value.map((r) => r.matchScore));
    });

    /** 匹配等级 */
    const matchLevel = computed<{ label: string; type: string }>(() => {
        const score = topMatchScore.value;
        if (score >= 80) return { label: "高度匹配", type: "success" };
        if (score >= 60) return { label: "部分匹配", type: "warning" };
        if (score > 0) return { label: "待提升", type: "danger" };
        return { label: "暂无匹配", type: "info" };
    });

    // ===== Actions =====
    /**
     * 执行岗位匹配推荐
     */
    const runRecommend = async (resumeId: number, enableWebSearch: boolean = true): Promise<MatchRecommendResult> => {
        loading.value = true;
        try {
            const result = await matchRecommendApi.recommend({
                resumeId,
                enableWebSearch,
            });
            currentResult.value = result;
            recommendations.value = result.recommendations || [];
            improvementSuggestion.value = result.improvementSuggestion || null;
            return result;
        } finally {
            loading.value = false;
        }
    };

    /**
     * 加载历史记录
     */
    const loadHistory = async (page: number = 1, size: number = 20): Promise<void> => {
        historyLoading.value = true;
        try {
            const response = await matchRecommendApi.getHistory({ page, size });
            history.value = response.content || [];
            total.value = response.total || 0;
            currentPage.value = response.page || page;
            pageSize.value = response.size || size;
        } catch (error) {
            console.error("加载历史记录失败:", error);
            throw error;
        } finally {
            historyLoading.value = false;
        }
    };

    /**
     * 加载推荐记录详情
     */
    const loadDetail = async (id: number): Promise<MatchRecommendDetail> => {
        detailLoading.value = true;
        try {
            const detail = await matchRecommendApi.getDetail(id);
            currentDetail.value = detail;
            return detail;
        } finally {
            detailLoading.value = false;
        }
    };

    /**
     * 清空当前结果
     */
    const clearCurrent = (): void => {
        currentResult.value = null;
        recommendations.value = [];
        improvementSuggestion.value = null;
        currentDetail.value = null;
    };

    /**
     * 清空详情
     */
    const clearDetail = (): void => {
        currentDetail.value = null;
    };

    return {
        // State
        currentResult,
        recommendations,
        improvementSuggestion,
        history,
        total,
        currentPage,
        pageSize,
        currentDetail,
        loading,
        historyLoading,
        detailLoading,
        // Getters
        hasRecommendations,
        topMatchScore,
        matchLevel,
        // Actions
        runRecommend,
        loadHistory,
        loadDetail,
        clearCurrent,
        clearDetail,
    };
});