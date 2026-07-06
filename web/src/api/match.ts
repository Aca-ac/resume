import request from "@/utils/request";
import type { PageResult } from "@/types/api";

export interface MatchRecord {
  id: number;
  resumeId: number;
  matchScore: number;
  analysis: string;
  createdAt?: string;
}

export function matchJd(data: { resumeId: number; jdText: string }) {
  return request.post<MatchRecord>("/match/jd", data);
}

export function fetchMatchHistory(page = 1, size = 50) {
  return request.get<PageResult<MatchRecord>>("/match/history", { params: { page, size } }).then((r) => r.records);
}