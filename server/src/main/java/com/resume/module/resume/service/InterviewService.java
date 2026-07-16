package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.InterviewMessageVO;
import com.resume.module.resume.dto.InterviewSessionVO;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.entity.InterviewMessage;
import com.resume.module.resume.entity.InterviewSession;
import com.resume.module.resume.entity.Resume;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final String STATUS_ONGOING = "ONGOING";
    private static final String STATUS_DONE = "DONE";

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final ResumeMapper resumeMapper;
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
    public InterviewSessionVO start(Long userId, Long resumeId, String jobTitle) {
        if (resumeId == null) {
            throw new BusinessException(400, "请选择简历");
        }
        String title = (jobTitle == null || jobTitle.isBlank()) ? "通用岗位" : jobTitle.trim();
        Resume resume = requireOwnedResume(userId, resumeId);
        String resumeContent = loadSummary(resumeId);
        if (resumeContent.isBlank()) {
            throw new BusinessException(400, "简历内容为空，请先完善简历");
        }
        requireApiKey();

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeId(resume.getId());
        session.setJobTitle(title);
        session.setStatus(STATUS_ONGOING);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        String firstQuestion = chat(List.of(
                Map.of("role", "system", "content", buildQuestionPrompt(title, resumeContent, "")),
                Map.of("role", "user", "content", "请提出第一个面试问题，只输出问题本身。")
        ));
        saveMessage(session.getId(), "assistant", firstQuestion, 1);
        return toSessionVo(session);
    }

    public PageResult<InterviewMessageVO> listMessages(Long userId, Long sessionId, int page, int size) {
        requireOwnedSession(userId, sessionId);
        List<InterviewMessage> all = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getSortOrder)
                .orderByAsc(InterviewMessage::getId));
        List<InterviewMessageVO> records = all.stream().map(this::toMessageVo).toList();
        return new PageResult<>(records, records.size(), Math.max(page, 1), size <= 0 ? 200 : size);
    }

    @Transactional
    public InterviewMessageVO answer(Long userId, Long sessionId, String answer) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        if (STATUS_DONE.equals(session.getStatus())) {
            throw new BusinessException(400, "面试已结束，请查看报告或开启新面试");
        }
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(400, "回答不能为空");
        }
        requireApiKey();

        int nextOrder = nextSortOrder(sessionId);
        saveMessage(sessionId, "user", answer.trim(), nextOrder);

        List<Map<String, String>> history = new ArrayList<>();
        history.add(Map.of(
                "role", "system",
                "content", systemPrompt(session.getJobTitle(), loadSummary(session.getResumeId()))
                        + "\n请基于候选人刚才的回答继续追问一个问题，只输出问题本身，不要评价。"
        ));
        List<InterviewMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getSortOrder));
        for (InterviewMessage m : messages) {
            history.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        String nextQuestion = chat(history);
        InterviewMessage assistant = saveMessage(sessionId, "assistant", nextQuestion, nextOrder + 1);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toMessageVo(assistant);
    }

    @Transactional
    public InterviewSessionVO report(Long userId, Long sessionId) {
        InterviewSession session = requireOwnedSession(userId, sessionId);
        if (session.getReport() != null && !session.getReport().isBlank()) {
            return toSessionVo(session);
        }
        requireApiKey();

        List<InterviewMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getSortOrder));
        String transcript = messages.stream()
                .map(m -> ("user".equals(m.getRole()) ? "候选人" : "面试官") + "：" + m.getContent())
                .collect(Collectors.joining("\n"));

        String report = chat(List.of(
                Map.of("role", "system", "content", buildSummaryPrompt(session.getJobTitle(), loadSummary(session.getResumeId()), transcript)),
                Map.of("role", "user", "content", "请生成面试复盘报告。")
        ));

        session.setReport(report);
        session.setStatus(STATUS_DONE);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toSessionVo(session);
    }

    private String buildQuestionPrompt(String jobTitle, String resumeContent, String jdText) {
        return interviewPromptService.questionPrompt(Map.of(
                "jobTitle", jobTitle,
                "jdText", jdText == null ? "" : jdText,
                "resumeContent", resumeContent
        ));
    }

    private String buildSummaryPrompt(String jobTitle, String resumeContent, String transcript) {
        return interviewPromptService.summaryPrompt(Map.of(
                "jobTitle", jobTitle,
                "jdText", "",
                "resumeContent", resumeContent,
                "transcript", transcript
        ));
    }

    private String systemPrompt(String jobTitle, String resumeContent) {
        return """
                你是一位专业、简洁的中文技术面试官。
                目标职位：%s
                候选人简历：
                %s
                请围绕简历与岗位提出有针对性的问题，一次只问一个问题。
                """.formatted(jobTitle, resumeContent);
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

    private InterviewMessage saveMessage(Long sessionId, String role, String content, int sortOrder) {
        InterviewMessage msg = new InterviewMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setSortOrder(sortOrder);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }

    private int nextSortOrder(Long sessionId) {
        InterviewMessage last = messageMapper.selectOne(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByDesc(InterviewMessage::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
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

    private String loadSummary(Long resumeId) {
        return summaryStore.load(resumeId);
    }

    private InterviewSessionVO toSessionVo(InterviewSession session) {
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setJobTitle(session.getJobTitle());
        vo.setStatus(session.getStatus());
        vo.setReport(session.getReport());
        return vo;
    }

    private InterviewMessageVO toMessageVo(InterviewMessage message) {
        InterviewMessageVO vo = new InterviewMessageVO();
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        return vo;
    }
}
