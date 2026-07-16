package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint3 AC3.1：四套简历模板渲染冒烟审计。
 */
class TemplateRenderAuditTest {

    private static final String[] STYLES = {"academic", "simple", "professional", "creative"};
    private static final String[] PLACEHOLDERS = {
            "{{name}}", "{{phone}}", "{{email}}", "{{jobIntention}}",
            "{{education}}", "{{workExperience}}", "{{project}}", "{{skill}}", "{{summary}}"
    };

    private final TemplateRenderService renderService = new TemplateRenderService();

    @Test
    void allTemplatesRenderWithoutPlaceholderLeak() throws Exception {
        TemplateRenderData data = sampleData();
        List<String> issues = new ArrayList<>();

        for (String style : STYLES) {
            String path = "/templates/resumes/" + style + "/template.docx";
            try {
                byte[] docx = renderService.renderWord(path, data);
                String xml = documentXml(docx);
                for (String ph : PLACEHOLDERS) {
                    if (xml.contains(ph)) {
                        issues.add(style + " 残留占位符 " + ph);
                    }
                }
                assertTrue(xml.contains("张三"), style + " 应包含姓名");
                assertTrue(xml.contains("Java开发工程师"), style + " 应包含求职意向");
            } catch (Exception e) {
                issues.add(style + " 渲染失败: " + e.getMessage());
            }
        }
        assertTrue(issues.isEmpty(), "模板问题: " + issues);
    }

    @Test
    void auditReportForDocumentation() throws Exception {
        TemplateRenderData data = sampleData();
        Map<String, String> report = new LinkedHashMap<>();
        for (String style : STYLES) {
            String path = "/templates/resumes/" + style + "/template.docx";
            try {
                byte[] docx = renderService.renderWord(path, data);
                String xml = documentXml(docx);
                long leftover = 0;
                for (String ph : PLACEHOLDERS) {
                    if (xml.contains(ph)) {
                        leftover++;
                    }
                }
                report.put(style, leftover == 0 ? "正常" : "异常-残留占位符" + leftover + "个");
            } catch (Exception e) {
                report.put(style, "异常-" + e.getMessage());
            }
        }
        assertFalse(report.containsValue("异常-渲染失败"), report.toString());
    }

    private TemplateRenderData sampleData() {
        return TemplateRenderData.builder()
                .name("张三")
                .phone("13800000000")
                .email("zhang@example.com")
                .jobIntention("Java开发工程师")
                .education("某某大学 计算机本科")
                .workExperience("某科技公司 后端开发")
                .project("职通车简历系统")
                .skill("Java Spring MySQL")
                .summary("热爱编程，沟通良好")
                .build();
    }

    private String documentXml(byte[] docx) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("document.xml missing");
    }
}
