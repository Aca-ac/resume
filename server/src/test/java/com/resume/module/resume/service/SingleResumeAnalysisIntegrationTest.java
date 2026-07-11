package com.resume.module.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.module.analyze.util.AnalyzeUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用测试专用简历文件夹中的真实简历，验证 AI 分析与加权评分。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SingleResumeAnalysisIntegrationTest {

    private static final Map<String, Double> WEIGHTS = Map.of(
            "summary", 0.10,
            "education", 0.15,
            "experience", 0.25,
            "skill", 0.25,
            "project", 0.25
    );

    @Autowired
    private AnalyzeUtil analyzeUtil;

    @Autowired
    private FileParseService fileParseService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${dashscope-maas.api-key:}")
    private String maasApiKey;

    @Test
    void analyzeResume1_withRealAi() throws Exception {
        Assumptions.assumeTrue(maasApiKey != null && !maasApiKey.isBlank(),
                "未配置 dashscope-maas.api-key，跳过真实 AI 调用");

        Path resumePath = Path.of("..", "测试专用简历", "简历1.docx").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.exists(resumePath), "找不到测试简历: " + resumePath);

        String resumeText = fileParseService.parse(resumePath, "DOCX");
        assertFalse(resumeText.isBlank(), "简历解析结果为空");
        System.out.println("=== 简历1 解析长度: " + resumeText.length() + " 字符 ===");
        System.out.println(resumeText.substring(0, Math.min(300, resumeText.length())).replace('\n', ' ') + "...");

        System.out.println("\n=== 调用 AI 分析（约 15-90 秒）===");
        String aiResponse = analyzeUtil.getAIResponse(resumeText);
        assertNotNull(aiResponse);
        assertFalse(aiResponse.isBlank());

        JsonNode parsed = analysisService.parseAiJson(aiResponse);
        Map<String, Integer> scores = extractScores(parsed);
        int totalScore = calculateTotalScore(scores);
        String suggestions = parsed.path("suggestions").asText("");

        System.out.println("\n=== 多维度评分 ===");
        scores.forEach((dim, score) ->
                System.out.printf("  %-12s %3d  (权重 %.0f%%)%n", dim, score, WEIGHTS.get(dim) * 100));
        System.out.println("  综合加权总分: " + totalScore);

        System.out.println("\n=== 优化建议（前 500 字）===");
        System.out.println(suggestions.substring(0, Math.min(500, suggestions.length())) + "...");

        for (int score : scores.values()) {
            assertTrue(score >= 0 && score <= 100, "分数应在 0-100");
        }
        assertTrue(suggestions.length() >= 200, "建议内容过短，可能未正常生成");
        assertTrue(totalScore >= 0 && totalScore <= 100, "总分应在 0-100");
    }

    private Map<String, Integer> extractScores(JsonNode parsed) {
        JsonNode scoresNode = parsed.has("scores") ? parsed.get("scores") : parsed;
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String key : WEIGHTS.keySet()) {
            JsonNode value = scoresNode.get(key);
            if (value == null || !value.isInt()) {
                value = parsed.get(key + "_score");
            }
            assertNotNull(value, "缺少维度: " + key);
            scores.put(key, value.asInt());
        }
        return scores;
    }

    private int calculateTotalScore(Map<String, Integer> scores) {
        double total = 0;
        for (Map.Entry<String, Double> entry : WEIGHTS.entrySet()) {
            total += scores.get(entry.getKey()) * entry.getValue();
        }
        return (int) Math.round(total);
    }
}
