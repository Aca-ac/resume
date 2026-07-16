import request from "@/utils/request";

export interface SpeechRecognizeResult {
  text: string;
  fileId?: string;
  originalFilename?: string;
}

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

/** 语音识别：上传音频文件，返回文本 */
export async function recognizeSpeech(file: File | Blob, filename = "recording.wav") {
  const form = new FormData();
  form.append("file", file, filename);
  const res = await request.post<ApiResult<SpeechRecognizeResult>>("/v1/voice/asr", form, {
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 120000
  });
  return unwrap(res);
}

/** 语音合成：返回 MP3 blob */
export async function synthesizeSpeech(text: string): Promise<Blob> {
  return request.post<Blob>("/v1/voice/tts", { text }, {
    responseType: "blob",
    timeout: 120000
  });
}
