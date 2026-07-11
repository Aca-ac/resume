import request from "@/utils/request";
import type { PageResult } from "@/types/api";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface InterviewSession {
  id: number;
  jobTitle: string;
  status: string;
  report?: string;
}

export interface InterviewMessage {
  role: string;
  content: string;
}

export interface InterviewReport {
  id: number;
  report: string;
  status: string;
}

function unwrap<T>(res: ApiResult<T>): T {
  if (res.code !== 200) {
    throw new Error(res.message || "请求失败");
  }
  return res.data;
}

export function startInterview(data: { resumeId: number; jobTitle: string }) {
  return request
    .post<ApiResult<InterviewSession>>("/v1/interview/start", data)
    .then((res) => unwrap(res));
}

export function fetchMessages(sessionId: number, page = 1, size = 200) {
  return request
    .get<ApiResult<PageResult<InterviewMessage>>>(`/v1/interview/${sessionId}/messages`, {
      params: { page, size }
    })
    .then((res) => unwrap(res).records ?? []);
}

export function submitAnswer(sessionId: number, data: { answer: string }) {
  return request
    .post<ApiResult<InterviewMessage>>(`/v1/interview/${sessionId}/answer`, data)
    .then((res) => unwrap(res));
}

export function fetchReport(sessionId: number) {
  return request
    .get<ApiResult<InterviewReport>>(`/v1/interview/${sessionId}/report`)
    .then((res) => unwrap(res));
}
