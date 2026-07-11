import { defineStore } from "pinia";
import { fetchMatchHistory, matchJd } from "@/api/match";

export const useMatchStore = defineStore("match", {
  state: () => ({
    history: [] as Array<{ id: number; resumeId: number; matchScore: number; analysis: string; createdAt?: string }>,
    latest: null as null | { id?: number; resumeId?: number; matchScore: number; analysis: string; createdAt?: string }
  }),
  actions: {
    async runMatch(resumeId: number, jdText: string) {
      const result = await matchJd({ resumeId, jdText });
      this.latest = result;
      await this.loadHistory();
    },
    async loadHistory() {
      this.history = await fetchMatchHistory();
    }
  }
});
