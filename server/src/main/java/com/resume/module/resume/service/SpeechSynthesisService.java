package com.resume.module.resume.service;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.resume.common.BusinessException;
import com.resume.config.VoiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechSynthesisService {

    private final VoiceProperties voiceProperties;

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    public byte[] synthesize(String text) {
        requireApiKey();
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, "合成文本不能为空");
        }
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model(voiceProperties.getTtsModel())
                .voice(voiceProperties.getTtsVoice())
                .build();
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        try {
            ByteBuffer audio = synthesizer.call(text.trim());
            if (audio == null || audio.remaining() == 0) {
                throw new BusinessException(500, "语音合成结果为空");
            }
            byte[] bytes = new byte[audio.remaining()];
            audio.get(bytes);
            return bytes;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("TTS failed: {}", e.getMessage());
            throw new BusinessException(500, "语音合成失败：" + e.getMessage());
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法进行语音合成");
        }
    }
}
