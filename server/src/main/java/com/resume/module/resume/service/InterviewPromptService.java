package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sprint3 tian：加载 AI 模拟面试提示词模板，供 InterviewService / 复盘模块使用。
 * 占位符格式：{{key}}
 */
@Service
@RequiredArgsConstructor
public class InterviewPromptService {

    private static final String QUESTION = "classpath:prompts/interview_question_prompt.txt";
    private static final String ANSWER_EVAL = "classpath:prompts/interview_answer_eval_prompt.txt";
    private static final String SUMMARY = "classpath:prompts/interview_summary_prompt.txt";

    private final ResourceLoader resourceLoader;

    public String questionPrompt(Map<String, String> vars) {
        return render(QUESTION, vars);
    }

    public String answerEvalPrompt(Map<String, String> vars) {
        return render(ANSWER_EVAL, vars);
    }

    public String summaryPrompt(Map<String, String> vars) {
        return render(SUMMARY, vars);
    }

    private String render(String location, Map<String, String> vars) {
        String template = load(location);
        String result = template;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                result = result.replace("{{" + e.getKey() + "}}", nullToEmpty(e.getValue()));
            }
        }
        return result;
    }

    private String load(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new BusinessException(500, "提示词文件不存在: " + location);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(500, "读取提示词失败: " + location);
        }
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
