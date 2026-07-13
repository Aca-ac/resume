package com.resume.module.resume.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false"
})
class TemplateExportIntegrationTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RESUME_ID = 1L;
    private static final Long TEST_TEMPLATE_ID = 1L;

    @Autowired
    private TemplateExportService templateExportService;

    @Test
    void exportWordByTemplate() throws Exception {
        byte[] docx = templateExportService.exportWord(TEST_USER_ID, TEST_RESUME_ID, TEST_TEMPLATE_ID);
        Assertions.assertNotNull(docx);
        Assertions.assertTrue(docx.length > 1000, "docx should not be empty");
        Assertions.assertEquals('P', (char) docx[0]);
        Assertions.assertEquals('K', (char) docx[1]);
    }

    @Test
    void exportPdfByTemplate() throws Exception {
        byte[] pdf = templateExportService.exportPdf(TEST_USER_ID, TEST_RESUME_ID, TEST_TEMPLATE_ID);
        Assertions.assertNotNull(pdf);
        Assertions.assertTrue(pdf.length > 1000, "pdf should not be empty");
        Assertions.assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"));
    }
}
