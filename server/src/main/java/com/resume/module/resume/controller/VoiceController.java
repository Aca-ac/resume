package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.SpeechRecognizeResultVO;
import com.resume.module.resume.dto.SpeechSynthesizeRequest;
import com.resume.module.resume.service.VoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping(value = "/asr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimiter(key = "voice-asr", maxCount = 30, duration = 60)
    public Result<SpeechRecognizeResultVO> recognize(@RequestParam("file") MultipartFile file) {
        return Result.success(voiceService.recognize(file));
    }

    @PostMapping("/tts")
    @RateLimiter(key = "voice-tts", maxCount = 30, duration = 60)
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody SpeechSynthesizeRequest body) {
        byte[] audio = voiceService.synthesize(body.getText());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }
}
