package com.resume.module.job.service;

import com.resume.common.BusinessException;
import com.resume.module.job.dto.CommentCreateRequest;
import com.resume.module.job.dto.JobCreateRequest;
import com.resume.module.job.dto.JobVO;
import com.resume.module.job.entity.TargetJob;
import com.resume.module.job.mapper.JobCommentMapper;
import com.resume.module.job.mapper.TargetJobMapper;
import com.resume.user_identify.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private TargetJobMapper targetJobMapper;
    @Mock
    private JobCommentMapper jobCommentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private JobService jobService;

    @Test
    void createJob_blankName_throws400() {
        JobCreateRequest req = new JobCreateRequest();
        req.setJobName("  ");
        BusinessException ex = assertThrows(BusinessException.class, () -> jobService.createJob(1L, req));
        assertEquals(400, ex.getCode());
    }

    @Test
    void forkJob_ownJob_throws400() {
        TargetJob source = new TargetJob();
        source.setId(9L);
        source.setUserId(1L);
        source.setJobName("后端");
        when(targetJobMapper.selectById(9L)).thenReturn(source);

        BusinessException ex = assertThrows(BusinessException.class, () -> jobService.forkJob(1L, 9L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能fork"));
    }

    @Test
    void forkJob_otherUser_insertsForkCopy() {
        TargetJob source = new TargetJob();
        source.setId(9L);
        source.setUserId(2L);
        source.setJobName("后端");
        source.setJdContent("JD");
        when(targetJobMapper.selectById(9L)).thenReturn(source);
        when(targetJobMapper.insert(any(TargetJob.class))).thenAnswer(inv -> {
            TargetJob j = inv.getArgument(0);
            j.setId(100L);
            return 1;
        });

        JobVO vo = jobService.forkJob(1L, 9L);
        assertEquals(100L, vo.getId());
        assertEquals(Integer.valueOf(2), vo.getSource());

        ArgumentCaptor<TargetJob> cap = ArgumentCaptor.forClass(TargetJob.class);
        verify(targetJobMapper).insert(cap.capture());
        assertEquals(1L, cap.getValue().getUserId());
        assertEquals(9L, cap.getValue().getOriginalJobId());
    }

    @Test
    void createComment_blank_throws400() {
        TargetJob job = new TargetJob();
        job.setId(1L);
        job.setUserId(2L);
        when(targetJobMapper.selectById(1L)).thenReturn(job);

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent("   ");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jobService.createComment(1L, 1L, req));
        assertEquals(400, ex.getCode());
    }

    @Test
    void updateJob_otherOwner_throws403() {
        TargetJob job = new TargetJob();
        job.setId(1L);
        job.setUserId(2L);
        when(targetJobMapper.selectById(1L)).thenReturn(job);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> jobService.updateJob(1L, 1L, new com.resume.module.job.dto.JobUpdateRequest()));
        assertEquals(403, ex.getCode());
    }

    @Test
    void getJob_missing_throws404() {
        when(targetJobMapper.selectById(8L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> jobService.getJob(1L, 8L)).getCode());
    }

    @Test
    void deleteJob_otherOwner_throws403() {
        TargetJob job = new TargetJob();
        job.setId(1L);
        job.setUserId(2L);
        when(targetJobMapper.selectById(1L)).thenReturn(job);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> jobService.deleteJob(1L, 1L)).getCode());
    }

    @Test
    void createComment_tooLong_throws400() {
        TargetJob job = new TargetJob();
        job.setId(1L);
        job.setUserId(2L);
        when(targetJobMapper.selectById(1L)).thenReturn(job);
        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent("x".repeat(5001));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> jobService.createComment(1L, 1L, req)).getCode());
    }

    @Test
    void searchCommunity_blankKeyword_throws400() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> jobService.searchCommunity("  ", 1, 10)).getCode());
    }

    @Test
    void createJob_happyPath_setsManualSource() {
        JobCreateRequest req = new JobCreateRequest();
        req.setJobName("  Java ");
        req.setJdContent("jd");
        when(targetJobMapper.insert(any(TargetJob.class))).thenAnswer(inv -> {
            TargetJob j = inv.getArgument(0);
            j.setId(50L);
            return 1;
        });
        JobVO vo = jobService.createJob(1L, req);
        assertEquals(50L, vo.getId());
        assertEquals("Java", vo.getJobName());
        assertEquals(Integer.valueOf(0), vo.getSource());
    }
}
