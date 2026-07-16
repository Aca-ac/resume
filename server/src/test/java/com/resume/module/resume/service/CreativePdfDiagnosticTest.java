package com.resume.module.resume.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.Texts;
import com.resume.module.resume.dto.TemplateRenderData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

class CreativePdfDiagnosticTest {

    private static final String SOFFICE = "C:/Program Files/LibreOffice/program/soffice.com";

    private final LibreOfficePdfConverter pdfConverter = new LibreOfficePdfConverter(
            new com.resume.config.ExportProperties() {{
                setLibreofficeCommand(SOFFICE);
                setConvertTimeoutSeconds(120);
            }}
    );

    @Test
    void diagnoseCreativeLightVsFull() throws Exception {
        Assumptions.assumeTrue(Files.exists(Path.of(SOFFICE)), "Skip: LibreOffice not installed");
        byte[] poiOut = renderWithPoiOnly();
        Files.write(Path.of("target/creative-poi-only.docx"), poiOut);

        TemplateRenderData data = sampleData();
        byte[] light = sanitize(poiOut, data, DocxTemplateSanitizer.SanitizeMode.LIGHT);
        byte[] full = sanitize(poiOut, data, DocxTemplateSanitizer.SanitizeMode.FULL);
        Files.write(Path.of("target/creative-light.docx"), light);
        Files.write(Path.of("target/creative-full.docx"), full);

        byte[] lightPdf = pdfConverter.convertDocxToPdf(light, "creative-light");
        byte[] fullPdf = pdfConverter.convertDocxToPdf(full, "creative-full");
        Files.write(Path.of("target/creative-light.pdf"), lightPdf);
        Files.write(Path.of("target/creative-full.pdf"), fullPdf);

        System.out.println("=== poi-only ===");
        printStats(poiOut);
        System.out.println("=== LIGHT ===");
        printStats(light);
        System.out.println("LIGHT pdf bytes=" + lightPdf.length + " hasName=" + pdfContains(lightPdf, "张三")
                + " text=" + pdfText(lightPdf));
        System.out.println("=== FULL ===");
        printStats(full);
        System.out.println("FULL pdf bytes=" + fullPdf.length + " hasName=" + pdfContains(fullPdf, "张三")
                + " text=" + pdfText(fullPdf));
        org.junit.jupiter.api.Assertions.assertTrue(
                lightPdf.length > fullPdf.length * 2,
                "LIGHT mode should produce much richer PDF than FULL unwrap for creative");
        org.junit.jupiter.api.Assertions.assertTrue(
                pdfText(lightPdf).contains("张三"),
                "LIGHT creative PDF should contain rendered name text");
    }

    private byte[] renderWithPoiOnly() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("phone", "13800000000");
        map.put("email", "zhang@example.com");
        map.put("jobIntention", Texts.of("Java开发").create());
        map.put("education", Texts.of("某某大学 计算机 本科").create());
        map.put("workExperience", Texts.of("某公司 后端开发").create());
        map.put("project", Texts.of("简历系统").create());
        map.put("skill", Texts.of("Java, Spring").create());
        map.put("summary", Texts.of("热爱编程").create());
        map.put("photo", null);

        try (var in = getClass().getClassLoader().getResourceAsStream("templates/resumes/creative/template.docx")) {
            try (XWPFTemplate t = XWPFTemplate.compile(in).render(map);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                t.write(out);
                return out.toByteArray();
            }
        }
    }

    private byte[] sanitize(byte[] poiOut, TemplateRenderData data, DocxTemplateSanitizer.SanitizeMode mode)
            throws Exception {
        return DocxTemplateSanitizer.normalize(
                poiOut, DocxTemplateSanitizer.textValuesFrom(data), false, mode);
    }

    private TemplateRenderData sampleData() {
        return TemplateRenderData.builder()
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
    }

    private void printStats(byte[] docx) throws Exception {
        String xml = documentXml(docx);
        System.out.println("  anchors=" + count(xml, "wp:anchor")
                + " v:rect=" + count(xml, "v:rect")
                + " wps:txbx=" + count(xml, "wps:txbx")
                + " 张三=" + xml.contains("张三")
                + " alt=" + count(xml, "mc:AlternateContent"));
    }

    private boolean pdfContains(byte[] pdf, String text) {
        return new String(pdf, StandardCharsets.ISO_8859_1).contains(text);
    }

    private String pdfText(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc).replaceAll("\\s+", " ").trim();
        }
    }

    private String documentXml(byte[] docx) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docx))) {
            var entry = zis.getNextEntry();
            while (entry != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                entry = zis.getNextEntry();
            }
        }
        throw new IllegalStateException("no document.xml");
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
