package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxTemplateSanitizerTest {

    private final TemplateRenderService renderService = new TemplateRenderService();

    @Test
    void sanitizeAndRenderAcademicNoFloatingLayers() throws Exception {
        byte[] docx = render("academic");
        String xml = documentXml(docx);
        assertFalse(xml.contains("wp:anchor"), "should remove floating anchors");
        assertFalse(xml.contains("wps:txbx"), "should remove floating text boxes");
        assertTrue(xml.contains("张三"));
        assertFalse(xml.contains("{{name}}"));
    }

    @Test
    void sanitizeAndRenderSimpleNoDuplicateLayers() throws Exception {
        byte[] docx = render("simple");
        String xml = documentXml(docx);
        assertFalse(xml.contains("wp:anchor"));
        assertTrue(xml.contains("张三") || xml.contains("Java"));
    }

    @Test
    void dedupeRemovesAnchorsBeforePlaceholderCountDrops() throws Exception {
        String raw = readRawXml("templates/resumes/academic/template.docx");
        int before = count(raw, "{{name}}");
        String cleaned = DocxTemplateSanitizer.stripFloatingLayers(raw);
        int after = count(cleaned, "{{name}}");
        assertTrue(before >= 2, "template has duplicate name placeholders");
        assertTrue(after <= 1, "should keep single name placeholder");
    }

    @Test
    void stripWhiteRunShadingRemovesOnlyWhiteFill() {
        String raw = "<w:rPr><w:shd w:val=\"clear\" w:fill=\"FFFFFF\"/>"
                + "<w:shd w:val=\"clear\" w:fill=\"C0C0C0\"/><w:sz w:val=\"22\"/></w:rPr>";
        String cleaned = DocxTemplateSanitizer.stripWhiteBackgrounds(raw);
        assertFalse(cleaned.contains("FFFFFF"));
        assertTrue(cleaned.contains("C0C0C0"));
        assertTrue(cleaned.contains("w:sz"));
    }

    @Test
    void sanitizeAndRenderCreativeRemovesWhiteRunShading() throws Exception {
        byte[] docx = render("creative");
        String xml = documentXml(docx);
        assertFalse(Pattern.compile("<w:shd[^>]*w:fill=\"FFFFFF\"", Pattern.CASE_INSENSITIVE).matcher(xml).find());
        assertTrue(xml.contains("某某大学") || xml.contains("本科"));
        assertTrue(xml.contains("热爱编程"));
    }

    private byte[] render(String style) throws Exception {
        TemplateRenderData data = TemplateRenderData.builder()
                .name("张三")
                .phone("13800000000")
                .email("zhang@example.com")
                .jobIntention("Java开发")
                .education("某某大学 计算机 本科")
                .workExperience("某公司 后端开发")
                .project("简历系统")
                .skill("Java, Spring")
                .summary("热爱编程")
                .build();
        return renderService.renderWord("/templates/resumes/" + style + "/template.docx", data);
    }

    private String readRawXml(String classpath) throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream(classpath)) {
            if (in == null) {
                throw new IllegalStateException("missing " + classpath);
            }
            try (ZipInputStream zis = new ZipInputStream(in)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if ("word/document.xml".equals(entry.getName())) {
                        return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }
        }
        throw new IllegalStateException("document.xml missing");
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

    private int count(String text, String needle) {
        int c = 0;
        int i = 0;
        while ((i = text.indexOf(needle, i)) >= 0) {
            c++;
            i += needle.length();
        }
        return c;
    }
}
