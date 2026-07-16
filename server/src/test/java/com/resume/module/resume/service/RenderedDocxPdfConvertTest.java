package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderedDocxPdfConvertTest {

    private final TemplateRenderService renderService = new TemplateRenderService();
    private final LibreOfficePdfConverter pdfConverter = new LibreOfficePdfConverter(
            new com.resume.config.ExportProperties() {{
                setLibreofficeCommand("C:/Program Files/LibreOffice/program/soffice.com");
                setConvertTimeoutSeconds(120);
            }}
    );

    @Test
    void renderedAcademicDocxConvertsToPdf() throws Exception {
        byte[] docx = render("academic");
        Path debug = Path.of("target/academic-rendered.docx");
        Files.createDirectories(debug.getParent());
        Files.write(debug, docx);
        byte[] pdf = pdfConverter.convertDocxToPdf(docx, "academic-test");
        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
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
}
