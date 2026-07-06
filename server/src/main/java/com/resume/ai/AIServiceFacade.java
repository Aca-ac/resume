package com.resume.ai;

import com.resume.common.BusinessException;
import com.resume.common.Constants;
import com.resume.common.ErrorCode;
import com.resume.config.LLMConfig;
import com.resume.module.ai.entity.AiCallLog;
import com.resume.module.ai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceFacade {

    private final LLMConfig llmConfig;
    private final PromptEngine promptEngine;
    private final List<LLMProvider> providers;
    private final AiCallLogMapper aiCallLogMapper;

    public String optimizeResume(Long userId, String resumeContent, String targetRole) {
        Map<String, String> vars = Map.of(
                "resume", resumeContent,
                "targetRole", targetRole == null ? "General" : targetRole);
        String system = promptEngine.renderSystem("resume-optimize", vars);
        String user = promptEngine.renderUser("resume-optimize", vars);
        return call(userId, "resume-optimize", system, user);
    }

    public Map<String, Object> matchJob(Long userId, String resumeContent, String jdText) {
        Map<String, String> vars = Map.of("resume", resumeContent, "jd", jdText);
        String system = promptEngine.renderSystem("job-match", vars);
        String user = promptEngine.renderUser("job-match", vars);
        String raw = call(userId, "job-match", system, user);
        Map<String, Object> result = new HashMap<>();
        Integer score = ScoreParser.parse(raw);
        result.put("score", score == null ? 0 : score);
        result.put("scoreAvailable", score != null);
        result.put("analysis", raw);
        return result;
    }

    public String interviewOpening(Long userId, String jobTitle) {
        Map<String, String> vars = Map.of("jobTitle", jobTitle);
        return call(userId, "interview", promptEngine.renderSystem("interview", vars),
                promptEngine.renderUser("interview-opening", vars));
    }

    public String interviewFollowUp(Long userId, String jobTitle, String answer) {
        Map<String, String> vars = Map.of("jobTitle", jobTitle, "answer", answer);
        return call(userId, "interview", promptEngine.renderSystem("interview", vars),
                promptEngine.renderUser("interview-followup", vars));
    }

    public String interviewReport(Long userId, String jobTitle, String transcript) {
        Map<String, String> vars = Map.of("jobTitle", jobTitle, "transcript", transcript);
        return call(userId, "interview-report", promptEngine.renderSystem("interview-report", vars),
                promptEngine.renderUser("interview-report", vars));
    }

    private String call(Long userId, String scene, String system, String user) {
        LLMProvider provider = resolveProvider();
        String response = null;
        boolean success = false;
        try {
            response = provider.chat(system, user);
            success = true;
            return response;
        } catch (Exception ex) {
            log.error("AI call failed scene={} provider={}", scene, provider.name(), ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI service unavailable");
        } finally {
            logCall(userId, provider.name(), scene, user, response, success);
        }
    }

    private LLMProvider resolveProvider() {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No LLM provider configured");
        }
        String name = llmConfig.getProvider() == null ? Constants.AiProvider.OPENAI : llmConfig.getProvider().toLowerCase(Locale.ROOT);
        return providers.stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("LLM provider not found: " + name));
    }

    private void logCall(Long userId, String provider, String scene, String request, String response, boolean success) {
        AiCallLog logEntry = new AiCallLog();
        logEntry.setUserId(userId);
        logEntry.setProvider(provider);
        logEntry.setScene(scene);
        logEntry.setRequestSummary(trim(request, 500));
        logEntry.setResponseSummary(success ? trim(response, 500) : "[FAILED]");
        logEntry.setCreatedAt(LocalDateTime.now());
        try {
            aiCallLogMapper.insert(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to persist AI call log", ex);
        }
    }

    private String trim(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}