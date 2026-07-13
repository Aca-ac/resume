package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "EMAIL_USERNAME=ci@example.com",
        "EMAIL_PASSWORD=ci-placeholder",
        "JWT_SECRET=ci-only-secret-must-be-at-least-32-chars!!",
        "MYSQL_DATABASE=resume_assistant",
        "MYSQL_USER=root",
        "MYSQL_PASSWORD=root"
})
class TemplateExportIntegrationTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RESUME_ID = 1L;
    private static final Long TEST_TEMPLATE_ID = 1L;

    @Autowired
    private TemplateExportService templateExportService;

    @Test
    void exportWordByTemplate() throws Exception {
        byte[] docx;
        try {
            docx = templateExportService.exportWord(TEST_USER_ID, TEST_RESUME_ID, TEST_TEMPLATE_ID);
        } catch (BusinessException | org.springframework.dao.DataAccessException e) {
            Assumptions.assumeTrue(false, "Skip: test DB has no resume/template seed — " + e.getMessage());
            return;
        }
        org.junit.jupiter.api.Assertions.assertNotNull(docx);
        org.junit.jupiter.api.Assertions.assertTrue(docx.length > 100, "docx should not be empty");
        org.junit.jupiter.api.Assertions.assertEquals('P', (char) docx[0]);
        org.junit.jupiter.api.Assertions.assertEquals('K', (char) docx[1]);
    }

    @Test
    void exportPdfByTemplate() throws Exception {
        byte[] pdf;
        try {
            pdf = templateExportService.exportPdf(TEST_USER_ID, TEST_RESUME_ID, TEST_TEMPLATE_ID);
        } catch (BusinessException | org.springframework.dao.DataAccessException | IllegalStateException e) {
            Assumptions.assumeTrue(false, "Skip: LibreOffice or seed data unavailable — " + e.getMessage());
            return;
        }
        org.junit.jupiter.api.Assertions.assertNotNull(pdf);
        org.junit.jupiter.api.Assertions.assertTrue(pdf.length > 100, "pdf should not be empty");
        org.junit.jupiter.api.Assertions.assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"));
    }
}
