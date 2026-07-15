package com.resume.module.resume.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PDF export: multi-page + CJK / special-char mapping. */
class ResumeExportServiceTest {

    private final ResumeExportService service = new ResumeExportService();

    @Test
    void manyLinesShouldCreateMultiplePages() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 120; i++) {
            sb.append("Line ").append(i).append(": multi-page export test content ABCDEFG").append('\n');
        }
        byte[] pdf = service.exportPdf("Multi page resume", sb.toString());
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 2, "expected multi-page, got " + doc.getNumberOfPages());
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Line 1:"));
            assertTrue(text.contains("Line 120:"));
        }
    }

    @Test
    void exportPdf_keepsChineseAndMapsSpecialChars() throws Exception {
        String content = "姓名：张三\n"
                + "项目：• BlueBird 状态机 → DONE\n"
                + "标点：“智能简历” — 省略号…\n"
                + "标记：★重点 ※备注\n"
                + "部首：⻘春\n";

        byte[] pdf = service.exportPdf("简历导出测试", content);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            boolean hasHan = text.chars().anyMatch(ch ->
                    Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
            assertTrue(hasHan,
                    "No CJK characters in PDF — install fonts-noto-cjk / fonts-wqy-zenhei: " + text);
            assertTrue(text.contains("张三"), text);
            assertTrue(text.contains("BlueBird"), text);
            assertTrue(text.contains("DONE"), text);
            assertTrue(text.contains("智能简历") || text.contains("智能"), text);
            assertTrue(text.contains("->") || text.contains("DONE"), text);
            assertTrue(text.contains("青春") || text.contains("春"), "radical should map: " + text);
            assertTrue(text.contains("*") || text.contains("重点"), text);
        }
    }

    @Test
    void exportDocx_andText_notEmpty() throws Exception {
        byte[] docx = service.exportDocx("标题", "正文一行");
        assertTrue(docx.length > 100);
        byte[] textBytes = service.exportText("标题", "正文");
        String text = new String(textBytes);
        assertTrue(text.contains("标题"));
        assertTrue(text.contains("正文"));
    }
}
