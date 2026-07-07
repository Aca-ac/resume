import { defineStore } from "pinia";
import { fetchMessages, fetchReport, startInterview, submitAnswer } from "@/api/interview";

export const useInterviewStore = defineStore("interview", {
  state: () => ({
    sessionId: null as number | null,
    jobTitle: "",
    messages: [] as Array<{ role: string; content: string }>,
    report: ""
  }),
  actions: {
    async start(resumeId: number, jobTitle: string) {
      const session = await startInterview({ resumeId, jobTitle });
      this.sessionId = session.id;
      this.jobTitle = jobTitle;
      this.messages = await fetchMessages(session.id);
      return session;
    },
    async answer(text: string) {
      if (!this.sessionId) return;
      const reply = await submitAnswer(this.sessionId, { answer: text });
      this.messages = await fetchMessages(this.sessionId);
      return reply;
    },
    async loadReport(sessionId: number) {
      const session = await fetchReport(sessionId);
      this.report = session.report || "";
      return session;
    }
  }
});
