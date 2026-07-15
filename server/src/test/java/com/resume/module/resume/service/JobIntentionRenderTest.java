package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JobIntentionRenderTest {

    private final TemplateRenderService renderService = new TemplateRenderService();

    @Test
    void allStylesRenderJobIntention() throws Exception {
        TemplateRenderData data = TemplateRenderData.builder()
                .name("张三")
                .phone("13800000000")
                .email("zhang@example.com")
                .jobIntention("Java开发工程师")
                .education("某某大学")
                .workExperience("某公司")
                .project("简历系统")
                .skill("Java")
                .summary("热爱编程")
                .build();

        for (String style : new String[]{"academic", "simple", "professional", "creative"}) {
            byte[] docx = renderService.renderWord("/templates/resumes/" + style + "/template.docx", data);
            String xml = documentXml(docx);
            assertTrue(xml.contains("Java开发工程师"),
                    style + " export should contain jobIntention value, leftover=" + xml.contains("{{jobIntention}}"));
        }
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
