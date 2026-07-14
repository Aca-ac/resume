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
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ResumeExportService {

    private static final float MARGIN = 50f;
    private static final float FONT_SIZE = 12f;
    private static final float LEADING = 16f;
    private static final float MAX_LINE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;

    /** Prefer single TTF first — raw TTC stream load often fails on Windows. */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simfang.ttf",
            "C:/Windows/Fonts/simkai.ttf",
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simsun.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc"
    };

    public byte[] exportPdf(String title, String content) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontLoadResult fontResult = loadFont(doc);
            PDFont font = fontResult.font();
            boolean chineseCapable = fontResult.chineseCapable();

            String fullText = nullToEmpty(title) + "\n\n" + nullToEmpty(content);
            fullText = Normalizer.normalize(fullText, Normalizer.Form.NFKC);
            fullText = replaceMissingGlyphs(font, fullText);
            if (!chineseCapable) {
                log.warn("No CJK font found; PDF will replace non-Latin characters");
                fullText = "[Tip: CJK font missing; non-Latin chars may show as ?]\n\n" + toPdfSafe(fullText);
            }

            List<String> lines = wrapLines(font, fullText, FONT_SIZE, MAX_LINE_WIDTH);
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - MARGIN;
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            try {
                cs.beginText();
                cs.setFont(font, FONT_SIZE);
                cs.newLineAtOffset(MARGIN, y);

                for (String line : lines) {
                    // Never break/truncate: open a new page when the current page is full.
                    if (y - LEADING < MARGIN) {
                        cs.endText();
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                        cs = new PDPageContentStream(doc, page);
                        cs.beginText();
                        cs.setFont(font, FONT_SIZE);
                        cs.newLineAtOffset(MARGIN, y);
                    }
                    writeLineSafe(cs, font, sanitize(line));
                    cs.newLineAtOffset(0, -LEADING);
                    y -= LEADING;
                }
                cs.endText();
            } finally {
                cs.close();
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
            titleRun.setFontFamily("Microsoft YaHei");
            titleRun.setText(nullToEmpty(title));

            for (String line : nullToEmpty(content).split("\\R", -1)) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setFontFamily("Microsoft YaHei");
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

    /**
     * Write line glyph-by-glyph so one unencodable character cannot abort the whole export
     * (historically that failure looked like "only the first page exported").
     */
    private void writeLineSafe(PDPageContentStream cs, PDFont font, String line) throws IOException {
        if (line == null || line.isEmpty()) {
            cs.showText("");
            return;
        }
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            int n = Character.charCount(cp);
            String ch = line.substring(i, i + n);
            i += n;
            if (canEncode(font, ch)) {
                chunk.append(ch);
                continue;
            }
            flushChunk(cs, chunk);
            String alt = fallbackForMissingGlyph(cp);
            if (alt != null && canEncode(font, alt)) {
                cs.showText(alt);
            } else if (canEncode(font, "?")) {
                cs.showText("?");
            }
        }
        flushChunk(cs, chunk);
    }

    private void flushChunk(PDPageContentStream cs, StringBuilder chunk) throws IOException {
        if (chunk.isEmpty()) {
            return;
        }
        cs.showText(chunk.toString());
        chunk.setLength(0);
    }

    private FontLoadResult loadFont(PDDocument doc) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.otf")) {
            if (in != null) {
                return new FontLoadResult(PDType0Font.load(doc, in, true), true);
            }
        } catch (Exception e) {
            log.debug("Classpath CJK font not available: {}", e.getMessage());
        }

        for (String path : FONT_CANDIDATES) {
            Path p = Path.of(path);
            if (!Files.isRegularFile(p)) {
                continue;
            }
            try (InputStream in = Files.newInputStream(p)) {
                PDFont font = PDType0Font.load(doc, in, true);
                log.info("PDF export using font: {}", path);
                return new FontLoadResult(font, true);
            } catch (Exception e) {
                log.warn("Failed to load font {}: {}", path, e.getMessage());
            }
        }

        return new FontLoadResult(new PDType1Font(Standard14Fonts.FontName.HELVETICA), false);
    }

    private List<String> wrapLines(PDFont font, String text, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); ) {
                int cp = paragraph.codePointAt(i);
                i += Character.charCount(cp);
                String ch = new String(Character.toChars(cp));
                String candidate = current + ch;
                float width;
                try {
                    width = font.getStringWidth(sanitize(candidate)) / 1000f * fontSize;
                } catch (Exception e) {
                    // If width measurement fails, force a line break before this char.
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                        current = new StringBuilder(ch);
                    } else {
                        lines.add(ch);
                        current = new StringBuilder();
                    }
                    continue;
                }
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(ch);
                } else {
                    current.append(ch);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String sanitize(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\t') {
                sb.append(' ');
            } else if (cp >= 32 || cp == '\n' || cp == '\r') {
                if (cp != '\n' && cp != '\r') {
                    sb.appendCodePoint(cp);
                }
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private String replaceMissingGlyphs(PDFont font, String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int n = Character.charCount(cp);
            String ch = text.substring(i, i + n);
            i += n;
            if (cp == '\n' || cp == '\r' || cp == '\t') {
                sb.append(cp == '\t' ? ' ' : (char) cp);
                continue;
            }
            if (cp < 32) {
                sb.append(' ');
                continue;
            }
            if (canEncode(font, ch)) {
                sb.append(ch);
            } else {
                String alt = fallbackForMissingGlyph(cp);
                if (alt != null && canEncode(font, alt)) {
                    sb.append(alt);
                } else if (canEncode(font, "?")) {
                    sb.append('?');
                }
            }
        }
        return sb.toString();
    }

    private boolean canEncode(PDFont font, String ch) {
        try {
            font.encode(ch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String fallbackForMissingGlyph(int cp) {
        return switch (cp) {
            case 0x2ED8 -> "\u9752";
            case 0x2E85 -> "\u957f";
            case 0x2E8C -> "\u722a";
            case 0x2EBE -> "\u8349";
            default -> null;
        };
    }

    private String toPdfSafe(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp <= 0xFF) {
                sb.appendCodePoint(cp);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private record FontLoadResult(PDFont font, boolean chineseCapable) {
    }
}
