import request from "@/utils/request";
import { downloadBlob } from "@/utils/download";
import type { PageResult } from "@/types/api";
import type { ResumeItem } from "@/stores/resume";

export function fetchResumes(page = 1, size = 100) {
  return request.get<PageResult<ResumeItem>>("/v1/resumes", { params: { page, size } }).then((r) => r.records);
}

export function fetchResume(id: number) {
  return request.get<ResumeItem>(`/v1/resumes/${id}`);
}

export function createResume(data: { title: string; content: string }) {
  return request.post<ResumeItem>("/v1/resumes", data);
}

export function updateResume(id: number, data: { title: string; content: string }) {
  return request.put<ResumeItem>(`/v1/resumes/${id}`, data);
}

export function deleteResume(id: number) {
  return request.delete<void>(`/v1/resumes/${id}`);
}

export function importResume(file: File) {
  const form = new FormData();
  form.append("file", file);
  return request.post<{ resumeId: number; title: string; content: string; fileId: number; fileType: string }>(
    "/v1/resumes/import",
    form
  );
}

export function runResumeOcr(fileId: number) {
  return request.post<{ fileId: number; ocrText: string }>(`/v1/resumes/files/${fileId}/ocr`);
}

export function optimizeResume(id: number, targetRole: string) {
  return request.post<ResumeItem>(`/v1/resumes/${id}/optimize`, { targetRole });
}

export async function exportResumePdf(id: number) {
  const blob = (await request.get(`/v1/resumes/${id}/export/pdf`, {
    responseType: "blob"
  })) as Blob;
  downloadBlob(blob, `resume-${id}.pdf`);
}

export async function exportResumeDocx(id: number) {
  const blob = (await request.get(`/v1/resumes/${id}/export/docx`, {
    responseType: "blob"
  })) as Blob;
  downloadBlob(blob, `resume-${id}.docx`);
}

export async function exportResumeText(id: number) {
  const blob = (await request.get(`/v1/resumes/${id}/export/text`, {
    responseType: "blob"
  })) as Blob;
  downloadBlob(blob, `resume-${id}.txt`);
}
