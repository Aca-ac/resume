package com.resume.module.analyze.test;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class MaasCallProofTest {

    @Value("${dashscope-maas.api-key:}")
    private String maasApiKey;

    @Test
    void printMaasCallProof() throws Exception {
        Assumptions.assumeTrue(maasApiKey != null && !maasApiKey.isBlank(),
                "未配置 dashscope-maas.api-key，跳过真实 AI 调用");

        String endpoint = "https://ws-ai0lwpilsl2yp798.cn-beijing.maas.aliyuncs.com/api/v1";
        Constants.baseHttpApiUrl = endpoint;

        System.out.println("=== 阿里云调用凭证 ===");
        System.out.println("endpoint: " + endpoint);
        System.out.println("model: qwen-plus");
        System.out.println("api-key prefix: " + mask(maasApiKey));
        System.out.println("env DASHSCOPE_API_KEY prefix: " + mask(System.getenv("DASHSCOPE_API_KEY")));

        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .apiKey(maasApiKey)
                .model("qwen-plus")
                .messages(List.of(Message.builder().role(Role.USER.getValue()).content("只回复OK").build()))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = gen.call(param);
        System.out.println("requestId: " + result.getRequestId());
        if (result.getUsage() != null) {
            System.out.println("usage input_tokens: " + result.getUsage().getInputTokens());
            System.out.println("usage output_tokens: " + result.getUsage().getOutputTokens());
        }
        System.out.println("response: " + result.getOutput().getChoices().get(0).getMessage().getContent());
        System.out.println("=== 调用成功，可在 MaaS 工作空间用量/日志中用 requestId 检索 ===");
    }

    private static String mask(String key) {
        if (key == null || key.isBlank()) return "(empty)";
        return key.substring(0, Math.min(12, key.length())) + "...";
    }
}
