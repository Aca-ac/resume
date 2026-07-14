package com.resume.module.resume.service;

import com.resume.module.resume.entity.ResumeDetail;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumeSummaryStoreTest {

    @Test
    void pickCanonical_prefersNewestNotLongest() {
        ResumeDetail oldLong = new ResumeDetail();
        oldLong.setId(1L);
        oldLong.setContent("OLD ".repeat(40));
        oldLong.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        ResumeDetail newShort = new ResumeDetail();
        newShort.setId(2L);
        newShort.setContent("short");
        newShort.setUpdatedAt(LocalDateTime.of(2026, 7, 14, 12, 0));

        ResumeDetail picked = ResumeSummaryStore.pickCanonical(List.of(oldLong, newShort));
        assertEquals(2L, picked.getId());
        assertEquals("short", picked.getContent());
    }
}