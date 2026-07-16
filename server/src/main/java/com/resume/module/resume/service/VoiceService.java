package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.config.VoiceProperties;
import com.resume.module.resume.dto.SpeechRecognizeResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final VoiceProperties voiceProperties;
    private final VoiceStorageService voiceStorageService;
    private final SpeechRecognitionService speechRecognitionService;
    private final SpeechSynthesisService speechSynthesisService;

    public SpeechRecognizeResultVO recognize(MultipartFile file) {
        try {
            VoiceStorageService.StoredVoice stored = voiceStorageService.store(file);
            String text = withRetry(() -> speechRecognitionService.recognize(stored.path(), stored.extension()));
            SpeechRecognizeResultVO vo = new SpeechRecognizeResultVO();
            vo.setText(text);
            vo.setFileId(stored.storedName());
            vo.setOriginalFilename(stored.originalName());
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Voice recognize pipeline failed: {}", e.getMessage());
            throw new BusinessException(500, "语音识别处理失败：" + e.getMessage());
        }
    }

    public byte[] synthesize(String text) {
        return withRetry(() -> speechSynthesisService.synthesize(text));
    }

    private <T> T withRetry(Supplier<T> action) {
        int attempts = voiceProperties.getMaxRetries() + 1;
        BusinessException lastBiz = null;
        RuntimeException lastOther = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return action.get();
            } catch (BusinessException e) {
                lastBiz = e;
                if (e.getCode() >= 400 && e.getCode() < 500) {
                    throw e;
                }
                sleepBeforeRetry(i, attempts);
            } catch (RuntimeException e) {
                lastOther = e;
                sleepBeforeRetry(i, attempts);
            }
        }
        if (lastBiz != null) {
            throw lastBiz;
        }
        if (lastOther != null) {
            throw lastOther;
        }
        throw new BusinessException(500, "语音服务调用失败");
    }

    private void sleepBeforeRetry(int attemptIndex, int attempts) {
        if (attemptIndex >= attempts - 1) {
            return;
        }
        try {
            Thread.sleep(voiceProperties.getRetryDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
