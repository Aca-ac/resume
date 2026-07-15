export interface TemplateVO {
  id: number;
  name: string;
  category: string;
  previewUrl: string;
  templatePath: string;
  applicableScene?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TemplateRecommendVO {
  template: TemplateVO;
  score: number;
  reason: string;
}

export interface TemplatePageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

export interface ExportResultVO {
  exportId: string;
  resumeId: number;
  templateId: number;
  format: "word" | "pdf";
  filename: string;
  downloadUrl: string;
  expiresAt?: string;
}

export type ExportFormat = "word" | "pdf";

export type ResumeSectionType =
  | "SUMMARY"
  | "EDUCATION"
  | "WORK_EXPERIENCE"
  | "PROJECT"
  | "SKILL";

export interface ResumeSectionDef {
  type: ResumeSectionType;
  name: string;
  placeholder: string;
  sortOrder: number;
}

export const RESUME_TEMPLATE_SECTIONS: ResumeSectionDef[] = [
  {
    type: "SUMMARY",
    name: "个人总结",
    placeholder: "简要介绍你的优势与核心亮点…",
    sortOrder: 1
  },
  {
    type: "EDUCATION",
    name: "教育经历",
    placeholder: "学校、专业、学历、起止时间、主修课程等…",
    sortOrder: 2
  },
  {
    type: "WORK_EXPERIENCE",
    name: "工作经历",
    placeholder: "公司、岗位、时间、职责与成果（建议量化）…",
    sortOrder: 3
  },
  {
    type: "PROJECT",
    name: "项目经历",
    placeholder: "项目名称、角色、技术栈、项目描述与成果…",
    sortOrder: 4
  },
  {
    type: "SKILL",
    name: "技能证书",
    placeholder: "编程语言、框架、工具、证书等…",
    sortOrder: 5
  }
];
