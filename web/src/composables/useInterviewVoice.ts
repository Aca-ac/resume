/**
 * he：面试侧语音对接适配层。
 * 只调用 tian 已提供的 `api/voice.ts`（ASR/TTS），不改动语音服务实现。
 * ASR 前将浏览器录音转为 16kHz 单声道 WAV（对齐《语音接口联调文档》）。
 */
import { onBeforeUnmount, ref } from "vue";
import { recognizeSpeech, synthesizeSpeech } from "@/api/voice";

const TARGET_SAMPLE_RATE = 16000;

function pickMimeType(): string {
  const candidates = [
    "audio/webm;codecs=opus",
    "audio/webm",
    "audio/ogg;codecs=opus",
    "audio/mp4"
  ];
  for (const type of candidates) {
    if (typeof MediaRecorder !== "undefined" && MediaRecorder.isTypeSupported(type)) {
      return type;
    }
  }
  return "";
}

/** 线性插值重采样到目标采样率，输出单声道 Float32 */
function resampleMono(channelData: Float32Array, fromRate: number, toRate: number): Float32Array {
  if (fromRate === toRate) {
    return channelData;
  }
  const ratio = fromRate / toRate;
  const outLen = Math.max(1, Math.round(channelData.length / ratio));
  const out = new Float32Array(outLen);
  for (let i = 0; i < outLen; i++) {
    const srcIndex = i * ratio;
    const i0 = Math.floor(srcIndex);
    const i1 = Math.min(i0 + 1, channelData.length - 1);
    const t = srcIndex - i0;
    out[i] = channelData[i0] * (1 - t) + channelData[i1] * t;
  }
  return out;
}

function floatTo16BitPCM(samples: Float32Array): Int16Array {
  const pcm = new Int16Array(samples.length);
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    pcm[i] = s < 0 ? Math.round(s * 0x8000) : Math.round(s * 0x7fff);
  }
  return pcm;
}

function encodeWavPcm16(pcm: Int16Array, sampleRate: number): Blob {
  const dataSize = pcm.length * 2;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);
  const writeStr = (offset: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  };
  writeStr(0, "RIFF");
  view.setUint32(4, 36 + dataSize, true);
  writeStr(8, "WAVE");
  writeStr(12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, 1, true); // mono
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeStr(36, "data");
  view.setUint32(40, dataSize, true);
  let offset = 44;
  for (let i = 0; i < pcm.length; i++, offset += 2) {
    view.setInt16(offset, pcm[i], true);
  }
  return new Blob([buffer], { type: "audio/wav" });
}

/** 浏览器录音（多为 webm/opus）→ 16kHz 单声道 PCM WAV */
async function toWav16k(blob: Blob): Promise<Blob> {
  const arrayBuffer = await blob.arrayBuffer();
  const audioCtx = new AudioContext();
  try {
    const decoded = await audioCtx.decodeAudioData(arrayBuffer.slice(0));
    const channelCount = decoded.numberOfChannels;
    const length = decoded.length;
    const mono = new Float32Array(length);
    for (let ch = 0; ch < channelCount; ch++) {
      const data = decoded.getChannelData(ch);
      for (let i = 0; i < length; i++) {
        mono[i] += data[i] / channelCount;
      }
    }
    const resampled = resampleMono(mono, decoded.sampleRate, TARGET_SAMPLE_RATE);
    return encodeWavPcm16(floatTo16BitPCM(resampled), TARGET_SAMPLE_RATE);
  } finally {
    await audioCtx.close().catch(() => undefined);
  }
}

export function useInterviewVoice() {
  const recording = ref(false);
  const recognizing = ref(false);
  const speaking = ref(false);
  const autoSpeak = ref(true);

  let mediaRecorder: MediaRecorder | null = null;
  let mediaStream: MediaStream | null = null;
  let chunks: BlobPart[] = [];
  let currentAudio: HTMLAudioElement | null = null;
  let objectUrl: string | null = null;

  function stopPlayback() {
    if (currentAudio) {
      currentAudio.pause();
      currentAudio.src = "";
      currentAudio = null;
    }
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl);
      objectUrl = null;
    }
    speaking.value = false;
  }

  async function speak(text: string) {
    const trimmed = text?.trim();
    if (!trimmed || !autoSpeak.value) return;
    stopPlayback();
    speaking.value = true;
    try {
      const blob = await synthesizeSpeech(trimmed);
      // TTS 失败时拦截器可能仍返回 JSON blob，避免当成音频播放
      if (blob.type && blob.type.includes("json")) {
        throw new Error("语音合成失败");
      }
      objectUrl = URL.createObjectURL(blob);
      const audio = new Audio(objectUrl);
      currentAudio = audio;
      await new Promise<void>((resolve, reject) => {
        audio.onended = () => resolve();
        audio.onerror = () => reject(new Error("语音播放失败"));
        void audio.play().catch(reject);
      });
    } finally {
      if (currentAudio) {
        speaking.value = false;
      }
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
        objectUrl = null;
      }
      currentAudio = null;
    }
  }

  /** 按顺序播报多段文本（评价 → 下一题） */
  async function speakQueue(texts: Array<string | null | undefined>) {
    for (const t of texts) {
      if (!t?.trim()) continue;
      await speak(t);
    }
  }

  async function startRecording() {
    if (recording.value || recognizing.value) return;
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error("当前浏览器不支持录音");
    }
    stopPlayback();
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true
      }
    });
    const mimeType = pickMimeType();
    mediaRecorder = mimeType
      ? new MediaRecorder(mediaStream, { mimeType })
      : new MediaRecorder(mediaStream);
    chunks = [];
    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) chunks.push(e.data);
    };
    mediaRecorder.start(200);
    recording.value = true;
  }

  async function stopRecordingAndRecognize(): Promise<string> {
    if (!mediaRecorder || !recording.value) {
      throw new Error("当前没有进行中的录音");
    }
    const mimeType = mediaRecorder.mimeType || "audio/webm";
    const blob = await new Promise<Blob>((resolve, reject) => {
      mediaRecorder!.onstop = () => {
        resolve(new Blob(chunks, { type: mimeType }));
      };
      mediaRecorder!.onerror = () => reject(new Error("录音失败"));
      mediaRecorder!.stop();
    });
    recording.value = false;
    mediaStream?.getTracks().forEach((t) => t.stop());
    mediaStream = null;
    mediaRecorder = null;
    chunks = [];

    if (blob.size < 256) {
      throw new Error("录音太短，请重试");
    }

    recognizing.value = true;
    try {
      // DashScope 不支持浏览器 webm；按联调文档转 16kHz wav
      const wav = await toWav16k(blob);
      if (wav.size < 44 + 320) {
        throw new Error("录音太短，请重试");
      }
      const result = await recognizeSpeech(wav, "recording.wav");
      const text = result.text?.trim() || "";
      if (!text) {
        throw new Error("未识别到有效语音，请重试");
      }
      return text;
    } catch (e: any) {
      const msg = e?.message || String(e);
      if (msg.includes("decodeAudioData") || msg.includes("Unable to decode")) {
        throw new Error("音频解码失败，请换用 Chrome/Edge 重试");
      }
      throw e;
    } finally {
      recognizing.value = false;
    }
  }

  async function cancelRecording() {
    if (!recording.value) return;
    try {
      mediaRecorder?.stop();
    } catch {
      /* ignore */
    }
    recording.value = false;
    mediaStream?.getTracks().forEach((t) => t.stop());
    mediaStream = null;
    mediaRecorder = null;
    chunks = [];
  }

  onBeforeUnmount(() => {
    void cancelRecording();
    stopPlayback();
  });

  return {
    recording,
    recognizing,
    speaking,
    autoSpeak,
    startRecording,
    stopRecordingAndRecognize,
    cancelRecording,
    speak,
    speakQueue,
    stopPlayback
  };
}
