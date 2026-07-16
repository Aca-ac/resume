package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 渲染后清理 docx：合并 AlternateContent 重复层；creative 额外去掉浮动 anchor；补全 poi-tl 未替换占位符。
 */
final class DocxTemplateSanitizer {

    enum SanitizeMode {
        /** 仅合并 AlternateContent，保留装饰性 anchor（简约/学术/专业） */
        LIGHT,
        /** 合并 AlternateContent 并移除全部 anchor（创意模板 PDF 叠层修复） */
        FULL
    }

    private static final Pattern ANCHOR = Pattern.compile("<wp:anchor[^>]*>([\\s\\S]*?)</wp:anchor>");
    private static final Pattern ALTERNATE = Pattern.compile("<mc:AlternateContent>[\\s\\S]*?</mc:AlternateContent>");
    private static final Pattern CHOICE_INNER = Pattern.compile("<mc:Choice[^>]*>([\\s\\S]*?)</mc:Choice>");
    private static final Pattern FALLBACK = Pattern.compile("<mc:Fallback>([\\s\\S]*?)</mc:Fallback>");
    private static final Pattern TEXT_RUN = Pattern.compile("<w:t[^>]*>([\\s\\S]*?)</w:t>");
    private static final Pattern PICT = Pattern.compile("<w:pict>[\\s\\S]*?</w:pict>");
    private static final Pattern DRAWING = Pattern.compile("<w:drawing>[\\s\\S]*?</w:drawing>");
    private static final Pattern TEXTBOX_CONTENT = Pattern.compile("<w:txbxContent>[\\s\\S]*?</w:txbxContent>");
    private static final Pattern VML_TEXTBOX = Pattern.compile("<v:textbox>[\\s\\S]*?</v:textbox>", Pattern.CASE_INSENSITIVE);

    private static final Pattern VML_RECT = Pattern.compile("<v:rect\\b[^>]*(?:/>|>[\\s\\S]*?</v:rect>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VML_OVAL = Pattern.compile("<v:oval\\b[^>]*(?:/>|>[\\s\\S]*?</v:oval>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VML_SHAPE = Pattern.compile("<v:shape\\b[^>]*>[\\s\\S]*?</v:shape>", Pattern.CASE_INSENSITIVE);

    private DocxTemplateSanitizer() {
    }

    /** 渲染前：仅合并 AlternateContent，避免 poi-tl 对 Choice/Fallback 双占位各渲染一次（部分模板 XML 会因此损坏，慎用）。 */
    static byte[] prepareTemplate(byte[] docx) throws IOException {
        String xml = readDocumentXml(docx);
        xml = mergeAlternateContent(xml, SanitizeMode.LIGHT);
        return writeDocumentXml(docx, xml);
    }

    static byte[] normalize(byte[] docx, Map<String, String> textValues, boolean hasPhoto) throws IOException {
        return normalize(docx, textValues, hasPhoto, SanitizeMode.LIGHT);
    }

    static byte[] normalize(byte[] docx, Map<String, String> textValues, boolean hasPhoto, SanitizeMode mode)
            throws IOException {
        String xml = readDocumentXml(docx);
        xml = stripFloatingLayers(xml, mode);
        if (hasPhoto) {
            xml = dedupePortraitPhotoAnchors(xml);
        }
        if (mode == SanitizeMode.LIGHT) {
            xml = removeBrokenVmlTextboxes(xml);
        }
        xml = fillRemainingPlaceholders(xml, textValues);
        if (!hasPhoto) {
            xml = xml.replace("{{@photo}}", "");
        }
        return writeDocumentXml(docx, xml);
    }

    static Map<String, String> textValuesFrom(TemplateRenderData data) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", safe(data.getName()));
        values.put("phone", safe(data.getPhone()));
        values.put("email", safe(data.getEmail()));
        values.put("jobIntention", safe(data.getJobIntention()));
        values.put("education", safe(data.getEducation()));
        values.put("workExperience", safe(data.getWorkExperience()));
        values.put("project", safe(data.getProject()));
        values.put("skill", safe(data.getSkill()));
        values.put("summary", safe(data.getSummary()));
        return values;
    }

    static String stripFloatingLayers(String xml) {
        return stripFloatingLayers(xml, SanitizeMode.FULL);
    }

    static String stripFloatingLayers(String xml, SanitizeMode mode) {
        String merged = mergeAlternateContent(xml, mode);
        if (mode == SanitizeMode.FULL) {
            merged = removeBrokenVmlTextboxes(merged);
            return unwrapAnchors(merged);
        }
        return merged;
    }

    private static String unwrapAnchors(String xml) {
        return ANCHOR.matcher(xml).replaceAll("$1");
    }

    /** poi-tl 可能对 AlternateContent 的 Choice/Fallback 各渲染一次照片，保留首个一寸照 anchor。 */
    private static String dedupePortraitPhotoAnchors(String xml) {
        StringBuilder out = new StringBuilder(xml.length());
        Matcher matcher = ANCHOR.matcher(xml);
        int last = 0;
        boolean keptPortrait = false;
        while (matcher.find()) {
            out.append(xml, last, matcher.start());
            String anchor = matcher.group();
            if (isPortraitPhotoAnchor(anchor)) {
                if (!keptPortrait) {
                    out.append(anchor);
                    keptPortrait = true;
                }
            } else {
                out.append(anchor);
            }
            last = matcher.end();
        }
        out.append(xml.substring(last));
        return out.toString();
    }

    private static boolean isPortraitPhotoAnchor(String anchor) {
        return (anchor.contains("wps:txbx") || anchor.contains("txBox=\"1\""))
                && (anchor.contains("a:blip") || anchor.contains("pic:pic"));
    }

    private static final Pattern BROKEN_VML_TEXTBOX = Pattern.compile(
            "<v:textbox[^>]*>\\s*(?:</w:r>\\s*)?(?:</w:p>\\s*)?(?:</w:txbxContent>\\s*)?</v:textbox>",
            Pattern.CASE_INSENSITIVE);

    private static String removeBrokenVmlTextboxes(String xml) {
        String prev;
        String result = xml;
        do {
            prev = result;
            result = BROKEN_VML_TEXTBOX.matcher(result).replaceAll("");
        } while (!result.equals(prev));
        return result;
    }

    private static String mergeAlternateContent(String xml) {
        return mergeAlternateContent(xml, SanitizeMode.LIGHT);
    }

    private static String mergeAlternateContent(String xml, SanitizeMode mode) {
        StringBuilder out = new StringBuilder(xml.length());
        Matcher altMatcher = ALTERNATE.matcher(xml);
        int last = 0;
        while (altMatcher.find()) {
            out.append(xml, last, altMatcher.start());
            out.append(resolveAlternate(altMatcher.group(), mode));
            last = altMatcher.end();
        }
        out.append(xml.substring(last));
        return out.toString();
    }

    private static String resolveAlternate(String block, SanitizeMode mode) {
        Matcher choice = CHOICE_INNER.matcher(block);
        Matcher fallback = FALLBACK.matcher(block);
        String choiceInner = choice.find() ? choice.group(1) : "";
        String fallbackInner = fallback.find() ? fallback.group(1) : "";
        boolean choiceHas = hasContent(choiceInner);
        boolean fallbackHas = hasContent(fallbackInner);

        if (choiceHas && fallbackHas) {
            String merged = choiceInner;
            merged += extractFallbackSupplement(fallbackInner, choiceInner, mode);
            return merged;
        }
        if (choiceHas) {
            return choiceInner;
        }
        if (fallbackHas) {
            return fallbackInner;
        }
        return "";
    }

    private static String resolveAlternate(String block) {
        return resolveAlternate(block, SanitizeMode.LIGHT);
    }

    private static String fillRemainingPlaceholders(String xml, Map<String, String> textValues) {
        String result = xml;
        for (Map.Entry<String, String> entry : textValues.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", escapeXml(entry.getValue()));
        }
        return result;
    }

    private static boolean containsInlinePhoto(String xml) {
        return xml.contains("pic:pic") || xml.contains("a:blip");
    }

    private static String extractFallbackSupplement(String fallbackInner, String choiceInner, SanitizeMode mode) {
        StringBuilder sb = new StringBuilder();
        appendGraphicsAbsentFromChoice(fallbackInner, choiceInner, sb, mode);
        return sb.toString();
    }

    private static void appendGraphicsAbsentFromChoice(
            String source, String choiceInner, StringBuilder sb, SanitizeMode mode) {
        boolean choiceHasPhoto = containsInlinePhoto(choiceInner) || choiceInner.contains("@photo");
        Matcher pict = PICT.matcher(source);
        while (pict.find()) {
            String original = pict.group();
            if (containsTextLayer(original)) {
                if (mode == SanitizeMode.FULL) {
                    appendCreativeDecorativeVml(original, choiceInner, choiceHasPhoto, sb);
                }
                continue;
            }
            appendGraphicIfAbsent(stripTextFromGraphicBlock(original), choiceInner, choiceHasPhoto, sb);
        }
        Matcher drawingMatcher = DRAWING.matcher(source);
        while (drawingMatcher.find()) {
            String original = drawingMatcher.group();
            if (containsTextLayer(original)) {
                if (mode == SanitizeMode.FULL) {
                    appendGraphicIfAbsent(stripTextFromGraphicBlock(original), choiceInner, choiceHasPhoto, sb);
                }
                continue;
            }
            appendGraphicIfAbsent(stripTextFromGraphicBlock(original), choiceInner, choiceHasPhoto, sb);
        }
    }

    /** 创意模板 Fallback 常与 Choice 同层；只提取 v:rect 等纯色装饰，跳过含 textbox 的 shape。 */
    private static void appendCreativeDecorativeVml(
            String block, String choiceInner, boolean choiceHasPhoto, StringBuilder sb) {
        appendVmlFragments(VML_RECT, block, choiceInner, choiceHasPhoto, sb);
        appendVmlFragments(VML_OVAL, block, choiceInner, choiceHasPhoto, sb);
        Matcher shapeMatcher = VML_SHAPE.matcher(block);
        while (shapeMatcher.find()) {
            String shape = shapeMatcher.group();
            if (shape.toLowerCase(Locale.ROOT).contains("v:textbox")) {
                continue;
            }
            appendGraphicIfAbsent(stripTextFromGraphicBlock(shape), choiceInner, choiceHasPhoto, sb);
        }
    }

    private static void appendVmlFragments(
            Pattern pattern, String block, String choiceInner, boolean choiceHasPhoto, StringBuilder sb) {
        Matcher matcher = pattern.matcher(block);
        while (matcher.find()) {
            appendGraphicIfAbsent(matcher.group(), choiceInner, choiceHasPhoto, sb);
        }
    }

    private static void appendGraphicIfAbsent(
            String block, String choiceInner, boolean choiceHasPhoto, StringBuilder sb) {
        if (block.isBlank() || !containsDecorativeDrawing(block) || choiceInner.contains(block)) {
            return;
        }
        if (choiceHasPhoto && containsInlinePhoto(block)) {
            return;
        }
        sb.append(block);
    }

    private static boolean containsTextLayer(String block) {
        return block.contains("v:textbox") || block.contains("w:txbxContent");
    }

    private static String stripTextFromGraphicBlock(String block) {
        String stripped = TEXT_RUN.matcher(block).replaceAll("");
        stripped = TEXTBOX_CONTENT.matcher(stripped).replaceAll("");
        stripped = VML_TEXTBOX.matcher(stripped).replaceAll("");
        return stripped;
    }

    private static boolean containsDecorativeDrawing(String xml) {
        return xml.contains("v:shape")
                || xml.contains("v:rect")
                || xml.contains("v:oval")
                || xml.contains("wps:wsp")
                || xml.contains("<w:drawing")
                || xml.contains("<w:pict");
    }

    private static boolean hasContent(String xml) {
        if (xml == null || xml.isBlank()) {
            return false;
        }
        if (containsInlinePhoto(xml)) {
            return true;
        }
        if (containsDecorativeDrawing(xml)) {
            return true;
        }
        Matcher m = TEXT_RUN.matcher(xml);
        while (m.find()) {
            if (!m.group(1).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String readDocumentXml(byte[] docx) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("word/document.xml not found");
    }

    private static byte[] writeDocumentXml(byte[] docx, String documentXml) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(docx.length + 4096);
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docx));
             ZipOutputStream zos = new ZipOutputStream(out)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(new ZipEntry(entry.getName()));
                if ("word/document.xml".equals(entry.getName())) {
                    zos.write(documentXml.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(zis.readAllBytes());
                }
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
