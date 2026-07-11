import request from "@/utils/request";
import type { PageResult } from "@/types/api";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface MatchRecord {
  id: number;
  resumeId: number;
  matchScore: number;
  analysis: string;
  createdAt?: string;
}

function unwrap<T>(res: ApiResult<T>): T {
  if (res.code !== 200) {
    throw new Error(res.message || "请求失败");
  }
  return res.data;
}

export function matchJd(data: { resumeId: number; jdText: string }) {
  return request
    .post<ApiResult<MatchRecord>>("/v1/match/jd", data)
    .then((res) => unwrap(res));
}

export function fetchMatchHistory(page = 1, size = 50) {
  return request
    .get<ApiResult<PageResult<MatchRecord>>>("/v1/match/history", { params: { page, size } })
    .then((res) => unwrap(res).records ?? []);
}
