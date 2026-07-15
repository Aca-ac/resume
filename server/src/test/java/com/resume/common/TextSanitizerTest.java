package com.resume.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSanitizerTest {

    @Test
    void forStoredContent_stripsNulAndControlChars_keepsNewlines() {
        String raw = "hello\u0000world\nnext\u0007line";
        String cleaned = TextSanitizer.forStoredContent(raw);
        assertEquals("helloworld\nnext line", cleaned);
    }

    @Test
    void forStoredContent_nullOrEmpty_returnsEmpty() {
        assertEquals("", TextSanitizer.forStoredContent(null));
        assertEquals("", TextSanitizer.forStoredContent(""));
    }

    @Test
    void forParsedText_trimsWhitespace() {
        assertEquals("abc", TextSanitizer.forParsedText("  abc  \n"));
    }

    @Test
    void forStoredContent_doesNotTrim() {
        assertEquals("  abc  ", TextSanitizer.forStoredContent("  abc  "));
    }
}
