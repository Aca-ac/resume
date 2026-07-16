import { useAuthStore } from "@/stores/auth";
import { interviewWsUrl, type InterviewAnswerResult, type InterviewMessage } from "@/api/interview";

type Handler = {
  onState?: (payload: Record<string, unknown>) => void;
  onAnswerResult?: (payload: InterviewAnswerResult) => void;
  onSnapshot?: (payload: { messages: InterviewMessage[]; lastSeq: number; state: string; questionIndex: number }) => void;
  onError?: (message: string) => void;
  onOpen?: () => void;
  onClose?: () => void;
};

/**
 * Sprint3：面试 WebSocket 客户端（连接 / 心跳 / RESUME 恢复）
 */
export function createInterviewSocket(sessionId: number, handlers: Handler = {}) {
  const auth = useAuthStore();
  let ws: WebSocket | null = null;
  let closedByUser = false;
  let pingTimer: number | undefined;
  let lastSeq = 0;
  let retry = 0;

  function connect() {
    const token = auth.accessToken;
    if (!token) {
      handlers.onError?.("未登录，无法建立面试实时连接");
      return;
    }
    closedByUser = false;
    ws = new WebSocket(interviewWsUrl(sessionId, token));
    ws.onopen = () => {
      retry = 0;
      handlers.onOpen?.();
      // 恢复断线消息
      ws?.send(JSON.stringify({ type: "RESUME", payload: { afterSeq: lastSeq } }));
      window.clearInterval(pingTimer);
      pingTimer = window.setInterval(() => {
        if (ws?.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: "PING", payload: {} }));
        }
      }, 25000);
    };
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(String(ev.data)) as { type: string; payload: any };
        const type = (msg.type || "").toUpperCase();
        const payload = msg.payload || {};
        if (type === "SNAPSHOT") {
          const messages = (payload.messages || []) as InterviewMessage[];
          for (const m of messages) {
            if (m.seq && m.seq > lastSeq) lastSeq = m.seq;
          }
          handlers.onSnapshot?.({
            messages,
            lastSeq: payload.lastSeq ?? lastSeq,
            state: payload.state,
            questionIndex: payload.questionIndex
          });
        } else if (type === "STATE") {
          handlers.onState?.(payload);
        } else if (type === "ANSWER_RESULT") {
          handlers.onAnswerResult?.(payload as InterviewAnswerResult);
        } else if (type === "ERROR") {
          handlers.onError?.(payload.message || "WebSocket 错误");
        }
      } catch {
        // ignore malformed
      }
    };
    ws.onclose = () => {
      window.clearInterval(pingTimer);
      handlers.onClose?.();
      if (!closedByUser && retry < 5) {
        const delay = Math.min(1000 * 2 ** retry, 10000);
        retry += 1;
        window.setTimeout(connect, delay);
      }
    };
    ws.onerror = () => {
      handlers.onError?.("WebSocket 连接异常");
    };
  }

  function sendAnswer(answer: string, clientMsgId?: string) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      throw new Error("实时通道未连接，请使用文字发送或稍后重试");
    }
    ws.send(JSON.stringify({
      type: "ANSWER",
      payload: { answer, clientMsgId }
    }));
  }

  function sendEnd() {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ type: "END", payload: {} }));
  }

  function setLastSeq(seq: number) {
    if (seq > lastSeq) lastSeq = seq;
  }

  function close() {
    closedByUser = true;
    window.clearInterval(pingTimer);
    ws?.close();
    ws = null;
  }

  return { connect, sendAnswer, sendEnd, setLastSeq, close };
}
