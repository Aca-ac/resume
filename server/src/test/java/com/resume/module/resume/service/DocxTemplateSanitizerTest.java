package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxTemplateSanitizerTest {

    private final TemplateRenderService renderService = new TemplateRenderService();

    @Test
    void sanitizeAndRenderAcademicKeepsDecorations() throws Exception {
        byte[] docx = render("academic");
        String xml = documentXml(docx);
        assertTrue(xml.contains("wp:anchor") || xml.contains("a:blip") || xml.contains("v:shape"),
                "should keep decorative graphics");
        assertTrue(xml.contains("张三"));
        assertFalse(xml.contains("{{name}}"));
    }

    @Test
    void lightSanitizePreservesAcademicVmlBlocks() throws Exception {
        String raw = readRawXml("templates/resumes/academic/template.docx");
        int before = count(raw, "v:shape");
        String cleaned = DocxTemplateSanitizer.stripFloatingLayers(raw, DocxTemplateSanitizer.SanitizeMode.LIGHT);
        int after = count(cleaned, "v:shape");
        assertTrue(before >= 6, "academic template should contain VML decorations");
        assertTrue(after >= 4, "light sanitize should keep core decorative VML");
        assertFalse(cleaned.contains("</w:txbxContent></v:textbox>"), "should not leave broken VML textbox");
    }

    @Test
    void lightSanitizeDoesNotDuplicatePlaceholderText() throws Exception {
        String raw = readRawXml("templates/resumes/academic/template.docx");
        String cleaned = DocxTemplateSanitizer.stripFloatingLayers(raw, DocxTemplateSanitizer.SanitizeMode.LIGHT);
        assertEquals(1, count(cleaned, "{{phone}}"));
        assertEquals(1, count(cleaned, "{{email}}"));
        assertEquals(1, count(cleaned, "{{@photo}}"));
    }

    @Test
    void sanitizeAndRenderCreativeKeepsFloatingLayout() throws Exception {
        byte[] docx = render("creative");
        String xml = documentXml(docx);
        assertTrue(xml.contains("wp:anchor"), "creative should keep floating anchors for PDF visibility");
        assertTrue(xml.contains("张三") || xml.contains("某某大学") || xml.contains("Java"));
    }

    @Test
    void sanitizeAndRenderSimpleKeepsFloatingLayout() throws Exception {
        byte[] docx = render("simple");
        String xml = documentXml(docx);
        assertTrue(xml.contains("wp:anchor"), "simple should keep floating anchors for single-page layout");
        assertTrue(xml.contains("张三") || xml.contains("Java"));
    }

    @Test
    void fullSanitizePreservesCreativeBackgroundBlocks() throws Exception {
        String raw = readRawXml("templates/resumes/creative/template.docx");
        String cleaned = DocxTemplateSanitizer.stripFloatingLayers(raw, DocxTemplateSanitizer.SanitizeMode.FULL);
        assertTrue(count(cleaned, "v:rect") >= 20, "creative should keep VML background blocks");
        assertFalse(cleaned.contains("</w:txbxContent></v:textbox>"), "should not leave broken VML textbox");
    }

    @Test
    void fullSanitizeUnwrapsCreativeFloatingLayers() throws Exception {
        String raw = readRawXml("templates/resumes/creative/template.docx");
        String cleaned = DocxTemplateSanitizer.stripFloatingLayers(raw, DocxTemplateSanitizer.SanitizeMode.FULL);
        assertFalse(cleaned.contains("mc:AlternateContent"));
        assertFalse(cleaned.contains("wp:anchor"));
        assertTrue(cleaned.contains("{{education}}"));
    }

    @Test
    void renderWithPhotoDoesNotDuplicatePortrait() throws Exception {
        byte[] jpeg = minimalJpeg();
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
                .photoBytes(jpeg)
                .photoExt("jpg")
                .build();
        byte[] docx = renderService.renderWord("/templates/resumes/academic/template.docx", data);
        String xml = documentXml(docx);
        int portraitAnchors = countPortraitPhotoAnchors(xml);
        assertEquals(1, portraitAnchors, "PDF should contain exactly one portrait photo layer");
    }

    private int countPortraitPhotoAnchors(String xml) {
        Matcher matcher = Pattern.compile("<wp:anchor[^>]*>([\\s\\S]*?)</wp:anchor>").matcher(xml);
        int portraits = 0;
        while (matcher.find()) {
            String anchor = matcher.group();
            if (!anchor.contains("a:blip") && !anchor.contains("pic:pic")) {
                continue;
            }
            if (anchor.contains("v:shape") && anchor.indexOf("v:shape") < anchor.indexOf("pic:pic")) {
                continue;
            }
            if (anchor.contains("wps:txbx") || anchor.contains("txBox=\"1\"")) {
                portraits++;
            }
        }
        return portraits;
    }

    private byte[] minimalJpeg() {
        return java.util.Base64.getDecoder().decode(
                "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////"
                        + "2wBDAf//////////////////////////////////////////////////////////////////////////////////////"
                        + "wAARCAABAAEDAREAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA"
                        + "/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8A0f/Z");
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
