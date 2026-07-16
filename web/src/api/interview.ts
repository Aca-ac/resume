import request from "@/utils/request";
import type { PageResult } from "@/types/api";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface InterviewMessage {
  id?: number;
  role: string;
  content: string;
  messageType?: string;
  questionIndex?: number;
  evaluation?: string;
  seq?: number;
  createdAt?: string;
}

export interface InterviewSession {
  id: number;
  resumeId?: number;
  jobId?: number;
  jobTitle: string;
  status: string;
  state?: string;
  questionIndex?: number;
  maxQuestions?: number;
  lastSeq?: number;
  report?: string;
  createdAt?: string;
  endedAt?: string;
  firstQuestion?: InterviewMessage;
}

export interface InterviewAnswerResult {
  state: string;
  questionIndex: number;
  maxQuestions: number;
  finished: boolean;
  report?: string;
  userMessage?: InterviewMessage;
  feedback?: InterviewMessage;
  nextQuestion?: InterviewMessage | null;
}

export interface InterviewReport {
  id: number;
  report: string;
  status: string;
  state?: string;
}

function unwrap<T>(res: ApiResult<T>): T {
  if (res.code !== 200) {
    throw new Error(res.message || "请求失败");
  }
  return res.data;
}

export function startInterview(data: {
  resumeId: number;
  jobTitle?: string;
  jobId?: number;
  maxQuestions?: number;
}) {
  return request
    .post<ApiResult<InterviewSession>>("/v1/interview/start", data)
    .then((res) => unwrap(res));
}

export function fetchInterviewHistory(page = 1, size = 20) {
  return request
    .get<ApiResult<PageResult<InterviewSession>>>("/v1/interview/history", { params: { page, size } })
    .then((res) => unwrap(res));
}

export function fetchInterviewSession(sessionId: number) {
  return request
    .get<ApiResult<InterviewSession>>(`/v1/interview/${sessionId}`)
    .then((res) => unwrap(res));
}

export function fetchMessages(sessionId: number, page = 1, size = 200, afterSeq?: number) {
  return request
    .get<ApiResult<PageResult<InterviewMessage>>>(`/v1/interview/${sessionId}/messages`, {
      params: { page, size, afterSeq }
    })
    .then((res) => unwrap(res).records ?? []);
}

export function submitAnswer(sessionId: number, data: { answer: string; clientMsgId?: string }) {
  return request
    .post<ApiResult<InterviewAnswerResult>>(`/v1/interview/${sessionId}/answer`, data)
    .then((res) => unwrap(res));
}

export function endInterview(sessionId: number) {
  return request
    .post<ApiResult<InterviewSession>>(`/v1/interview/${sessionId}/end`)
    .then((res) => unwrap(res));
}

export function generateNextQuestion(sessionId: number) {
  return request
    .post<ApiResult<InterviewMessage>>(`/v1/interview/${sessionId}/question`)
    .then((res) => unwrap(res));
}

export function fetchReport(sessionId: number) {
  return request
    .get<ApiResult<InterviewReport>>(`/v1/interview/${sessionId}/report`)
    .then((res) => unwrap(res));
}

/** WebSocket URL helper for yang */
export function interviewWsUrl(sessionId: number, token: string) {
  const proto = location.protocol === "https:" ? "wss" : "ws";
  return `${proto}://${location.host}/ws/interview?sessionId=${sessionId}&token=${encodeURIComponent(token)}`;
}
