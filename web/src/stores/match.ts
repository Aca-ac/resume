import { defineStore } from "pinia";
import {fetchAnalysisDetail, fetchMatchHistory, MatchAnalysisResult, matchJd, MatchRecord} from "@/api/match";

export const useMatchStore = defineStore("match", {
  state: () => ({
    history: [] as MatchRecord[],
    latest: null as null | (MatchRecord & { analysisDetail?: MatchAnalysisResult }),
    currentAnalysis: null as null | MatchAnalysisResult,
    loading: false,
  }),
  actions: {
    async runMatch(resumeId: number, jdText: string) {
      this.loading = true;
      try {
        const result = await matchJd({ resumeId, jdText });
        this.latest = result;
        // 如果有 analysisId，拉取详细数据（包含雷达图维度）
        if (result.analysisId) {
          const detail = await fetchAnalysisDetail(result.analysisId);
          this.latest.analysisDetail = detail;
          this.currentAnalysis = detail;
        }
        await this.loadHistory();
        return result;
      } finally {
        this.loading = false;
      }
    },
    async loadHistory() {
      this.history = await fetchMatchHistory();
    },
    async loadAnalysisDetail(analysisId: number) {
      const detail = await fetchAnalysisDetail(analysisId);
      this.currentAnalysis = detail;
      return detail;
    },
    clearCurrentAnalysis() {
      this.currentAnalysis = null;
    },
  },
});