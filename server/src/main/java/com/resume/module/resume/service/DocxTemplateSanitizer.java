package com.resume.module.resume.service;

import com.resume.module.resume.dto.TemplateRenderData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 渲染后清理 docx：去掉浮动层与 VML/Choice 重复内容；清除 LibreOffice PDF 会画成白块的文字底纹；仅补全 poi-tl 未替换的占位符。
 */
final class DocxTemplateSanitizer {

    private static final Pattern ANCHOR = Pattern.compile("<wp:anchor[\\s\\S]*?</wp:anchor>");
    private static final Pattern ALTERNATE = Pattern.compile("<mc:AlternateContent>[\\s\\S]*?</mc:AlternateContent>");
    private static final Pattern CHOICE_INNER = Pattern.compile("<mc:Choice[^>]*>([\\s\\S]*?)</mc:Choice>");
    private static final Pattern FALLBACK = Pattern.compile("<mc:Fallback>([\\s\\S]*?)</mc:Fallback>");
    private static final Pattern TEXT_RUN = Pattern.compile("<w:t[^>]*>([\\s\\S]*?)</w:t>");
    private static final Pattern PARAGRAPH = Pattern.compile("<w:p[\\s\\S]*?</w:p>");
    private static final Pattern SHADING = Pattern.compile("<w:shd[^>]*/>", Pattern.CASE_INSENSITIVE);

    private DocxTemplateSanitizer() {
    }

    static byte[] normalize(byte[] docx, Map<String, String> textValues, boolean hasPhoto) throws IOException {
        String xml = readDocumentXml(docx);
        xml = stripFloatingLayers(xml);
        xml = stripWhiteBackgrounds(xml);
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
        String withoutAnchors = ANCHOR.matcher(xml).replaceAll("");

        StringBuilder out = new StringBuilder(withoutAnchors.length());
        Matcher altMatcher = ALTERNATE.matcher(withoutAnchors);
        int last = 0;
        while (altMatcher.find()) {
            out.append(withoutAnchors, last, altMatcher.start());
            out.append(resolveAlternate(altMatcher.group()));
            last = altMatcher.end();
        }
        out.append(withoutAnchors.substring(last));
        return out.toString();
    }

    static String stripWhiteBackgrounds(String xml) {
        Matcher matcher = SHADING.matcher(xml);
        StringBuilder out = new StringBuilder(xml.length());
        int last = 0;
        while (matcher.find()) {
            String tag = matcher.group();
            if (!isWhiteFill(tag)) {
                continue;
            }
            out.append(xml, last, matcher.start());
            last = matcher.end();
        }
        out.append(xml.substring(last));
        return out.toString();
    }

    private static boolean isWhiteFill(String shadingTag) {
        Matcher fill = Pattern.compile("w:fill=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(shadingTag);
        if (!fill.find()) {
            return false;
        }
        String color = fill.group(1).replace("#", "");
        return "FFFFFF".equalsIgnoreCase(color) || "FFF".equalsIgnoreCase(color);
    }

    private static String resolveAlternate(String block) {
        Matcher choice = CHOICE_INNER.matcher(block);
        Matcher fallback = FALLBACK.matcher(block);
        String choiceInner = choice.find() ? choice.group(1) : "";
        String fallbackInner = fallback.find() ? fallback.group(1) : "";
        boolean choiceHas = hasContent(choiceInner);
        boolean fallbackHas = hasContent(fallbackInner);

        if (choiceHas && fallbackHas) {
            String merged = choiceInner;
            if (!containsInlinePhoto(choiceInner)) {
                merged += extractPhotoParagraphs(fallbackInner);
            }
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

    private static String extractPhotoParagraphs(String fallbackInner) {
        Matcher para = PARAGRAPH.matcher(fallbackInner);
        StringBuilder sb = new StringBuilder();
        while (para.find()) {
            String p = para.group();
            if (containsInlinePhoto(p) || p.contains("@photo")) {
                sb.append(p);
            }
        }
        return sb.toString();
    }

    private static boolean hasContent(String xml) {
        if (xml == null || xml.isBlank()) {
            return false;
        }
        if (containsInlinePhoto(xml)) {
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
