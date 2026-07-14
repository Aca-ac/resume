package com.resume.module.resume.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against single-page truncation when exporting long resumes. */
class ResumeExportServiceTest {

    @Test
    void manyLinesShouldCreateMultiplePages() throws Exception {
        ResumeExportService svc = new ResumeExportService();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 120; i++) {
            sb.append("Line ").append(i).append(": multi-page export test content ABCDEFG").append('\n');
        }
        byte[] pdf = svc.exportPdf("Multi page resume", sb.toString());
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 2, "expected multi-page, got " + doc.getNumberOfPages());
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Line 1:"));
            assertTrue(text.contains("Line 120:"));
        }
    }
}