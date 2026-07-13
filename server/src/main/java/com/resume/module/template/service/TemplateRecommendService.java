package com.resume.module.template.service;

import com.resume.module.template.dto.TemplateRecommendRequest;
import com.resume.module.template.dto.TemplateRecommendVO;
import com.resume.module.template.dto.TemplateVO;
import com.resume.module.template.entity.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 规则引擎推荐；后续可替换为机器学习模型，保持接口不变。
 */
@Service
@RequiredArgsConstructor
public class TemplateRecommendService {

    private final TemplateService templateService;

    public List<TemplateRecommendVO> recommend(TemplateRecommendRequest req) {
        String education = norm(req.getEducation());
        String industry = norm(req.getIndustry());
        String workYears = norm(req.getWorkYears());
        String target = norm(req.getTargetPosition());

        List<TemplateRecommendVO> scored = new ArrayList<>();
        for (Template t : templateService.listAll()) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            String cat = norm(t.getCategory());
            String scene = norm(t.getApplicableScene()) + " " + norm(t.getName());

            if (isFreshGraduate(education, workYears)) {
                if (containsAny(cat + scene, "应届", "校园", "校招", "简洁", "fresh", "campus")) {
                    score += 40;
                    reasons.add("匹配应届生/校园场景");
                }
            } else if (parseYears(workYears) >= 5) {
                if (containsAny(cat + scene, "商务", "管理", "资深", "高管", "business")) {
                    score += 30;
                    reasons.add("匹配资深/管理场景");
                }
            }

            if (containsAny(target + industry, "java", "后端", "开发", "前端", "算法", "技术", "engineer", "software")) {
                if (containsAny(cat + scene, "技术", "开发", "工程师", "it", "tech")) {
                    score += 35;
                    reasons.add("匹配技术岗位模板");
                }
            }
            if (containsAny(target + industry, "设计", "ui", "ux", "产品", "design")) {
                if (containsAny(cat + scene, "设计", "创意", "design")) {
                    score += 35;
                    reasons.add("匹配设计岗位模板");
                }
            }
            if (containsAny(target + industry, "管理", "经理", "总监", "运营", "人事", "hr")) {
                if (containsAny(cat + scene, "商务", "管理", "正式")) {
                    score += 30;
                    reasons.add("匹配商务/管理模板");
                }
            }
            if ("通用".equals(t.getCategory()) || containsAny(cat, "通用", "general")) {
                score += 5;
                reasons.add("通用兜底");
            }

            if (score <= 0) {
                score = 1;
                reasons.add("可作为备选模板");
            }

            TemplateRecommendVO vo = new TemplateRecommendVO();
            vo.setTemplate(templateService.toVO(t));
            vo.setScore(score);
            vo.setReason(String.join("；", reasons));
            scored.add(vo);
        }

        scored.sort(Comparator.comparingInt(TemplateRecommendVO::getScore).reversed());
        return scored;
    }

    private static boolean isFreshGraduate(String education, String workYears) {
        int years = parseYears(workYears);
        return years <= 1 || containsAny(education, "应届", "在读", "本科", "硕士", "大专")
                && years <= 2;
    }

    private static int parseYears(String workYears) {
        if (workYears == null || workYears.isBlank()) {
            return 0;
        }
        String digits = workYears.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            if (workYears.contains("应届") || workYears.contains("无")) {
                return 0;
            }
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean containsAny(String text, String... keys) {
        String t = norm(text);
        for (String k : keys) {
            if (t.contains(norm(k))) {
                return true;
            }
        }
        return false;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
