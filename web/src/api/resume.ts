import request from "@/utils/request";
import { downloadBlob } from "@/utils/download";
import type { ResumeItem } from "@/stores/resume";

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

function toResumeItem(r: {
  id: number;
  title: string;
  content?: string;
  updatedAt?: string;
}): ResumeItem {
  return {
    id: r.id,
    title: r.title,
    content: r.content ?? "",
    updatedAt: r.updatedAt
  };
}

export function fetchResumes() {
  return request.get<ApiResult<ResumeItem[]>>("/v1/resumes").then((res) => unwrap(res).map(toResumeItem));
}

export function fetchResume(id: number) {
  return request.get<ApiResult<ResumeItem>>(`/v1/resumes/${id}`).then((res) => toResumeItem(unwrap(res)));
}

export function createResume(data: { title: string; content: string }) {
  return request.post<ApiResult<ResumeItem>>("/v1/resumes", data).then((res) => toResumeItem(unwrap(res)));
}

export function updateResume(id: number, data: { title: string; content: string }) {
  return request.put<ApiResult<ResumeItem>>(`/v1/resumes/${id}`, data).then((res) => toResumeItem(unwrap(res)));
}

export function deleteResume(id: number) {
  return request.delete<ApiResult<void>>(`/v1/resumes/${id}`).then(unwrap);
}

export function importResume(file: File) {
  const form = new FormData();
  form.append("file", file);
  return request
    .post<ApiResult<{ resumeId: number; title: string; content: string; fileId: number; fileType: string }>>(
      "/v1/resumes/import",
      form
    )
    .then(unwrap);
}

export function runResumeOcr(fileId: number) {
  return request
    .post<ApiResult<{ id: number; ocrText?: string }>>(`/v1/resumes/files/${fileId}/ocr`)
    .then((res) => {
      const file = unwrap(res);
      return { fileId: file.id, ocrText: file.ocrText ?? "" };
    });
}

export function optimizeResume(id: number, targetRole: string) {
  return request.post<ApiResult<ResumeItem>>(`/v1/resumes/${id}/optimize`, { targetRole }).then((res) => toResumeItem(unwrap(res)));
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
