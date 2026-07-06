import request from "@/utils/request";
import type { PageResult } from "@/types/api";

export interface InterviewSession {
  id: number;
  jobTitle: string;
  status: string;
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

export function startInterview(data: { resumeId: number; jobTitle: string }) {
  return request.post<InterviewSession>("/interview/start", data);
}

export function fetchMessages(sessionId: number, page = 1, size = 200) {
  return request
    .get<PageResult<InterviewMessage>>(`/interview/${sessionId}/messages`, { params: { page, size } })
    .then((r) => r.records);
}

export function submitAnswer(sessionId: number, data: { answer: string }) {
  return request.post<InterviewMessage>(`/interview/${sessionId}/answer`, data);
}

export function fetchReport(sessionId: number) {
  return request.get<InterviewReport>(`/interview/${sessionId}/report`);
}