package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.TemplateRenderData;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeTemplate;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.TemplateMapper;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateExportService {

    private static final String SECTION_SUMMARY = "SUMMARY";
    private static final String SECTION_EDUCATION = "EDUCATION";
    private static final String SECTION_WORK = "WORK_EXPERIENCE";
    private static final String SECTION_PROJECT = "PROJECT";
    private static final String SECTION_SKILL = "SKILL";

    /** he 的 V11 种子数据对齐；DB 未就绪时作为兜底 */
    private static final Map<Long, String> FALLBACK_TEMPLATE_PATHS = Map.of(
            1L, "/templates/resumes/simple/template.docx",
            2L, "/templates/resumes/professional/template.docx",
            3L, "/templates/resumes/creative/template.docx",
            4L, "/templates/resumes/academic/template.docx"
    );

    private final ResumeService resumeService;
    private final TemplateMapper templateMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final UserMapper userMapper;
    private final TemplateRenderService templateRenderService;
    private final LibreOfficePdfConverter pdfConverter;

    public byte[] exportWord(Long userId, Long resumeId, Long templateId) throws IOException {
        Resume resume = resumeService.getResume(resumeId, userId);
        String templatePath = resolveTemplatePath(templateId);
        TemplateRenderData data = buildRenderData(userId, resume);
        return templateRenderService.renderWord(templatePath, data);
    }

    public byte[] exportPdf(Long userId, Long resumeId, Long templateId) throws IOException {
        Resume resume = resumeService.getResume(resumeId, userId);
        byte[] docx = exportWord(userId, resumeId, templateId);
        return pdfConverter.convertDocxToPdf(docx, resume.getTitle());
    }

    String resolveTemplatePath(Long templateId) {
        if (templateId == null) {
            throw new BusinessException(400, "请指定 templateId");
        }
        try {
            ResumeTemplate template = templateMapper.selectById(templateId);
            if (template != null && template.getTemplatePath() != null && !template.getTemplatePath().isBlank()) {
                return template.getTemplatePath();
            }
        } catch (DataAccessException e) {
            log.debug("resume_templates not available yet, using fallback paths: {}", e.getMessage());
        }
        String fallback = FALLBACK_TEMPLATE_PATHS.get(templateId);
        if (fallback == null) {
            throw new BusinessException(404, "模板不存在，templateId=" + templateId);
        }
        return fallback;
    }

    private TemplateRenderData buildRenderData(Long userId, Resume resume) {
        User user = userMapper.selectById(userId);
        List<ResumeDetail> details = resumeDetailMapper.selectList(
                new LambdaQueryWrapper<ResumeDetail>()
                        .eq(ResumeDetail::getResumeId, resume.getId())
                        .orderByAsc(ResumeDetail::getSortOrder)
        );

        return TemplateRenderData.builder()
                .title(resume.getTitle())
                .name(pickName(user))
                .phone(user == null ? "" : nullToEmpty(user.getPhone()))
                .email(user == null ? "" : nullToEmpty(user.getEmail()))
                .summary(sectionContent(details, SECTION_SUMMARY))
                .education(sectionContent(details, SECTION_EDUCATION))
                .workExperience(sectionContent(details, SECTION_WORK))
                .project(sectionContent(details, SECTION_PROJECT))
                .skill(sectionContent(details, SECTION_SKILL))
                .build();
    }

    private String pickName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return nullToEmpty(user.getNickname());
    }

    private String sectionContent(List<ResumeDetail> details, String sectionType) {
        StringBuilder builder = new StringBuilder();
        for (ResumeDetail detail : details) {
            if (!sectionType.equalsIgnoreCase(detail.getSectionType())) {
                continue;
            }
            if (detail.getContent() == null || detail.getContent().isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            if (detail.getSectionName() != null && !detail.getSectionName().isBlank()) {
                builder.append("【").append(detail.getSectionName()).append("】\n");
            }
            builder.append(detail.getContent().trim());
        }
        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
