import request from "@/utils/request";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface TemplateVO {
  id: number;
  name: string;
  category: string;
  previewUrl: string;
  templatePath: string;
  applicableScene?: string;
}

export interface TemplateRecommendVO {
  template: TemplateVO;
  score: number;
  reason: string;
}

function unwrap<T>(res: ApiResult<T>): T {
  if (res.code !== 200) {
    throw new Error(res.message || "请求失败");
  }
  return res.data;
}

export function fetchTemplates(category?: string, page = 1, size = 20) {
  return request
    .get<ApiResult<{ records: TemplateVO[]; total: number }>>("/v1/templates", {
      params: { category, page, size }
    })
    .then(unwrap);
}

export function fetchTemplate(id: number) {
  return request.get<ApiResult<TemplateVO>>(`/v1/templates/${id}`).then(unwrap);
}

export function recommendTemplates(params: {
  education?: string;
  industry?: string;
  workYears?: string;
  targetPosition?: string;
}) {
  return request
    .get<ApiResult<TemplateRecommendVO[]>>("/v1/templates/recommend", { params })
    .then(unwrap);
}



/** Browser URL for preview image (proxied to backend). */
export function previewSrc(previewUrl?: string) {
  if (!previewUrl) return "";
  if (previewUrl.startsWith("http")) return previewUrl;
  return previewUrl.startsWith("/") ? previewUrl : `/${previewUrl}`;
}
