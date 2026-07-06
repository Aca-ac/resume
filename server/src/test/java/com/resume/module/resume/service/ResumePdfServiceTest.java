package com.resume.module.resume.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResumePdfServiceTest {

    private final ResumePdfService service = new ResumePdfService();

    @Test
    void generatesValidPdfBytes() throws Exception {
        byte[] pdf = service.export("Test Resume", "Line one\nLine two");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header);
    }
}