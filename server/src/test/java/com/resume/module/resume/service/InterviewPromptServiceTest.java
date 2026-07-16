package com.resume.module.resume.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewPromptServiceTest {

    private final InterviewPromptService service =
            new InterviewPromptService(new DefaultResourceLoader());

    @Test
    void questionPrompt_replacesPlaceholders() {
        String prompt = service.questionPrompt(Map.of(
                "jobTitle", "Java开发",
                "jdText", "负责后端开发",
                "resumeContent", "有 Spring 经验"
        ));
        assertTrue(prompt.contains("Java开发"));
        assertTrue(prompt.contains("负责后端开发"));
        assertFalse(prompt.contains("{{jobTitle}}"));
    }

    @Test
    void summaryPrompt_loadsTemplate() {
        String prompt = service.summaryPrompt(Map.of(
                "jobTitle", "测试",
                "jdText", "",
                "resumeContent", "简历",
                "transcript", "问答"
        ));
        assertTrue(prompt.contains("## 综合评价"));
        assertTrue(prompt.contains("问答"));
    }
}
