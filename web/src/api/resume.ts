import request from "@/utils/request";
import { assertDownloadBlob, downloadBlob } from "@/utils/download";
import type { ResumeItem } from "@/stores/resume";
import type { AxiosError } from "axios";

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

async function readBlobError(error: unknown): Promise<never> {
  const ax = error as AxiosError<Blob>;
  const data = ax.response?.data;
  if (data instanceof Blob) {
    try {
      const text = await data.text();
      const json = JSON.parse(text) as { message?: string };
      throw new Error(json.message || `导出失败(${ax.response?.status ?? "?"})`);
    } catch (e) {
      if (e instanceof Error && !e.message.startsWith("Unexpected") && !(e instanceof SyntaxError)) {
        throw e;
      }
    }
  }
  if (ax.code === "ECONNABORTED") {
    throw new Error("导出超时：后端响应过慢或未启动");
  }
  if (ax.message?.includes("Network Error")) {
    throw new Error("无法连接后端，请确认服务已在 8080 端口启动");
  }
  throw error instanceof Error ? error : new Error("导出失败");
}

async function downloadExport(path: string, filename: string) {
  try {
    const raw = (await request.get(path, {
      responseType: "blob",
      timeout: 60000
    })) as Blob;
    const blob = await assertDownloadBlob(raw);
    downloadBlob(blob, filename);
  } catch (e) {
    await readBlobError(e);
  }
}


function toResumeItem(r: {
  id: number;
  title: string;
  sourceType?: string;
  content?: string;
  photoUrl?: string;
  updatedAt?: string;
}): ResumeItem {
  return {
    id: r.id,
    title: r.title,
    sourceType: r.sourceType ?? "MANUAL",
    content: r.content ?? "",
    photoUrl: r.photoUrl,
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

export function uploadResumePhoto(resumeId: number, file: File) {
  const form = new FormData();
  form.append("file", file);
  return request
    .post<ApiResult<ResumeItem>>(`/v1/resumes/${resumeId}/photo`, form, multipartHeaders())
    .then((res) => toResumeItem(unwrap(res)));
}

export function deleteResumePhoto(resumeId: number) {
  return request.delete<ApiResult<ResumeItem>>(`/v1/resumes/${resumeId}/photo`).then((res) => toResumeItem(unwrap(res)));
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
  await downloadExport(`/v1/resumes/${id}/export/pdf`, `resume-${id}.pdf`);
}

export async function exportResumeDocx(id: number) {
  await downloadExport(`/v1/resumes/${id}/export/docx`, `resume-${id}.docx`);
}

export async function exportResumeText(id: number) {
  await downloadExport(`/v1/resumes/${id}/export/text`, `resume-${id}.txt`);
}

export interface ExportResultVO {
  exportId: string;
  resumeId: number;
  templateId: number;
  format: string;
  filename: string;
  downloadUrl: string;
  expiresAt?: string;
}

/** Strip accidental /api prefix so axios baseURL=/api does not become /api/api/... */
function toAxiosApiPath(url: string): string {
  const trimmed = (url || "").trim();
  if (!trimmed) return trimmed;
  if (trimmed.startsWith("/api/")) return trimmed.slice(4);
  if (trimmed.startsWith("api/")) return `/${trimmed.slice(4)}`;
  return trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
}

/** Template export: POST job then download blob via downloadUrl / exportId. */
export async function exportResumeByTemplate(
  resumeId: number,
  templateId: number,
  format: "word" | "pdf" = "word"
) {
  const job = await request
    .post<ApiResult<ExportResultVO>>(
      `/v1/resumes/${resumeId}/export?templateId=${templateId}&format=${format}`,
      null,
      { timeout: 120000 }
    )
    .then(unwrap);

  const path = job.downloadUrl
    ? toAxiosApiPath(job.downloadUrl)
    : `/v1/resumes/exports/${job.exportId}/download`;
  await downloadExport(path, job.filename || `resume-${resumeId}.${format === "pdf" ? "pdf" : "docx"}`);
  return job;
}

/** Direct template Word/PDF (one-shot, no job id). */
export async function exportResumeWordByTemplate(resumeId: number, templateId: number) {
  await downloadExport(
    `/v1/resumes/${resumeId}/export/word?templateId=${templateId}`,
    `resume-${resumeId}.docx`
  );
}

export async function exportResumePdfByTemplate(resumeId: number, templateId: number) {
  await downloadExport(
    `/v1/resumes/${resumeId}/export/pdf?templateId=${templateId}`,
    `resume-${resumeId}.pdf`
  );
}
