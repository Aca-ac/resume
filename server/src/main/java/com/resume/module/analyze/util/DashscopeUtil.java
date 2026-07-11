package com.resume.module.analyze.util;
import java.util.Arrays;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
@Component
public class DashscopeUtil {
    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;
    @Value("${app.ai.qwen.workspace-id:}")
    private String workspaceId;
    @PostConstruct
    public void init() {
        Constants.baseHttpApiUrl = String.format("https://%s.cn-beijing.maas.aliyuncs.com/api/v1", workspaceId);
    }
    public String singleSystemChat(String systemMessage) throws ApiException, NoApiKeyException, InputRequiredException {
        System.out.println("url为：" + Constants.baseHttpApiUrl);
        System.out.println("apiKey为：" + apiKey);
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemMessage)
                .build();
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                // 仅新增这一行，强制开启联网搜索，无其他改动
                .enableSearch(true)
                .build();
        GenerationResult result = gen.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }
}