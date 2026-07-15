import type { ResumeSectionType } from "@/types/template";

export interface ResumeDetailItem {
  id: number;
  resumeId: number;
  sectionType: ResumeSectionType | string;
  sectionName: string;
  content: string;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}
