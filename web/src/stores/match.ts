import { defineStore } from "pinia";
import { fetchMatchHistory, matchJd } from "@/api/match";

export const useMatchStore = defineStore("match", {
  state: () => ({
    history: [] as Array<{ id: number; resumeId: number; matchScore: number; analysis: string; createdAt?: string }>,
    latest: null as null | { matchScore: number; analysis: string }
  }),
  actions: {
    async runMatch(resumeId: number, jdText: string) {
      this.latest = await matchJd({ resumeId, jdText });
      await this.loadHistory();
    },
    async loadHistory() {
      this.history = await fetchMatchHistory();
    }
  }
});
