package com.resume.module.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ResumeExportService {

    private static final float MARGIN = 50f;
    private static final float FONT_SIZE = 12f;
    private static final float LEADING = 16f;
    private static final float MAX_LINE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;

    /**
     * Prefer single TTF first (PDFBox handles them reliably).
     * TTC must be loaded via TrueTypeCollection — raw stream load often causes 乱码 for special glyphs.
     */
    private static final String[] FONT_TTF_CANDIDATES = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simfang.ttf",
            "C:/Windows/Fonts/simkai.ttf",
            "C:/Windows/Fonts/simsunb.ttf",
            "C:/Windows/Fonts/SimsunExtG.ttf"
    };

    private static final String[] FONT_TTC_CANDIDATES = {
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simsun.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc"
    };

    /** Preferred face names inside common TTC files. */
    private static final String[] TTC_FACE_NAMES = {
            "Microsoft YaHei",
            "MicrosoftYaHei",
            "微软雅黑",
            "SimSun",
            "NSimSun",
            "宋体",
            "WenQuanYi Zen Hei",
            "Noto Sans CJK SC",
            "PingFang SC",
            "STHeiti"
    };

    private static final Map<Integer, String> SPECIAL_CHAR_FALLBACKS = buildSpecialCharFallbacks();

    public byte[] exportPdf(String title, String content) throws IOException {
        List<AutoCloseable> fontResources = new ArrayList<>();
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontLoadResult fontResult = loadFont(doc, fontResources);
            PDFont font = fontResult.font();
            boolean chineseCapable = fontResult.chineseCapable();

            String fullText = nullToEmpty(title) + "\n\n" + nullToEmpty(content);
            fullText = prepareTextForPdf(fullText);
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
                    // Encode per codepoint so one bad glyph cannot corrupt the whole line
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
        } finally {
            for (AutoCloseable resource : fontResources) {
                try {
                    resource.close();
                } catch (Exception e) {
                    log.debug("Close font resource: {}", e.getMessage());
                }
            }
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

    private FontLoadResult loadFont(PDDocument doc, List<AutoCloseable> fontResources) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.otf")) {
            if (in != null) {
                return new FontLoadResult(PDType0Font.load(doc, in, true), true);
            }
        } catch (Exception e) {
            log.debug("Classpath CJK font not available: {}", e.getMessage());
        }

        for (String path : FONT_TTF_CANDIDATES) {
            Path p = Path.of(path);
            if (!Files.isRegularFile(p)) {
                continue;
            }
            try (InputStream in = Files.newInputStream(p)) {
                PDFont font = PDType0Font.load(doc, in, true);
                log.info("PDF export using TTF font: {}", path);
                return new FontLoadResult(font, true);
            } catch (Exception e) {
                log.warn("Failed to load TTF {}: {}", path, e.getMessage());
            }
        }

        for (String path : FONT_TTC_CANDIDATES) {
            Path p = Path.of(path);
            if (!Files.isRegularFile(p)) {
                continue;
            }
            try {
                PDFont font = loadFromTtc(doc, p, fontResources);
                if (font != null) {
                    log.info("PDF export using TTC font: {}", path);
                    return new FontLoadResult(font, true);
                }
            } catch (Exception e) {
                log.warn("Failed to load TTC {}: {}", path, e.getMessage());
            }
        }

        return new FontLoadResult(new PDType1Font(Standard14Fonts.FontName.HELVETICA), false);
    }

    /**
     * Load a face from a TrueType Collection. Never feed .ttc bytes directly to PDType0Font.load(stream)
     * — that often embeds the wrong cmap and shows special characters as 乱码.
     * Keep the collection open until after PDF save (caller closes fontResources).
     */
    private PDFont loadFromTtc(PDDocument doc, Path ttcPath, List<AutoCloseable> fontResources) throws IOException {
        TrueTypeCollection ttc = new TrueTypeCollection(ttcPath.toFile());
        fontResources.add(ttc);
        for (String face : TTC_FACE_NAMES) {
            try {
                TrueTypeFont ttf = ttc.getFontByName(face);
                if (ttf != null) {
                    return PDType0Font.load(doc, ttf, true);
                }
            } catch (Exception ignored) {
                // try next face name
            }
        }
        AtomicReference<TrueTypeFont> first = new AtomicReference<>();
        ttc.processAllFonts(font -> {
            if (first.get() == null) {
                first.set(font);
            }
        });
        TrueTypeFont ttf = first.get();
        if (ttf == null) {
            return null;
        }
        return PDType0Font.load(doc, ttf, true);
    }

    /**
     * NFKC + strip invisible controls + map common resume special symbols to font-safe forms.
     */
    private String prepareTextForPdf(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); ) {
            int cp = normalized.codePointAt(i);
            i += Character.charCount(cp);

            if (isInvisibleOrBidi(cp)) {
                continue;
            }
            if (Character.getType(cp) == Character.PRIVATE_USE) {
                sb.append('?');
                continue;
            }
            // Emoji / symbols outside BMP often missing in CJK fonts
            if (cp > 0xFFFF) {
                String mapped = SPECIAL_CHAR_FALLBACKS.get(cp);
                sb.append(mapped != null ? mapped : "?");
                continue;
            }

            String mapped = SPECIAL_CHAR_FALLBACKS.get(cp);
            if (mapped != null) {
                sb.append(mapped);
            } else {
                sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }

    private boolean isInvisibleOrBidi(int cp) {
        return cp == 0x00AD // soft hyphen
                || cp == 0x034F
                || cp == 0x061C
                || cp == 0x180E
                || cp == 0x200B || cp == 0x200C || cp == 0x200D || cp == 0x200E || cp == 0x200F
                || cp == 0x202A || cp == 0x202B || cp == 0x202C || cp == 0x202D || cp == 0x202E
                || cp == 0x2060 || cp == 0x2061 || cp == 0x2062 || cp == 0x2063 || cp == 0x2064
                || cp == 0x2066 || cp == 0x2067 || cp == 0x2068 || cp == 0x2069
                || cp == 0xFEFF
                || cp == 0xFFFC || cp == 0xFFFD;
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
                    // skip glyph that cannot be measured
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

    /**
     * Write text codepoint-by-codepoint so a single unsupported glyph does not garble the line.
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
            } else {
                if (!chunk.isEmpty()) {
                    cs.showText(chunk.toString());
                    chunk.setLength(0);
                }
                String alt = fallbackForMissingGlyph(cp);
                if (alt != null && canEncode(font, alt)) {
                    cs.showText(alt);
                } else if (canEncode(font, "?")) {
                    cs.showText("?");
                }
            }
        }
        if (!chunk.isEmpty()) {
            cs.showText(chunk.toString());
        }
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
            } else if (cp >= 32 && cp != '\n' && cp != '\r') {
                sb.appendCodePoint(cp);
            } else if (cp < 32) {
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
        String mapped = SPECIAL_CHAR_FALLBACKS.get(cp);
        if (mapped != null) {
            return mapped;
        }
        return switch (cp) {
            case 0x2ED8 -> "青";
            case 0x2E85 -> "人";
            case 0x2E8C -> "爪";
            case 0x2EBE -> "草";
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

    private static Map<Integer, String> buildSpecialCharFallbacks() {
        Map<Integer, String> m = new LinkedHashMap<>();
        // dashes / minus variants
        putAll(m, "-", 0x2010, 0x2011, 0x2012, 0x2013, 0x2014, 0x2015, 0x2212, 0xFE58, 0xFE63, 0xFF0D);
        // quotes
        putAll(m, "\"", 0x201C, 0x201D, 0x201E, 0x201F, 0x00AB, 0x00BB, 0x300A, 0x300B);
        putAll(m, "'", 0x2018, 0x2019, 0x201A, 0x201B, 0x2032, 0x2035);
        // bullets / list markers
        putAll(m, "*", 0x2022, 0x2023, 0x2043, 0x25CF, 0x25CB, 0x25A0, 0x25A1, 0x25AA, 0x25AB,
                0x25E6, 0x2219, 0x00B7, 0x30FB, 0xFF65);
        // ellipsis / middle dots
        m.put(0x2026, "...");
        m.put(0x22EF, "...");
        // arrows
        m.put(0x2190, "<-");
        m.put(0x2192, "->");
        m.put(0x2191, "^");
        m.put(0x2193, "v");
        m.put(0x21D2, "=>");
        m.put(0x21D0, "<=");
        m.put(0x2194, "<->");
        // stars / marks common on resumes
        putAll(m, "*", 0x2605, 0x2606, 0x2736, 0x066D, 0x203B);
        putAll(m, "#", 0x3012);
        // spaces
        putAll(m, " ", 0x00A0, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006,
                0x2007, 0x2008, 0x2009, 0x200A, 0x202F, 0x205F, 0x3000);
        // CJK radicals often seen after OCR / font subsetting
        m.put(0x2ED8, "青");
        m.put(0x2E85, "人");
        m.put(0x2E8C, "爪");
        m.put(0x2EBE, "草");
        return m;
    }

    private static void putAll(Map<Integer, String> map, String replacement, int... codepoints) {
        for (int cp : codepoints) {
            map.put(cp, replacement);
        }
    }

    private record FontLoadResult(PDFont font, boolean chineseCapable) {
    }
}
