import request from "@/utils/request";
import { downloadBlob } from "@/utils/download";
import type { ResumeItem } from "@/stores/resume";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

const CHUNK_SIZE = 5 * 1024 * 1024;

function unwrap<T>(res: ApiResult<T>): T {
  if (res.code !== 200) {
    throw new Error(res.message || "请求失败");
  }
  return res.data;
}


function toResumeItem(r: {
  id: number;
  title: string;
  sourceType?: string;
  content?: string;
  updatedAt?: string;
}): ResumeItem {
  return {
    id: r.id,
    title: r.title,
    sourceType: r.sourceType ?? "MANUAL",
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

async function uploadChunks(file: File, uploadId: string) {
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  for (let i = 0; i < totalChunks; i++) {
    const chunk = file.slice(i * CHUNK_SIZE, Math.min(file.size, (i + 1) * CHUNK_SIZE));
    const form = new FormData();
    form.append("uploadId", uploadId);
    form.append("chunkIndex", String(i));
    form.append("file", chunk, file.name);
    await request.post<ApiResult<unknown>>("/v1/resumes/upload/chunk", form, multipartHeaders());
  }
  return request
    .post<ApiResult<{ resumeId: number; title: string; content: string; fileId: number; fileType: string; parseStatus?: string }>>(
      `/v1/resumes/upload/merge?uploadId=${encodeURIComponent(uploadId)}&filename=${encodeURIComponent(file.name)}&totalChunks=${totalChunks}`
    )
    .then(unwrap);
}

function multipartHeaders() {
  return { headers: { "Content-Type": "multipart/form-data" } };
}

export async function importResume(file: File) {
  if (file.size <= CHUNK_SIZE) {
    const form = new FormData();
    form.append("file", file);
    return request
      .post<ApiResult<{ resumeId: number; title: string; content: string; fileId: number; fileType: string; parseStatus?: string }>>(
        "/v1/resumes/import",
        form,
        multipartHeaders()
      )
      .then(unwrap);
  }
  const uploadId = crypto.randomUUID();
  return uploadChunks(file, uploadId);
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
