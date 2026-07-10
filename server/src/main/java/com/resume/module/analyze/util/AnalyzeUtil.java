package com.resume.module.analyze.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class AnalyzeUtil {

    private static final String PROMPT_TEMPLATE_PATH = "prompts/resume_analysis_prompt.txt";
    private static final String PLACEHOLDER = "【在此处粘贴简历全文】";

    @Autowired
    private DashscopeUtil dashscopeUtil;

    public String getAIResponse(String resumeContent) {
        try {
            String promptTemplate = loadPromptTemplate();
            String fullPrompt = promptTemplate.replace(PLACEHOLDER, resumeContent);
            return dashscopeUtil.singleSystemChat(fullPrompt);
        } catch (Exception e) {
            throw new RuntimeException("AI分析简历失败: " + e.getMessage(), e);
        }
    }

    private String loadPromptTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource(PROMPT_TEMPLATE_PATH);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}