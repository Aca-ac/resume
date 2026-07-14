package com.resume.common;

/**
 * Strip NUL / C0 control characters that can truncate MySQL TEXT mid-string
 * and make resume body appear shorter than what the editor sent.
 */
public final class TextSanitizer {

    private TextSanitizer() {
    }

    /** Keep user-edited content as-is (no trim). */
    public static String forStoredContent(String text) {
        return sanitize(text, false);
    }

    /** Parsed / OCR text: also trim leading/trailing whitespace. */
    public static String forParsedText(String text) {
        return sanitize(text, true);
    }

    private static String sanitize(String text, boolean trim) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == 0) {
                continue;
            }
            if (cp == '\n' || cp == '\r' || cp == '\t') {
                sb.appendCodePoint(cp);
                continue;
            }
            if (cp < 32 || (cp >= 0x7F && cp <= 0x9F)) {
                sb.append(' ');
                continue;
            }
            sb.appendCodePoint(cp);
        }
        String result = sb.toString();
        return trim ? result.trim() : result;
    }
}
