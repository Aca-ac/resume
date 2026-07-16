import request from "@/utils/request";
import type { PageResult } from "@/types/api";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}
export interface Suggestion {
  type: string;
  title: string;
  description: string;
  priority: string;
  category?: string;
}

export interface SubDimension {
  name: string;
  score: number;
  description?: string;
}

export interface DimensionScore {
  id: number;
  name: string;
  score: number;
  weight: number;
  description?: string;
  details?: string;
  subDimensions?: SubDimension[];
}


export interface MatchAnalysisResult {
  id: number;
  resumeId: number;
  jobDescriptionId?: number;
  matchScore: number;
  matchLevel: string;
  dimensions: DimensionScore[];
  analysis: string;
  summary: string;
  highlights: string[];
  weaknesses: string[];
  suggestions: Suggestion[];
  createdAt: string;
  updatedAt: string;
  version: string;
}
export interface MatchRecord {
  id: number;
  resumeId: number;
  jobId?: number;
  matchScore: number;
  summaryScore?: number;
  educationScore?: number;
  experienceScore?: number;
  skillScore?: number;
  projectScore?: number;
  analysisId?: number;
  analysis: string;
  createdAt?: string;
  analyzedAt?: string;
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

export function matchByJob(data: { resumeId: number; jobId: number }) {
  return request
      .post<ApiResult<MatchRecord>>("/v1/match/job", data)
      .then((res) => unwrap(res));
}

export function fetchMatchHistory(page = 1, size = 50) {
  return request
      .get<ApiResult<PageResult<MatchRecord>>>("/v1/match/history", { params: { page, size } })
      .then((res) => unwrap(res).records ?? []);
}
export function fetchAnalysisDetail(analysisId: number) {
  return request
      .get<ApiResult<MatchAnalysisResult>>(`/v1/match/analysis/${analysisId}`)
      .then((res) => unwrap(res));
}