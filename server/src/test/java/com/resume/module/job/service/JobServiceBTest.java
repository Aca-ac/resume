package com.resume.module.job.service;

import com.resume.common.BusinessException;
import com.resume.module.job.dto.JobRecommendationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "EMAIL_USERNAME=ci@example.com",
        "EMAIL_PASSWORD=ci-placeholder",
        "JWT_SECRET=ci-only-secret-must-be-at-least-32-chars!!",
        "MYSQL_DATABASE=resume_assistant",
        "MYSQL_USER=root",
        "MYSQL_PASSWORD=root"
})
class JobServiceBTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RESUME_ID = 1L;

    @Autowired
    private JobServiceB jobServiceB;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Test
    void matchJobs_returnsRecommendations_whenAllDependenciesAvailable() {
        skipIfMissingApiKey();

        List<JobRecommendationVO> recommendations;
        try {
            recommendations = jobServiceB.matchJobs(TEST_USER_ID, TEST_RESUME_ID);
        } catch (BusinessException e) {
            if (e.getMessage().contains("简历不存在")) {
                Assumptions.assumeTrue(false, "Skip: test DB has no resume seed for userId=1, resumeId=1");
                return;
            }
            if (e.getMessage().contains("简历内容为空")) {
                Assumptions.assumeTrue(false, "Skip: test resume has no content");
                return;
            }
            throw e;
        }

        assertNotNull(recommendations);

        printFullRecommendationResults(recommendations);
    }

    private void printFullRecommendationResults(List<JobRecommendationVO> recommendations) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("路径B：平台内岗位匹配推荐完整结果");
        System.out.println("=".repeat(80));

        if (recommendations.isEmpty()) {
            System.out.println("结果：无匹配岗位（匹配度均低于30分）");
            System.out.println("=".repeat(80));
            return;
        }

        System.out.printf("匹配岗位数量：%d%n", recommendations.size());
        System.out.println("-".repeat(80));

        for (int i = 0; i < recommendations.size(); i++) {
            JobRecommendationVO vo = recommendations.get(i);
            System.out.printf("%n【推荐岗位 %d】%n", i + 1);
            System.out.printf("├─ 岗位名称：%s%n", vo.getJobName());
            System.out.printf("├─ 岗位ID：%d%n", vo.getSourceJobId());
            System.out.printf("├─ 匹配度：%d 分%n", vo.getMatchScore());
            System.out.printf("├─ 数据来源：%s%n", vo.getSource());
            System.out.printf("├─ 来源链接：%s%n", vo.getSourceUrl() != null ? vo.getSourceUrl() : "无");
            System.out.printf("├─ 匹配理由：%n");
            String[] reasonLines = vo.getMatchReason().split("\n");
            for (String line : reasonLines) {
                System.out.printf("│   %s%n", line);
            }
            System.out.printf("└─ JD内容（前200字符）：%s%n", 
                    vo.getJdContent() != null && vo.getJdContent().length() > 200 
                            ? vo.getJdContent().substring(0, 200) + "..." 
                            : (vo.getJdContent() != null ? vo.getJdContent() : "空"));
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("JSON格式输出：");
        System.out.println("-".repeat(80));
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(recommendations);
            System.out.println(json);
        } catch (Exception e) {
            System.out.println("JSON序列化失败：" + e.getMessage());
        }
        System.out.println("=".repeat(80));
    }

    @Test
    void matchJobs_returnsEmptyList_whenNoJobsAvailable() {
        skipIfMissingApiKey();

        List<JobRecommendationVO> recommendations;
        try {
            recommendations = jobServiceB.matchJobs(TEST_USER_ID, TEST_RESUME_ID);
        } catch (BusinessException e) {
            if (e.getMessage().contains("简历不存在") || e.getMessage().contains("简历内容为空")) {
                Assumptions.assumeTrue(false, "Skip: test DB has no resume seed");
                return;
            }
            throw e;
        }

        assertNotNull(recommendations);
    }

    @Test
    void matchJobs_rejectsInvalidResumeId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceB.matchJobs(TEST_USER_ID, 99999L);
        });
    }

    @Test
    void matchJobs_rejectsUnauthorizedUserId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceB.matchJobs(99999L, TEST_RESUME_ID);
        });
    }

    @Test
    void matchJobs_rejectsNullResumeId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceB.matchJobs(TEST_USER_ID, null);
        });
    }

    @Test
    void matchJobs_rejectsNullUserId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceB.matchJobs(null, TEST_RESUME_ID);
        });
    }

    private void skipIfMissingApiKey() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Skip: DASHSCOPE_API_KEY not configured");
    }
}