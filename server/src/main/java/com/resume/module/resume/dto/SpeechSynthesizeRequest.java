package com.resume.module.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SpeechSynthesizeRequest {

    @NotBlank(message = "合成文本不能为空")
    @Size(max = 2000, message = "合成文本过长")
    private String text;
}
