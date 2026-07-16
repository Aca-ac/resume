package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AllTemplatesPdfConvertTest {

    private static final String SOFFICE = "C:/Program Files/LibreOffice/program/soffice.com";

    private final TemplateRenderService renderService = new TemplateRenderService();
    private final LibreOfficePdfConverter pdfConverter = new LibreOfficePdfConverter(
            new com.resume.config.ExportProperties() {{
                setLibreofficeCommand(SOFFICE);
                setConvertTimeoutSeconds(120);
            }}
    );

    @ParameterizedTest
    @ValueSource(strings = {"academic", "simple", "professional", "creative"})
    void renderedDocxConvertsToPdf(String style) throws Exception {
        Assumptions.assumeTrue(Files.exists(Path.of(SOFFICE)), "Skip: LibreOffice not installed");
        byte[] docx = render(style);
        Path debug = Path.of("target", style + "-rendered.docx");
        Files.createDirectories(debug.getParent());
        Files.write(debug, docx);
        byte[] pdf = pdfConverter.convertDocxToPdf(docx, style + "-test");
        Files.write(Path.of("target", style + "-rendered.pdf"), pdf);
        assertTrue(pdf.length > 100, style + " pdf too small");
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"), style + " not pdf");
        if ("creative".equals(style)) {
            assertTrue(pdf.length > 50_000, "creative PDF should retain visible content (~98KB with anchors)");
        }
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
