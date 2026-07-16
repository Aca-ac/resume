package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.module.job.mapper.TargetJobMapper;
import com.resume.module.resume.entity.InterviewSession;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.interview.InterviewState;
import com.resume.module.resume.mapper.InterviewMessageMapper;
import com.resume.module.resume.mapper.InterviewSessionMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewSessionMapper sessionMapper;
    @Mock private InterviewMessageMapper messageMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private TargetJobMapper targetJobMapper;
    @Mock private ResumeSummaryStore summaryStore;
    @Mock private InterviewPromptService interviewPromptService;

    @InjectMocks
    private InterviewService interviewService;

    @BeforeEach
    void setApiKey() {
        ReflectionTestUtils.setField(interviewService, "apiKey", "test-key");
        ReflectionTestUtils.setField(interviewService, "baseUrl", "http://localhost");
        ReflectionTestUtils.setField(interviewService, "model", "qwen-plus");
    }

    @Test
    void start_nullResumeId_throws400() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.start(1L, null, "Java", null, 5));
        assertEquals(400, ex.getCode());
    }

    @Test
    void start_emptyResume_throws400() {
        Resume resume = new Resume();
        resume.setId(9L);
        resume.setUserId(1L);
        when(resumeMapper.selectById(9L)).thenReturn(resume);
        when(summaryStore.load(9L)).thenReturn("  ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.start(1L, 9L, "Java", null, 5));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("简历内容为空"));
    }

    @Test
    void start_invalidMaxQuestions_throws400() {
        Resume resume = new Resume();
        resume.setId(9L);
        resume.setUserId(1L);
        when(resumeMapper.selectById(9L)).thenReturn(resume);
        when(summaryStore.load(9L)).thenReturn("有内容");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.start(1L, 9L, "Java", null, 99));
        assertEquals(400, ex.getCode());
    }

    @Test
    void answer_blank_throws400() {
        InterviewSession session = ongoingSession();
        when(sessionMapper.selectById(100L)).thenReturn(session);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.answer(1L, 100L, "  ", null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void answer_whenCompleted_throws400() {
        InterviewSession session = ongoingSession();
        session.setState(InterviewState.COMPLETED.name());
        session.setStatus("DONE");
        when(sessionMapper.selectById(100L)).thenReturn(session);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.answer(1L, 100L, "回答", null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void answer_wrongOwner_throws404() {
        InterviewSession session = ongoingSession();
        session.setUserId(99L);
        when(sessionMapper.selectById(100L)).thenReturn(session);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interviewService.answer(1L, 100L, "回答", null));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getSession_ok() {
        when(sessionMapper.selectById(100L)).thenReturn(ongoingSession());
        var vo = interviewService.getSession(1L, 100L);
        assertEquals(100L, vo.getId());
        assertEquals("QUESTIONING", vo.getState());
    }

    @Test
    void end_preparing_aborts() {
        InterviewSession session = ongoingSession();
        session.setState(InterviewState.PREPARING.name());
        when(sessionMapper.selectById(100L)).thenReturn(session);

        var vo = interviewService.end(1L, 100L);
        assertEquals("ABORTED", vo.getState());
        verify(sessionMapper).updateById(any(InterviewSession.class));
    }

    @Test
    void listMessages_afterSeq() {
        when(sessionMapper.selectById(100L)).thenReturn(ongoingSession());
        when(messageMapper.selectList(any())).thenReturn(Collections.emptyList());
        var page = interviewService.listMessages(1L, 100L, 1, 20, 3);
        assertNotNull(page.getRecords());
    }

    private static InterviewSession ongoingSession() {
        InterviewSession s = new InterviewSession();
        s.setId(100L);
        s.setUserId(1L);
        s.setResumeId(9L);
        s.setJobTitle("Java");
        s.setStatus("ONGOING");
        s.setState(InterviewState.QUESTIONING.name());
        s.setQuestionIndex(1);
        s.setMaxQuestions(5);
        s.setLastSeq(1);
        return s;
    }
}
