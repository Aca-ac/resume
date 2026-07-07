package com.resume.module.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.ai.AIServiceFacade;
import com.resume.common.BusinessException;
import com.resume.common.Constants;
import com.resume.common.ErrorCode;
import com.resume.module.interview.entity.InterviewMessage;
import com.resume.module.interview.entity.InterviewSession;
import com.resume.module.interview.mapper.InterviewMessageMapper;
import com.resume.module.interview.mapper.InterviewSessionMapper;
import com.resume.module.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final ResumeService resumeService;
    private final AIServiceFacade aiServiceFacade;
    @Qualifier("sseExecutor")
    private final Executor sseExecutor;

    public InterviewSession start(Long userId, Long resumeId, String jobTitle) {
        resumeService.getOwned(userId, resumeId);
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeId(resumeId);
        session.setJobTitle(jobTitle);
        session.setStatus(Constants.InterviewStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        String opening = aiServiceFacade.interviewOpening(userId, jobTitle);
        saveMessage(session.getId(), Constants.MessageRole.ASSISTANT, opening);
        return session;
    }

    public InterviewMessage answer(Long userId, Long sessionId, String answer) {
        InterviewSession session = getOwned(userId, sessionId);
        saveMessage(sessionId, Constants.MessageRole.USER, answer);
        String followUp = aiServiceFacade.interviewFollowUp(userId, session.getJobTitle(), answer);
        return saveMessage(sessionId, Constants.MessageRole.ASSISTANT, followUp);
    }

    public List<InterviewMessage> messages(Long userId, Long sessionId) {
        getOwned(userId, sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getCreatedAt));
    }

    public Page<InterviewMessage> messages(Long userId, Long sessionId, int page, int size) {
        getOwned(userId, sessionId);
        return messageMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getCreatedAt));
    }

    public SseEmitter stream(Long userId, Long sessionId) {
        getOwned(userId, sessionId);
        SseEmitter emitter = new SseEmitter(120_000L);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Runnable cancel = () -> cancelled.set(true);
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(ex -> cancelled.set(true));

        try {
            sseExecutor.execute(() -> streamMessages(userId, sessionId, emitter, cancelled));
        } catch (RejectedExecutionException ex) {
            log.warn("SSE task rejected for session {}", sessionId, ex);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    private void streamMessages(Long userId, Long sessionId, SseEmitter emitter, AtomicBoolean cancelled) {
        try {
            emitter.send(SseEmitter.event().name("retry").data("3000"));
            List<InterviewMessage> list = messages(userId, sessionId);
            for (InterviewMessage msg : list) {
                if (cancelled.get()) {
                    return;
                }
                emitter.send(SseEmitter.event().name("message").data(msg.getRole() + ": " + msg.getContent()));
            }
            if (!cancelled.get()) {
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            }
        } catch (Exception ex) {
            if (!cancelled.get()) {
                try {
                    emitter.completeWithError(ex);
                } catch (Exception completeEx) {
                    log.debug("SSE emitter already closed", completeEx);
                }
            }
        }
    }

    public InterviewSession report(Long userId, Long sessionId) {
        InterviewSession session = getOwned(userId, sessionId);
        List<InterviewMessage> list = messages(userId, sessionId);
        StringBuilder transcript = new StringBuilder();
        for (InterviewMessage m : list) {
            transcript.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
        }
        String report = aiServiceFacade.interviewReport(userId, session.getJobTitle(), transcript.toString());
        session.setReport(report);
        session.setStatus(Constants.InterviewStatus.COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return session;
    }

    private InterviewMessage saveMessage(Long sessionId, String role, String content) {
        InterviewMessage message = new InterviewMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private InterviewSession getOwned(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "session not found");
        }
        return session;
    }
}