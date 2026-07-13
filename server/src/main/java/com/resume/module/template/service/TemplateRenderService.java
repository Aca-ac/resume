package com.resume.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deepoove.poi.XWPFTemplate;
import com.resume.common.BusinessException;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.module.resume.service.ResumeExportService;
import com.resume.module.template.entity.Template;
import com.resume.module.template.support.TemplatePlaceholders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateRenderService {

    private final TemplateService templateService;
    private final TemplateFileStorageService fileStorage;
    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ResumeExportService resumeExportService;

    public Map<String, Object> buildDataMap(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        Map<String, Object> data = TemplatePlaceholders.emptyDataMap();
        data.put(TemplatePlaceholders.NAME, resume.getTitle() == null ? "" : resume.getTitle());

        List<ResumeDetail> details = resumeDetailMapper.selectList(
                new LambdaQueryWrapper<ResumeDetail>()
                        .eq(ResumeDetail::getResumeId, resumeId)
                        .orderByAsc(ResumeDetail::getSortOrder));

        Map<String, StringBuilder> sections = new LinkedHashMap<>();
        for (String key : List.of(
                TemplatePlaceholders.EDUCATION,
                TemplatePlaceholders.EXPERIENCE,
                TemplatePlaceholders.PROJECT,
                TemplatePlaceholders.SKILLS,
                TemplatePlaceholders.SUMMARY)) {
            sections.put(key, new StringBuilder());
        }

        for (ResumeDetail d : details) {
            String key = TemplatePlaceholders.mapSectionType(d.getSectionType());
            StringBuilder sb = sections.getOrDefault(key, sections.get(TemplatePlaceholders.SUMMARY));
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            if (d.getSectionName() != null && !d.getSectionName().isBlank()) {
                sb.append(d.getSectionName()).append("\n");
            }
            if (d.getContent() != null) {
                sb.append(d.getContent());
            }
            tryFillProfileFields(data, d);
        }

        for (Map.Entry<String, StringBuilder> e : sections.entrySet()) {
            data.put(e.getKey(), e.getValue().toString().trim());
        }
        // 若 summary 为空，拼一个总览
        if (isBlank(data.get(TemplatePlaceholders.SUMMARY))) {
            StringBuilder all = new StringBuilder();
            for (String key : List.of(
                    TemplatePlaceholders.EDUCATION,
                    TemplatePlaceholders.EXPERIENCE,
                    TemplatePlaceholders.PROJECT,
                    TemplatePlaceholders.SKILLS)) {
                Object v = data.get(key);
                if (!isBlank(v)) {
                    if (!all.isEmpty()) {
                        all.append("\n\n");
                    }
                    all.append(v);
                }
            }
            data.put(TemplatePlaceholders.SUMMARY, all.toString());
        }
        return data;
    }

    public byte[] renderDocx(Long templateId, Map<String, Object> data) throws IOException {
        Template template = templateService.require(templateId);
        Path path = fileStorage.resolveTemplatePath(template.getTemplatePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "模板文件不存在");
        }
        try (XWPFTemplate tpl = XWPFTemplate.compile(path.toFile()).render(data);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            tpl.write(out);
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("poi-tl render failed, templateId={}", templateId, e);
            throw new BusinessException(500, "模板渲染失败: " + e.getMessage());
        }
    }

    public byte[] renderPdf(String title, Map<String, Object> data) throws IOException {
        return resumeExportService.exportPdf(title, toPlainText(data));
    }

    public String toPlainText(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "姓名", data.get(TemplatePlaceholders.NAME));
        appendLine(sb, "电话", data.get(TemplatePlaceholders.PHONE));
        appendLine(sb, "邮箱", data.get(TemplatePlaceholders.EMAIL));
        appendLine(sb, "学校", data.get(TemplatePlaceholders.SCHOOL));
        appendLine(sb, "专业", data.get(TemplatePlaceholders.MAJOR));
        appendLine(sb, "城市", data.get(TemplatePlaceholders.CITY));
        appendBlock(sb, "教育经历", data.get(TemplatePlaceholders.EDUCATION));
        appendBlock(sb, "工作经历", data.get(TemplatePlaceholders.EXPERIENCE));
        appendBlock(sb, "项目经历", data.get(TemplatePlaceholders.PROJECT));
        appendBlock(sb, "技能", data.get(TemplatePlaceholders.SKILLS));
        appendBlock(sb, "自我评价", data.get(TemplatePlaceholders.SUMMARY));
        return sb.toString().trim();
    }

    private void tryFillProfileFields(Map<String, Object> data, ResumeDetail d) {
        String content = d.getContent() == null ? "" : d.getContent();
        String type = d.getSectionType() == null ? "" : d.getSectionType().toUpperCase();
        if (type.contains("BASIC") || type.contains("PROFILE") || "SUMMARY".equals(type)) {
            fillIfMatch(data, TemplatePlaceholders.PHONE, content, "电话", "手机", "Tel", "Phone");
            fillIfMatch(data, TemplatePlaceholders.EMAIL, content, "邮箱", "Email", "邮件");
            fillIfMatch(data, TemplatePlaceholders.SCHOOL, content, "学校", "院校");
            fillIfMatch(data, TemplatePlaceholders.MAJOR, content, "专业");
            fillIfMatch(data, TemplatePlaceholders.CITY, content, "城市", "现居");
        }
    }

    private void fillIfMatch(Map<String, Object> data, String key, String content, String... labels) {
        if (!isBlank(data.get(key))) {
            return;
        }
        for (String label : labels) {
            int idx = content.indexOf(label);
            if (idx < 0) {
                continue;
            }
            int start = idx + label.length();
            while (start < content.length() && "：: \t".indexOf(content.charAt(start)) >= 0) {
                start++;
            }
            int end = start;
            while (end < content.length() && content.charAt(end) != '\n' && content.charAt(end) != ' ') {
                end++;
            }
            String value = content.substring(start, Math.min(end, content.length())).trim();
            if (!value.isEmpty()) {
                data.put(key, value);
                return;
            }
        }
    }

    private static void appendLine(StringBuilder sb, String label, Object value) {
        if (isBlank(value)) {
            return;
        }
        sb.append(label).append("：").append(value).append('\n');
    }

    private static void appendBlock(StringBuilder sb, String label, Object value) {
        if (isBlank(value)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append('\n');
        }
        sb.append("【").append(label).append("】\n").append(value).append('\n');
    }

    private static boolean isBlank(Object v) {
        return v == null || v.toString().isBlank();
    }
}
