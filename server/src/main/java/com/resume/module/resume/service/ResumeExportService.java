package com.resume.module.resume.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeExportService {

    public byte[] exportPdf(String title, String content) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 14;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset(margin, y);
                for (String line : wrapLines(title + "\n\n" + nullToEmpty(content), 80)) {
                    if (y < margin) {
                        break;
                    }
                    cs.showText(sanitize(line));
                    cs.newLineAtOffset(0, -leading);
                    y -= leading;
                }
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    public byte[] exportDocx(String title, String content) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph titlePara = doc.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setText(nullToEmpty(title));

            for (String line : nullToEmpty(content).split("\\R")) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line);
            }
            doc.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportText(String title, String content) {
        String text = nullToEmpty(title) + "\n\n" + nullToEmpty(content);
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private List<String> wrapLines(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            while (paragraph.length() > width) {
                lines.add(paragraph.substring(0, width));
                paragraph = paragraph.substring(width);
            }
            lines.add(paragraph);
        }
        return lines;
    }

    private String sanitize(String line) {
        return line.replace('\t', ' ');
    }
}
