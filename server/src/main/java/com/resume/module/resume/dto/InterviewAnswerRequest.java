package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class InterviewAnswerRequest {
    private String answer;
    /** 客户端幂等 ID，断线重发时使用 */
    private String clientMsgId;
}
