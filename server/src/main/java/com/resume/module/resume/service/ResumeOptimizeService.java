package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResumeOptimizeService {

    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.ai.qwen.model:qwen-plus}")
    private String model;

    public String optimize(String resumeContent, String targetRole) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法进行 AI 优化");
        }
        String role = (targetRole == null || targetRole.isBlank()) ? "通用岗位" : targetRole.trim();
        String content = resumeContent == null ? "" : resumeContent.trim();
        if (content.isBlank()) {
            throw new BusinessException(400, "简历内容为空，请先编辑或导入简历");
        }

        String prompt = """
                你是资深中文简历优化助手。请根据目标职位优化下面的简历正文。
                要求：
                1. 保留真实经历，不要编造公司、学校、时间；
                2. 突出与目标职位相关的技能、成果与关键词；
                3. 优化表达，使条理更清晰，适当量化成果；
                4. 只输出优化后的完整简历正文，不要解释、不要 Markdown 标题装饰。
                
                目标职位：%s
                
                原始简历：
                %s
                """.formatted(role, content);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                ))
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

            String optimized = parseContent(resp);
            if (optimized == null || optimized.isBlank()) {
                throw new BusinessException(500, "AI 未返回有效内容，请稍后重试");
            }
            return optimized.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Optimize API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "AI 优化服务异常：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Optimize failed: {}", e.getMessage());
            throw new BusinessException(500, "AI 优化失败：" + e.getMessage());
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
}
