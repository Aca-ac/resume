package com.resume.module.resume.service;

import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeSummaryStoreTest {

    @Mock
    private ResumeDetailMapper resumeDetailMapper;

    @InjectMocks
    private ResumeSummaryStore store;

    @Test
    void pickCanonical_prefersNewestNotLongest() {
        ResumeDetail oldLong = row(1L, "OLD ".repeat(40), LocalDateTime.of(2026, 1, 1, 0, 0));
        ResumeDetail newShort = row(2L, "short", LocalDateTime.of(2026, 7, 14, 12, 0));
        ResumeDetail picked = ResumeSummaryStore.pickCanonical(List.of(oldLong, newShort));
        assertEquals(2L, picked.getId());
        assertEquals("short", picked.getContent());
    }

    @Test
    void pickCanonical_sameUpdatedAt_prefersLargerId() {
        LocalDateTime same = LocalDateTime.of(2026, 7, 14, 12, 0);
        ResumeDetail earlierId = row(10L, "a", same);
        ResumeDetail laterId = row(20L, "b", same);
        assertEquals(20L, ResumeSummaryStore.pickCanonical(List.of(earlierId, laterId)).getId());
    }

    @Test
    void load_empty_returnsEmptyString() {
        when(resumeDetailMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertEquals("", store.load(1L));
        assertNull(store.findCanonical(1L));
    }

    @Test
    void load_nullContent_returnsEmptyString() {
        ResumeDetail d = row(5L, null, LocalDateTime.now());
        when(resumeDetailMapper.selectList(any())).thenReturn(List.of(d));
        assertEquals("", store.load(1L));
        assertEquals(5L, store.findCanonical(1L).getId());
    }

    @Test
    void save_insertsWhenNoSummaryRows() {
        when(resumeDetailMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(resumeDetailMapper.insert(any(ResumeDetail.class))).thenAnswer(inv -> {
            ResumeDetail d = inv.getArgument(0);
            d.setId(77L);
            return 1;
        });

        store.save(9L, "hello\u0000");

        ArgumentCaptor<ResumeDetail> cap = ArgumentCaptor.forClass(ResumeDetail.class);
        verify(resumeDetailMapper).insert(cap.capture());
        assertEquals("hello", cap.getValue().getContent());
        assertEquals(ResumeSummaryStore.SECTION_SUMMARY, cap.getValue().getSectionType());
        verify(resumeDetailMapper).delete(any());
    }

    private static ResumeDetail row(Long id, String content, LocalDateTime updatedAt) {
        ResumeDetail d = new ResumeDetail();
        d.setId(id);
        d.setContent(content);
        d.setUpdatedAt(updatedAt);
        d.setSectionType(ResumeSummaryStore.SECTION_SUMMARY);
        return d;
    }
}
