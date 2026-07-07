package com.resume.module.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ResumePdfService {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 14f;

    public byte[] export(String title, String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont titleFont = resolveFont(document, true);
            PDFont bodyFont = resolveFont(document, false);
            float pageWidth = PDRectangle.A4.getWidth();
            float maxWidth = pageWidth - MARGIN * 2;

            List<LineSpec> lines = new ArrayList<>();
            lines.add(new LineSpec(title == null ? "" : title, titleFont, 18f));
            lines.add(new LineSpec("", bodyFont, 11f));
            for (String line : wrapLines(content == null ? "" : content, bodyFont, 11f, maxWidth)) {
                lines.add(new LineSpec(line, bodyFont, 11f));
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float y = page.getMediaBox().getHeight() - MARGIN;
            PDPageContentStream stream = new PDPageContentStream(document, page);

            for (LineSpec spec : lines) {
                if (y < MARGIN) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    y = page.getMediaBox().getHeight() - MARGIN;
                    stream = new PDPageContentStream(document, page);
                }
                stream.beginText();
                stream.setFont(spec.font(), spec.size());
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(sanitize(spec.text()));
                stream.endText();
                y -= LINE_HEIGHT;
            }
            stream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private record LineSpec(String text, PDFont font, float size) {}

    private PDFont resolveFont(PDDocument document, boolean bold) throws IOException {
        ClassPathResource resource = new ClassPathResource("fonts/NotoSansSC-Regular.otf");
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                return PDType0Font.load(document, in);
            }
        }
        for (String path : List.of(
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return PDType0Font.load(document, file);
                } catch (IOException ex) {
                    log.debug("Failed to load font {}", path, ex);
                }
            }
        }
        log.debug("Using Helvetica fallback; add fonts/NotoSansSC-Regular.otf for CJK");
        return new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
    }

    private List<String> wrapLines(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            if (paragraph.isBlank()) {
                result.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char ch = paragraph.charAt(i);
                String candidate = current.toString() + ch;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;
                if (width > maxWidth && !current.isEmpty()) {
                    result.add(current.toString());
                    current = new StringBuilder(String.valueOf(ch));
                } else {
                    current.append(ch);
                }
            }
            if (!current.isEmpty()) {
                result.add(current.toString());
            }
        }
        return result;
    }

    private String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return " ";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\t') {
                sb.append(' ');
            } else if (ch >= 32) {
                sb.append(ch);
            }
        }
        return sb.isEmpty() ? " " : sb.toString();
    }
}