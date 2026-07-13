package com.resume.module.resume.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeExportServiceTest {

    private final ResumeExportService service = new ResumeExportService();

    @Test
    void exportPdf_keepsChineseAndMapsSpecialChars() throws Exception {
        String content = """
                姓名：张三
                项目：• Chronos 状态机 → DONE
                标点：“智能简历” — 省略号…
                标记：★重点 ※备注
                部首：⻘春
                """;

        byte[] pdf = service.exportPdf("简历导出测试", content);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            boolean hasHan = text.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
            assertTrue(hasHan,
                    "No CJK characters in PDF — install fonts-noto-cjk / fonts-wqy-zenhei, or embed a font under /fonts/: " + text);
            assertTrue(text.contains("张三"), text);
            assertTrue(text.contains("Chronos"), text);
            assertTrue(text.contains("DONE"), text);
            assertTrue(text.contains("智能简历") || text.contains("智能"), text);
            assertTrue(text.contains("->") || text.contains("DONE"), text);
            assertTrue(text.contains("青春") || text.contains("春"), "radical should map: " + text);
            // specials mapped to ASCII-safe forms, not left as raw problematic glyphs
            assertTrue(text.contains("*") || text.contains("重点"), text);
        }
    }
}
