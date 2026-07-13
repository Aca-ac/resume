package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.ExportResultVO;
import com.resume.module.resume.dto.TemplateRenderData;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeTemplate;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateExportService {

    private static final String SECTION_SUMMARY = "SUMMARY";
    private static final String SECTION_EDUCATION = "EDUCATION";
    private static final String SECTION_WORK = "WORK_EXPERIENCE";
    private static final String SECTION_PROJECT = "PROJECT";
    private static final String SECTION_SKILL = "SKILL";

    private final ResumeService resumeService;
    private final TemplateService templateService;
    private final ResumeDetailMapper resumeDetailMapper;
    private final UserMapper userMapper;
    private final TemplateRenderService templateRenderService;
    private final LibreOfficePdfConverter pdfConverter;
    private final ExportStorageService exportStorageService;

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

    public ExportResultVO createExportJob(Long userId, Long resumeId, Long templateId, String format) throws IOException {
        if (format == null || format.isBlank()) {
            throw new BusinessException(400, "请指定导出格式 word 或 pdf");
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        Resume resume = resumeService.getResume(resumeId, userId);
        ResumeTemplate template = templateService.require(templateId);

        byte[] content;
        String downloadFilename;
        if ("pdf".equals(normalized)) {
            content = exportPdf(userId, resumeId, templateId);
            downloadFilename = buildFilename(resume.getTitle(), "pdf");
            normalized = "pdf";
        } else if ("word".equals(normalized) || "docx".equals(normalized)) {
            content = exportWord(userId, resumeId, templateId);
            downloadFilename = buildFilename(resume.getTitle(), "docx");
            normalized = "word";
        } else {
            throw new BusinessException(400, "不支持的导出格式: " + format);
        }

        ExportStorageService.StoredExport stored = exportStorageService.store(
                userId, resumeId, templateId, normalized, content, downloadFilename);

        ExportResultVO vo = new ExportResultVO();
        vo.setExportId(stored.exportId());
        vo.setResumeId(resumeId);
        vo.setTemplateId(templateId);
        vo.setFormat(normalized);
        vo.setFilename(downloadFilename);
        vo.setDownloadUrl("/api/v1/resumes/exports/" + stored.exportId() + "/download");
        vo.setExpiresAt(exportStorageService.formatExpiresAt(stored.expiresAt()));
        log.info("Export job created exportId={}, resumeId={}, template={}", stored.exportId(), resumeId, template.getName());
        return vo;
    }

    public ExportStorageService.StoredExport requireExportFile(Long userId, String exportId) {
        return exportStorageService.requireOwned(userId, exportId);
    }

    public byte[] readExportFile(ExportStorageService.StoredExport stored) throws IOException {
        return exportStorageService.read(stored);
    }

    String resolveTemplatePath(Long templateId) {
        ResumeTemplate template = templateService.require(templateId);
        if (template.getTemplatePath() == null || template.getTemplatePath().isBlank()) {
            throw new BusinessException(500, "模板未配置 template_path，templateId=" + templateId);
        }
        return template.getTemplatePath();
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

    private String buildFilename(String title, String ext) {
        String base = title == null || title.isBlank() ? "resume" : title.trim();
        return base.replaceAll("[\\\\/:*?\"<>|]", "_") + "." + ext;
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
