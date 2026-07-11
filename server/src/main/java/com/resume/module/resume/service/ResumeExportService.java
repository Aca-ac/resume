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

    /** Prefer static TTF; variable fonts are unreliable in PDFBox. */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
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
            // NFKC: ⻘→青 等部首/兼容字；再剔除当前字体没有的字形，避免导出直接失败
            fullText = Normalizer.normalize(fullText, Normalizer.Form.NFKC);
            fullText = replaceMissingGlyphs(font, fullText);
            if (!chineseCapable) {
                log.warn("No CJK font found; PDF will replace non-Latin characters");
                fullText = "[提示: 未找到中文字体，部分字符可能显示为 ?]\n\n" + toPdfSafe(fullText);
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
                    cs.showText(sanitize(line));
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
            titleRun.setFontFamily("微软雅黑");
            titleRun.setText(nullToEmpty(title));

            for (String line : nullToEmpty(content).split("\\R", -1)) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setFontFamily("微软雅黑");
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

    private FontLoadResult loadFont(PDDocument doc) throws IOException {
        // Optional bundled font: src/main/resources/fonts/NotoSansSC-Regular.otf
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
                float width = font.getStringWidth(sanitize(candidate)) / 1000f * fontSize;
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

    /** PDF text operators reject control characters other than space. */
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

    /**
     * Drop / replace codepoints the loaded font cannot encode
     * (e.g. CJK radicals like U+2ED8 ⻘ missing in SimHei).
     */
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

    /** Manual map for common radicals / variants that NFKC may not fully cover. */
    private String fallbackForMissingGlyph(int cp) {
        return switch (cp) {
            case 0x2ED8 -> "青"; // ⻘
            case 0x2E85 -> "长"; // ⺅-like radicals — best-effort
            case 0x2E8C -> "爪";
            case 0x2EBE -> "草";
            default -> null;
        };
    }

    /** Fallback when only Helvetica is available. */
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
