import request from "@/utils/request";
import type { PageResult } from "@/types/api";
import type { ResumeItem } from "@/stores/resume";

export function fetchResumes(page = 1, size = 100) {
  return request.get<PageResult<ResumeItem>>("/resumes", { params: { page, size } }).then((r) => r.records);
}

export function fetchResume(id: number) {
  return request.get<ResumeItem>(`/resumes/${id}`);
}

export function createResume(data: { title: string; content: string }) {
  return request.post<ResumeItem>("/resumes", data);
}

export function updateResume(id: number, data: { title: string; content: string }) {
  return request.put<ResumeItem>(`/resumes/${id}`, data);
}

export function deleteResume(id: number) {
  return request.delete<void>(`/resumes/${id}`);
}

export function optimizeResume(id: number, targetRole: string) {
  return request.post<ResumeItem>(`/resumes/${id}/optimize`, { targetRole });
}

export async function exportResumePdf(id: number) {
  const blob = (await request.get(`/resumes/${id}/export/pdf`, {
    responseType: "blob"
  })) as Blob;
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `resume-${id}.pdf`;
  a.click();
  URL.revokeObjectURL(url);
}

export async function exportResumeText(id: number) {
  const blob = (await request.get(`/resumes/${id}/export/text`, {
    responseType: "blob"
  })) as Blob;
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `resume-${id}.txt`;
  a.click();
  URL.revokeObjectURL(url);
}