// src/api/matchRecommend.ts
import request from "@/utils/request";
import type {
    MatchRecommendRequest,
    MatchRecommendResult,
    MatchRecommendRecord,
    MatchRecommendDetail,
    MatchRecommendHistoryResponse,
} from "@/types/api";

interface ApiResult<T> {
    code: number;
    message: string;
    data: T;
}

function unwrap<T>(res: ApiResult<T>): T {
    if (res.code !== 200) {
        throw new Error(res.message || "请求失败");
    }
    return res.data;
}

/**
 * 岗位匹配推荐API
 */
export const matchRecommendApi = {
    /**
     * 基于简历进行岗位匹配推荐
     * @param data.resumeId - 简历ID
     * @param data.enableWebSearch - 是否开启联网搜索，默认 true
     */
    recommend(data: MatchRecommendRequest): Promise<MatchRecommendResult> {
        return request
            .post<ApiResult<MatchRecommendResult>>("/v1/match/recommend", data)
            .then((res) => unwrap(res));
    },

    /**
     * 查询推荐历史记录列表
     * @param params.page - 页码，默认 1
     * @param params.size - 每页数量，默认 20
     */
    getHistory(params: { page?: number; size?: number }): Promise<MatchRecommendHistoryResponse> {
        return request
            .get<ApiResult<MatchRecommendHistoryResponse>>("/v1/match/recommendations", { params })
            .then((res) => unwrap(res));
    },

    /**
     * 查询单条推荐记录详情
     * @param id - 推荐记录ID
     */
    getDetail(id: number): Promise<MatchRecommendDetail> {
        return request
            .get<ApiResult<MatchRecommendDetail>>(`/v1/match/recommendations/${id}`)
            .then((res) => unwrap(res));
    },
};