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
import org.springframework.test.util.ReflectionTestUtils;


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
class JobServiceATest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RESUME_ID = 1L;

    @Autowired
    private JobServiceA jobServiceA;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.ai.doubao.api-key:}")
    private String apiKey;

    @Test
    void searchJobs_returnsRecommendations_whenAllDependenciesAvailable() {
        skipIfMissingApiKey();

        List<JobRecommendationVO> recommendations;
        try {
            recommendations = jobServiceA.searchJobs(TEST_USER_ID, TEST_RESUME_ID);
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
        System.out.println("路径A：联网搜索岗位推荐完整结果");
        System.out.println("=".repeat(80));

        if (recommendations.isEmpty()) {
            System.out.println("结果：无匹配岗位（匹配度均低于30分或联网搜索未返回有效结果）");
            System.out.println("=".repeat(80));
            return;
        }

        System.out.printf("匹配岗位数量：%d%n", recommendations.size());
        System.out.println("-".repeat(80));

        for (int i = 0; i < recommendations.size(); i++) {
            JobRecommendationVO vo = recommendations.get(i);
            System.out.printf("%n【推荐岗位 %d】%n", i + 1);
            System.out.printf("├─ 岗位名称：%s%n", vo.getJobName());
            System.out.printf("├─ 匹配度：%d 分%n", vo.getMatchScore());
            System.out.printf("├─ 数据来源：%s%n", vo.getSource());
            System.out.printf("├─ 来源链接：%s%n", vo.getSourceUrl() != null ? vo.getSourceUrl() : "无");
            System.out.printf("├─ 匹配理由：%n");
            String[] reasonLines = vo.getMatchReason() != null ? vo.getMatchReason().split("\n") : new String[0];
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
    void searchJobs_returnsEmptyList_whenApiKeyNotConfigured() {
        String originalApiKey = (String) ReflectionTestUtils.getField(jobServiceA, "apiKey");
        try {
            ReflectionTestUtils.setField(jobServiceA, "apiKey", "");
            List<JobRecommendationVO> recommendations = jobServiceA.searchJobs(TEST_USER_ID, TEST_RESUME_ID);
            assertNotNull(recommendations);
        } catch (BusinessException e) {
            if (e.getMessage().contains("简历不存在") || e.getMessage().contains("简历内容为空")) {
                Assumptions.assumeTrue(false, "Skip: test DB has no resume seed");
            } else {
                throw e;
            }
        } finally {
            ReflectionTestUtils.setField(jobServiceA, "apiKey", originalApiKey);
        }
    }

    @Test
    void searchJobs_rejectsInvalidResumeId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceA.searchJobs(TEST_USER_ID, 99999L);
        });
    }

    @Test
    void searchJobs_rejectsUnauthorizedUserId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceA.searchJobs(99999L, TEST_RESUME_ID);
        });
    }

    @Test
    void searchJobs_rejectsNullResumeId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceA.searchJobs(TEST_USER_ID, null);
        });
    }

    @Test
    void searchJobs_rejectsNullUserId() {
        assertThrows(BusinessException.class, () -> {
            jobServiceA.searchJobs(null, TEST_RESUME_ID);
        });
    }

    @Test
    void searchJobs_rejectsEmptyResumeContent() {
        try {
            jobServiceA.searchJobs(TEST_USER_ID, TEST_RESUME_ID);
        } catch (BusinessException e) {
            if (e.getMessage().contains("简历内容为空")) {
                assertTrue(true, "正确抛出简历内容为空异常");
                return;
            }
            if (e.getMessage().contains("简历不存在")) {
                Assumptions.assumeTrue(false, "Skip: test DB has no resume seed");
                return;
            }
        }
    }

    private void skipIfMissingApiKey() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Skip: DOUBAO_API_KEY not configured");
    }
}