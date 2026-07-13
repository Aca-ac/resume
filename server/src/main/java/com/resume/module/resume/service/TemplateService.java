package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.dto.TemplateRecommendVO;
import com.resume.module.resume.dto.TemplateSaveRequest;
import com.resume.module.resume.dto.TemplateVO;
import com.resume.module.resume.entity.ResumeTemplate;
import com.resume.module.resume.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * he：模板数据管理与智能推荐（tian 负责渲染/导出，只读 template_path）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TemplateMapper templateMapper;

    public PageResult<TemplateVO> list(String category, int page, int size) {
        LambdaQueryWrapper<ResumeTemplate> q = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            q.eq(ResumeTemplate::getCategory, category.trim());
        }
        q.orderByAsc(ResumeTemplate::getId);
        Page<ResumeTemplate> result = templateMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), q);
        List<TemplateVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public TemplateVO get(Long id) {
        return toVO(require(id));
    }

    @Transactional
    public TemplateVO create(TemplateSaveRequest req) {
        ResumeTemplate entity = new ResumeTemplate();
        apply(entity, req);
        templateMapper.insert(entity);
        log.info("Created resume template id={}, name={}", entity.getId(), entity.getName());
        return toVO(entity);
    }

    @Transactional
    public TemplateVO update(Long id, TemplateSaveRequest req) {
        ResumeTemplate entity = require(id);
        apply(entity, req);
        templateMapper.updateById(entity);
        log.info("Updated resume template id={}", id);
        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        templateMapper.deleteById(id);
        log.info("Deleted resume template id={}", id);
    }

    /**
     * 按工作年限、行业、目标岗位对模板打分排序。
     */
    public List<TemplateRecommendVO> recommend(String education, String industry, String workYears, String targetPosition) {
        String edu = norm(education);
        String ind = norm(industry);
        String years = norm(workYears);
        String target = norm(targetPosition);

        List<ResumeTemplate> all = templateMapper.selectList(
                new LambdaQueryWrapper<ResumeTemplate>().orderByAsc(ResumeTemplate::getId));
        List<TemplateRecommendVO> scored = new ArrayList<>();
        for (ResumeTemplate t : all) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            String cat = norm(t.getCategory());
            String scene = norm(t.getApplicableScene()) + " " + norm(t.getName());

            int yearNum = parseYears(years);
            if (yearNum <= 1 || containsAny(edu, "应届", "在读", "校招")) {
                if (containsAny(cat + scene, "简约", "应届", "初级", "校园")) {
                    score += 40;
                    reasons.add("匹配应届生/初级场景");
                }
            } else if (yearNum >= 5) {
                if (containsAny(cat + scene, "专业", "高级", "金融", "咨询", "中级")) {
                    score += 35;
                    reasons.add("匹配资深/专业场景");
                }
            } else if (yearNum >= 2) {
                if (containsAny(cat + scene, "专业", "中级")) {
                    score += 25;
                    reasons.add("匹配中级岗位");
                }
            }

            if (containsAny(target + ind, "设计", "创意", "ui", "ux", "产品视觉")) {
                if (containsAny(cat + scene, "创意", "设计")) {
                    score += 40;
                    reasons.add("匹配设计/创意岗位");
                }
            }
            if (containsAny(target + ind, "研究", "学术", "博士", "教师", "教育", "科研")) {
                if (containsAny(cat + scene, "学术", "教育", "研究")) {
                    score += 40;
                    reasons.add("匹配学术/教育岗位");
                }
            }
            if (containsAny(target + ind, "金融", "咨询", "审计", "投行")) {
                if (containsAny(cat + scene, "专业", "金融", "咨询")) {
                    score += 30;
                    reasons.add("匹配金融/咨询场景");
                }
            }
            if (containsAny(cat, "简约") && yearNum <= 2) {
                score += 5;
            }
            if (score <= 0) {
                score = 1;
                reasons.add("可作为备选模板");
            }

            TemplateRecommendVO vo = new TemplateRecommendVO();
            vo.setTemplate(toVO(t));
            vo.setScore(score);
            vo.setReason(String.join("；", reasons));
            scored.add(vo);
        }
        scored.sort(Comparator.comparingInt(TemplateRecommendVO::getScore).reversed());
        return scored;
    }

    public ResumeTemplate require(Long id) {
        ResumeTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(404, "模板不存在");
        }
        return t;
    }

    public TemplateVO toVO(ResumeTemplate t) {
        TemplateVO vo = new TemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setCategory(t.getCategory());
        vo.setPreviewUrl(t.getPreviewUrl());
        vo.setTemplatePath(t.getTemplatePath());
        vo.setApplicableScene(t.getApplicableScene());
        vo.setCreatedAt(t.getCreatedAt() == null ? null : t.getCreatedAt().format(FMT));
        vo.setUpdatedAt(t.getUpdatedAt() == null ? null : t.getUpdatedAt().format(FMT));
        return vo;
    }

    private void apply(ResumeTemplate entity, TemplateSaveRequest req) {
        entity.setName(req.getName().trim());
        entity.setCategory(req.getCategory().trim());
        entity.setPreviewUrl(blankToNull(req.getPreviewUrl()));
        entity.setTemplatePath(req.getTemplatePath().trim());
        entity.setApplicableScene(blankToNull(req.getApplicableScene()));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static int parseYears(String workYears) {
        if (workYears == null || workYears.isBlank()) {
            return 0;
        }
        if (workYears.contains("应届") || workYears.contains("无")) {
            return 0;
        }
        String digits = workYears.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
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
