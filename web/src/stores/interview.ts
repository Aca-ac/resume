import { defineStore } from "pinia";
import {
  endInterview,
  fetchInterviewHistory,
  fetchInterviewSession,
  fetchMessages,
  fetchReport,
  startInterview,
  submitAnswer,
  type InterviewSession
} from "@/api/interview";

export const useInterviewStore = defineStore("interview", {
  state: () => ({
    sessionId: null as number | null,
    jobTitle: "",
    state: "" as string,
    questionIndex: 0,
    maxQuestions: 5,
    lastSeq: 0,
    messages: [] as Array<{
      role: string;
      content: string;
      messageType?: string;
      seq?: number;
      questionIndex?: number;
    }>,
    report: "",
    finished: false,
    history: [] as InterviewSession[],
    historyTotal: 0
  }),
  getters: {
    progressPercent(state): number {
      const max = state.maxQuestions || 5;
      const cur = Math.min(state.questionIndex || 0, max);
      return Math.round((cur / max) * 100);
    },
    stateLabel(state): string {
      const map: Record<string, string> = {
        PREPARING: "准备中",
        QUESTIONING: "等待回答",
        EVALUATING: "评估中",
        SUMMARIZING: "生成总结",
        COMPLETED: "已结束",
        ABORTED: "已中止",
        ONGOING: "进行中",
        DONE: "已结束"
      };
      return map[state.state] || state.state || "进行中";
    }
  },
  actions: {
    applySessionMeta(session: InterviewSession) {
      this.sessionId = session.id;
      this.jobTitle = session.jobTitle || this.jobTitle;
      this.state = session.state || session.status || "";
      this.questionIndex = session.questionIndex || 0;
      this.maxQuestions = session.maxQuestions || 5;
      this.lastSeq = session.lastSeq || 0;
      if (session.report) this.report = session.report;
      this.finished = ["COMPLETED", "ABORTED", "DONE"].includes(this.state);
    },
    async start(resumeId: number, jobTitle: string, jobId?: number, maxQuestions?: number) {
      const session = await startInterview({ resumeId, jobTitle, jobId, maxQuestions });
      this.applySessionMeta(session);
      this.finished = false;
      this.report = "";
      this.messages = await fetchMessages(session.id);
      if ((!this.messages || this.messages.length === 0) && session.firstQuestion) {
        this.messages = [session.firstQuestion];
        if (session.firstQuestion.seq) this.lastSeq = session.firstQuestion.seq;
      }
      return session;
    },
    async loadSession(sessionId: number) {
      const session = await fetchInterviewSession(sessionId);
      this.applySessionMeta(session);
      this.messages = await fetchMessages(sessionId);
      const last = this.messages[this.messages.length - 1];
      if (last?.seq) this.lastSeq = last.seq;
      return session;
    },
    async answer(text: string, clientMsgId?: string) {
      if (!this.sessionId) return;
      const result = await submitAnswer(this.sessionId, { answer: text, clientMsgId });
      this.state = result.state;
      this.questionIndex = result.questionIndex;
      this.maxQuestions = result.maxQuestions;
      this.finished = result.finished;
      if (result.report) this.report = result.report;
      this.messages = await fetchMessages(this.sessionId);
      const last = this.messages[this.messages.length - 1];
      if (last?.seq) this.lastSeq = last.seq;
      return result;
    },
    async end() {
      if (!this.sessionId) return;
      const session = await endInterview(this.sessionId);
      this.applySessionMeta(session);
      this.finished = true;
      this.report = session.report || this.report;
      this.messages = await fetchMessages(this.sessionId);
      return session;
    },
    async loadReport(sessionId: number) {
      const session = await fetchReport(sessionId);
      this.report = session.report || "";
      this.state = session.state || session.status;
      this.finished = true;
      return session;
    },
    async loadHistory(page = 1, size = 20) {
      const data = await fetchInterviewHistory(page, size);
      this.history = data.records || [];
      this.historyTotal = data.total || 0;
      return data;
    }
  }
});
