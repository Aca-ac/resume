package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.BusinessException;
import com.resume.module.job.entity.TargetJob;
import com.resume.module.job.mapper.TargetJobMapper;
import com.resume.module.resume.dto.InterviewAnswerResultVO;
import com.resume.module.resume.dto.InterviewMessageVO;
import com.resume.module.resume.dto.InterviewSessionVO;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.entity.InterviewMessage;
import com.resume.module.resume.entity.InterviewSession;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.interview.InterviewState;
import com.resume.module.resume.mapper.InterviewMessageMapper;
import com.resume.module.resume.mapper.InterviewSessionMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_MAX_QUESTIONS = 5;

    public static final String TYPE_QUESTION = "QUESTION";
    public static final String TYPE_ANSWER = "ANSWER";
    public static final String TYPE_FEEDBACK = "FEEDBACK";
    public static final String TYPE_SYSTEM = "SYSTEM";

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final ResumeMapper resumeMapper;
    private final TargetJobMapper targetJobMapper;
    private final ResumeSummaryStore summaryStore;
    private final InterviewPromptService interviewPromptService;
    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.ai.qwen.model:qwen-plus}")
    private String model;

    @Transactional
    public InterviewSessionVO start(Long userId, Long resumeId, String jobTitle, Long jobId, Integer maxQuestions) {
        if (resumeId == null) {
            throw new BusinessException(400, "请选择简历");
        }
        Resume resume = requireOwnedResume(userId, resumeId);
        String resumeContent = summaryStore.load(resumeId);
        if (resumeContent.isBlank()) {
            throw new BusinessException(400, "简历内容为空，请先完善简历");
        }
        requireApiKey();

        String title = (jobTitle == null || jobTitle.isBlank()) ? "通用岗位" : jobTitle.trim();
        String jdText = "";
        if (jobId != null) {
            TargetJob job = targetJobMapper.selectById(jobId);
            if (job == null || !job.getUserId().equals(userId)) {
                throw new BusinessException(404, "目标岗位不存在");
            }
            if (job.getJobName() != null && !job.getJobName().isBlank()) {
                title = job.getJobName().trim();
            }
            jdText = job.getJdContent() == null ? "" : job.getJdContent();
        }

        int maxQ = normalizeMaxQuestions(maxQuestions);

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeId(resume.getId());
        session.setJobId(jobId);
        session.setJobTitle(title);
        session.setStatus(InterviewState.PREPARING.toLegacyStatus());
        session.setState(InterviewState.PREPARING.name());
        session.setQuestionIndex(0);
        session.setMaxQuestions(maxQ);
        session.setLastSeq(0);
        session.setJdSnapshot(jdText.isBlank() ? null : jdText);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        String firstQuestion = chat(List.of(
                Map.of("role", "system", "content", buildQuestionPrompt(title, resumeContent, jdText)),
                Map.of("role", "user", "content", "请提出第一个面试问题，只输出问题本身。")
        ));

        transit(session, InterviewState.QUESTIONING);
        session.setQuestionIndex(1);
        InterviewMessage q = saveMessage(session, "assistant", TYPE_QUESTION, 1, firstQuestion, null, null);
        sessionMapper.updateById(session);

        InterviewSessionVO vo = toSessionVo(session);
        vo.setFirstQuestion(toMessageVo(q));
        return vo;
    }

    public InterviewSessionVO getSession(Long userId, Long sessionId) {
        return toSessionVo(requireOwnedSession(userId, sessionId));
    }

    public PageResult<InterviewSessionVO> history(Long userId, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }
        Page<InterviewSession> result = sessionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .orderByDesc(InterviewSession::getCreatedAt)
        );
        List<InterviewSessionVO> records = result.getRecords().stream().map(this::toSessionVo).toList();
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public PageResult<InterviewMessageVO> listMessages(Long userId, Long sessionId, int page, int size, Integer afterSeq) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        LambdaQueryWrapper<InterviewMessage> q = new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getSeq)
                .orderByAsc(InterviewMessage::getId);
        if (afterSeq != null && afterSeq >= 0) {
            q.gt(InterviewMessage::getSeq, afterSeq);
        }
        List<InterviewMessage> all = messageMapper.selectList(q);
        List<InterviewMessageVO> records = all.stream().map(this::toMessageVo).toList();
        // afterSeq 场景直接全量返回切片；否则兼容旧分页语义
        if (afterSeq != null) {
            return new PageResult<>(records, records.size(), 1, Math.max(records.size(), 1));
        }
        int p = Math.max(page, 1);
        int s = size <= 0 ? 200 : size;
        int from = Math.min((p - 1) * s, records.size());
        int to = Math.min(from + s, records.size());
        return new PageResult<>(records.subList(from, to), records.size(), p, s);
    }

    @Transactional
    public InterviewAnswerResultVO answer(Long userId, Long sessionId, String answer, String clientMsgId) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        InterviewState state = InterviewState.from(session.getState());
        if (state.isTerminal()) {
            throw new BusinessException(400, "面试已结束，请查看报告或开启新面试");
        }
        if (!state.canAnswer()) {
            throw new BusinessException(409, "当前状态不可作答: " + state);
        }
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(400, "回答不能为空");
        }
        String trimmed = answer.trim();
        if (trimmed.length() > 8000) {
            throw new BusinessException(400, "回答过长");
        }

        if (clientMsgId != null && !clientMsgId.isBlank()) {
            InterviewMessage existing = messageMapper.selectOne(new LambdaQueryWrapper<InterviewMessage>()
                    .eq(InterviewMessage::getSessionId, sessionId)
                    .eq(InterviewMessage::getClientMsgId, clientMsgId.trim())
                    .last("LIMIT 1"));
            if (existing != null) {
                return rebuildAnswerResult(session, existing);
            }
        }

        requireApiKey();
        transit(session, InterviewState.EVALUATING);
        sessionMapper.updateById(session);

        int qIndex = session.getQuestionIndex() == null ? 1 : session.getQuestionIndex();
        InterviewMessage userMsg = saveMessage(session, "user", TYPE_ANSWER, qIndex, trimmed, null, clientMsgId);

        String resumeContent = summaryStore.load(session.getResumeId());
        String jd = session.getJdSnapshot() == null ? "" : session.getJdSnapshot();
        int maxQ = session.getMaxQuestions() == null ? DEFAULT_MAX_QUESTIONS : session.getMaxQuestions();
        String transcript = buildTranscript(session.getId());
        String evalRaw = chat(List.of(
                Map.of("role", "system", "content", buildEvalPrompt(
                        session.getJobTitle(), jd, resumeContent, qIndex, maxQ, transcript)),
                Map.of("role", "user", "content", "请根据对话历史输出 JSON 决策。")
        ));
        EvalDecision decision = parseEvalDecision(evalRaw);
        String feedbackText = decision.feedback() == null || decision.feedback().isBlank()
                ? shorten(evalRaw, 500)
                : decision.feedback();
        InterviewMessage feedback = saveMessage(session, "assistant", TYPE_FEEDBACK, qIndex, feedbackText, evalRaw, null);

        InterviewAnswerResultVO result = new InterviewAnswerResultVO();
        result.setUserMessage(toMessageVo(userMsg));
        result.setFeedback(toMessageVo(feedback));
        result.setMaxQuestions(maxQ);

        boolean reachMax = qIndex >= maxQ || decision.finish();
        if (reachMax) {
            finishWithSummary(session, resumeContent, jd);
            result.setFinished(true);
            result.setState(session.getState());
            result.setQuestionIndex(session.getQuestionIndex());
            result.setReport(session.getReport());
            result.setNextQuestion(null);
            return result;
        }

        String nextQ = decision.nextQuestion();
        if (nextQ == null || nextQ.isBlank()) {
            nextQ = chat(List.of(
                    Map.of("role", "system", "content", buildQuestionPrompt(session.getJobTitle(), resumeContent, jd)
                            + "\n已完成问题数：" + qIndex + "/" + maxQ
                            + "\n请继续追问一个新问题，只输出问题本身。"),
                    Map.of("role", "user", "content", "上一题回答：" + trimmed)
            ));
        }
        transit(session, InterviewState.QUESTIONING);
        session.setQuestionIndex(qIndex + 1);
        InterviewMessage next = saveMessage(session, "assistant", TYPE_QUESTION, session.getQuestionIndex(), nextQ, null, null);
        sessionMapper.updateById(session);

        result.setFinished(false);
        result.setState(session.getState());
        result.setQuestionIndex(session.getQuestionIndex());
        result.setNextQuestion(toMessageVo(next));
        return result;
    }

    /** 兼容旧前端：返回下一题消息 VO */
    @Transactional
    public InterviewMessageVO answerLegacy(Long userId, Long sessionId, String answer) {
        InterviewAnswerResultVO r = answer(userId, sessionId, answer, null);
        if (r.isFinished()) {
            InterviewMessageVO done = new InterviewMessageVO();
            done.setRole("assistant");
            done.setMessageType(TYPE_SYSTEM);
            done.setContent(r.getReport() == null ? "面试已结束，请查看报告。" : "面试已结束。\n" + shorten(r.getReport(), 500));
            done.setSeq(requireOwnedSession(userId, sessionId).getLastSeq());
            return done;
        }
        return r.getNextQuestion();
    }

    @Transactional
    public InterviewSessionVO end(Long userId, Long sessionId) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        InterviewState state = InterviewState.from(session.getState());
        if (state.isTerminal()) {
            return toSessionVo(session);
        }
        if (state == InterviewState.PREPARING) {
            transit(session, InterviewState.ABORTED);
            session.setEndedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            return toSessionVo(session);
        }
        String resumeContent = summaryStore.load(session.getResumeId());
        String jd = session.getJdSnapshot() == null ? "" : session.getJdSnapshot();
        if (state == InterviewState.QUESTIONING || state == InterviewState.EVALUATING) {
            transit(session, InterviewState.SUMMARIZING);
        }
        finishWithSummary(session, resumeContent, jd);
        return toSessionVo(session);
    }

    @Transactional
    public InterviewSessionVO report(Long userId, Long sessionId) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        if (session.getReport() != null && !session.getReport().isBlank()) {
            return toSessionVo(session);
        }
        InterviewState state = InterviewState.from(session.getState());
        if (state.canAnswer() || state == InterviewState.EVALUATING) {
            return end(userId, sessionId);
        }
        requireApiKey();
        String resumeContent = summaryStore.load(session.getResumeId());
        String jd = session.getJdSnapshot() == null ? "" : session.getJdSnapshot();
        if (!state.isTerminal() && state != InterviewState.SUMMARIZING) {
            transit(session, InterviewState.SUMMARIZING);
        }
        finishWithSummary(session, resumeContent, jd);
        return toSessionVo(session);
    }

    public List<InterviewMessageVO> messagesAfterSeq(Long userId, Long sessionId, int afterSeq) {
        return listMessages(userId, sessionId, 1, 500, afterSeq).getRecords();
    }

    /**
     * 在 QUESTIONING 状态下强制再生成一题（用于 AI 首题/追问失败后的重试）。
     * 若当前题号已达上限则直接收尾。
     */
    @Transactional
    public InterviewMessageVO generateNextQuestion(Long userId, Long sessionId) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        InterviewState state = InterviewState.from(session.getState());
        if (state.isTerminal()) {
            throw new BusinessException(400, "面试已结束");
        }
        if (state != InterviewState.QUESTIONING && state != InterviewState.PREPARING) {
            throw new BusinessException(409, "当前状态不可生成问题: " + state);
        }
        requireApiKey();
        int qIndex = session.getQuestionIndex() == null ? 0 : session.getQuestionIndex();
        int maxQ = session.getMaxQuestions() == null ? DEFAULT_MAX_QUESTIONS : session.getMaxQuestions();
        if (qIndex >= maxQ) {
            String resumeContent = summaryStore.load(session.getResumeId());
            String jd = session.getJdSnapshot() == null ? "" : session.getJdSnapshot();
            if (state == InterviewState.QUESTIONING) {
                transit(session, InterviewState.SUMMARIZING);
            }
            finishWithSummary(session, resumeContent, jd);
            throw new BusinessException(400, "已达最大问题数，面试已结束，请查看报告");
        }
        String resumeContent = summaryStore.load(session.getResumeId());
        String jd = session.getJdSnapshot() == null ? "" : session.getJdSnapshot();
        String promptExtra = qIndex == 0
                ? "请提出第一个面试问题，只输出问题本身。"
                : "已完成问题数：" + qIndex + "/" + maxQ + "。请继续提出下一个新问题，只输出问题本身。";
        String nextQ = chat(List.of(
                Map.of("role", "system", "content", buildQuestionPrompt(session.getJobTitle(), resumeContent, jd)),
                Map.of("role", "user", "content", promptExtra)
        ));
        if (state == InterviewState.PREPARING) {
            transit(session, InterviewState.QUESTIONING);
        }
        session.setQuestionIndex(qIndex + 1);
        InterviewMessage msg = saveMessage(session, "assistant", TYPE_QUESTION, session.getQuestionIndex(), nextQ, null, null);
        sessionMapper.updateById(session);
        return toMessageVo(msg);
    }

    private void finishWithSummary(InterviewSession session, String resumeContent, String jd) {
        requireApiKey();
        InterviewState cur = InterviewState.from(session.getState());
        if (cur != InterviewState.SUMMARIZING && !cur.isTerminal()) {
            if (cur == InterviewState.QUESTIONING || cur == InterviewState.EVALUATING) {
                transit(session, InterviewState.SUMMARIZING);
            } else if (cur == InterviewState.PREPARING) {
                transit(session, InterviewState.ABORTED);
                session.setEndedAt(LocalDateTime.now());
                session.setUpdatedAt(LocalDateTime.now());
                sessionMapper.updateById(session);
                return;
            }
        }
        if (InterviewState.from(session.getState()).isTerminal() && session.getReport() != null && !session.getReport().isBlank()) {
            return;
        }
        String transcript = buildTranscript(session.getId());
        String report = chat(List.of(
                Map.of("role", "system", "content", buildSummaryPrompt(session.getJobTitle(), resumeContent, jd, transcript)),
                Map.of("role", "user", "content", "请生成面试复盘报告。")
        ));
        session.setReport(report);
        InterviewState afterPrep = InterviewState.from(session.getState());
        if (afterPrep == InterviewState.SUMMARIZING) {
            transit(session, InterviewState.COMPLETED);
        } else if (!afterPrep.isTerminal()) {
            // 理论上不会走到：兜底直接标记完成
            session.setState(InterviewState.COMPLETED.name());
            session.setStatus(InterviewState.COMPLETED.toLegacyStatus());
        }
        session.setEndedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        if (afterPrep != InterviewState.COMPLETED && afterPrep != InterviewState.ABORTED) {
            saveMessage(session, "system", TYPE_SYSTEM, session.getQuestionIndex(), "面试已结束，总结已生成。", null, null);
            sessionMapper.updateById(session);
        }
    }

    private String buildTranscript(Long sessionId) {
        List<InterviewMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getSeq));
        return messages.stream()
                .map(m -> ("user".equals(m.getRole()) ? "候选人" : "面试官") + "：" + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    private EvalDecision parseEvalDecision(String raw) {
        if (raw == null || raw.isBlank()) {
            return new EvalDecision(false, "", "", null);
        }
        String json = raw.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            String action = String.valueOf(map.getOrDefault("action", "follow_up"));
            boolean finish = action.toLowerCase().contains("finish");
            String feedback = map.get("feedback") == null ? "" : String.valueOf(map.get("feedback"));
            String next = map.get("nextQuestion") == null ? "" : String.valueOf(map.get("nextQuestion"));
            Integer score = null;
            if (map.get("score") instanceof Number n) {
                score = n.intValue();
            }
            return new EvalDecision(finish, feedback, next, score);
        } catch (Exception e) {
            log.debug("Eval JSON parse fallback: {}", e.getMessage());
            return new EvalDecision(false, shorten(raw, 500), "", null);
        }
    }

    private record EvalDecision(boolean finish, String feedback, String nextQuestion, Integer score) {
    }

    private InterviewAnswerResultVO rebuildAnswerResult(InterviewSession session, InterviewMessage userMsg) {
        InterviewAnswerResultVO result = new InterviewAnswerResultVO();
        result.setUserMessage(toMessageVo(userMsg));
        result.setState(session.getState());
        result.setQuestionIndex(session.getQuestionIndex());
        result.setMaxQuestions(session.getMaxQuestions());
        result.setFinished(InterviewState.from(session.getState()).isTerminal());
        result.setReport(session.getReport());
        List<InterviewMessage> after = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, session.getId())
                .gt(InterviewMessage::getSeq, userMsg.getSeq())
                .orderByAsc(InterviewMessage::getSeq));
        for (InterviewMessage m : after) {
            if (TYPE_FEEDBACK.equals(m.getMessageType()) && result.getFeedback() == null) {
                result.setFeedback(toMessageVo(m));
            } else if (TYPE_QUESTION.equals(m.getMessageType()) && result.getNextQuestion() == null) {
                result.setNextQuestion(toMessageVo(m));
            }
        }
        return result;
    }

    private void transit(InterviewSession session, InterviewState next) {
        InterviewState cur = InterviewState.from(session.getState());
        cur.assertCanTransitTo(next);
        session.setState(next.name());
        session.setStatus(next.toLegacyStatus());
        session.setUpdatedAt(LocalDateTime.now());
    }

    private InterviewMessage saveMessage(InterviewSession session, String role, String type,
                                         Integer questionIndex, String content, String evaluation, String clientMsgId) {
        int nextSeq = (session.getLastSeq() == null ? 0 : session.getLastSeq()) + 1;
        InterviewMessage msg = new InterviewMessage();
        msg.setSessionId(session.getId());
        msg.setRole(role);
        msg.setMessageType(type);
        msg.setQuestionIndex(questionIndex);
        msg.setContent(content);
        msg.setEvaluation(evaluation);
        msg.setSeq(nextSeq);
        msg.setClientMsgId(clientMsgId == null || clientMsgId.isBlank() ? null : clientMsgId.trim());
        msg.setSortOrder(nextSeq);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        session.setLastSeq(nextSeq);
        return msg;
    }

    private int normalizeMaxQuestions(Integer maxQuestions) {
        if (maxQuestions == null) {
            return DEFAULT_MAX_QUESTIONS;
        }
        if (maxQuestions < 3 || maxQuestions > 10) {
            throw new BusinessException(400, "maxQuestions 需在 3–10 之间");
        }
        return maxQuestions;
    }

    private String buildQuestionPrompt(String jobTitle, String resumeContent, String jdText) {
        return interviewPromptService.questionPrompt(Map.of(
                "jobTitle", jobTitle,
                "jdText", jdText == null ? "" : jdText,
                "resumeContent", resumeContent
        ));
    }

    private String buildEvalPrompt(String jobTitle, String jdText, String resumeContent,
                                   int questionIndex, int maxQuestions, String transcript) {
        return interviewPromptService.answerEvalPrompt(Map.of(
                "jobTitle", jobTitle,
                "jdText", jdText == null ? "" : jdText,
                "resumeContent", resumeContent,
                "questionIndex", String.valueOf(questionIndex),
                "maxQuestions", String.valueOf(maxQuestions),
                "transcript", transcript == null ? "" : transcript
        ));
    }

    private String buildSummaryPrompt(String jobTitle, String resumeContent, String jdText, String transcript) {
        return interviewPromptService.summaryPrompt(Map.of(
                "jobTitle", jobTitle,
                "jdText", jdText == null ? "" : jdText,
                "resumeContent", resumeContent,
                "transcript", transcript
        ));
    }

    private String chat(List<Map<String, String>> messages) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages
        );
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String content = parseContent(resp);
            if (content == null || content.isBlank()) {
                throw new BusinessException(500, "AI 未返回有效内容");
            }
            return content.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Interview API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "面试 AI 服务异常：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Interview chat failed: {}", e.getMessage());
            throw new BusinessException(500, "面试对话失败：" + e.getMessage());
        }
    }

    private String parseContent(Map<String, Object> resp) {
        if (resp == null) {
            return null;
        }
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> msg)) {
            return null;
        }
        Object content = msg.get("content");
        return content == null ? null : content.toString();
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法进行模拟面试");
        }
    }

    private Resume requireOwnedResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    private InterviewSession requireOwnedSession(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(404, "面试会话不存在");
        }
        return session;
    }

    private InterviewSessionVO toSessionVo(InterviewSession session) {
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setResumeId(session.getResumeId());
        vo.setJobId(session.getJobId());
        vo.setJobTitle(session.getJobTitle());
        vo.setStatus(session.getStatus());
        vo.setState(session.getState());
        vo.setQuestionIndex(session.getQuestionIndex());
        vo.setMaxQuestions(session.getMaxQuestions());
        vo.setLastSeq(session.getLastSeq());
        vo.setReport(session.getReport());
        vo.setCreatedAt(session.getCreatedAt() == null ? null : session.getCreatedAt().format(FMT));
        vo.setEndedAt(session.getEndedAt() == null ? null : session.getEndedAt().format(FMT));
        return vo;
    }

    private InterviewMessageVO toMessageVo(InterviewMessage message) {
        InterviewMessageVO vo = new InterviewMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setMessageType(message.getMessageType());
        vo.setQuestionIndex(message.getQuestionIndex());
        vo.setContent(message.getContent());
        vo.setEvaluation(message.getEvaluation());
        vo.setSeq(message.getSeq());
        vo.setCreatedAt(message.getCreatedAt() == null ? null : message.getCreatedAt().format(FMT));
        return vo;
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
