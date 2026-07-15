package com.resume.module.resume.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResumePhotoServiceTest {

    @Test
    void photoUrl_blankPath_returnsNull() {
        assertNull(ResumePhotoService.photoUrl(1L, null));
        assertNull(ResumePhotoService.photoUrl(1L, "  "));
        assertNull(ResumePhotoService.photoUrl(null, "a.jpg"));
    }

    @Test
    void photoUrl_keepsApiPrefixForBrowserImgSrc() {
        assertEquals("/api/v1/resumes/12/photo", ResumePhotoService.photoUrl(12L, "uploads/a.jpg"));
    }
}
