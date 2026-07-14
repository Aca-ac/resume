import { defineStore } from "pinia";
import { createResume, deleteResume, fetchResume, fetchResumes, updateResume } from "@/api/resume";

export interface ResumeItem {
  id: number;
  title: string;
  sourceType?: string;
  content: string;
  /** 有照片时返回，如 /api/v1/resumes/1/photo */
  photoUrl?: string;
  updatedAt?: string;
}

export const useResumeStore = defineStore("resume", {
  state: () => ({
    list: [] as ResumeItem[],
    current: null as ResumeItem | null
  }),
  actions: {
    async loadList() {
      this.list = await fetchResumes();
    },
    async loadOne(id: number) {
      this.current = await fetchResume(id);
    },
    async save(payload: { id?: number; title: string; content: string }) {
      if (payload.id) {
        await updateResume(payload.id, { title: payload.title, content: payload.content });
      } else {
        await createResume({ title: payload.title, content: payload.content });
      }
      await this.loadList();
    },
    async remove(id: number) {
      await deleteResume(id);
      await this.loadList();
    }
  }


});
