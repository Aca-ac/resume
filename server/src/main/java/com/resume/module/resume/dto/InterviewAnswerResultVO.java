package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class InterviewAnswerResultVO {
    private String state;
    private Integer questionIndex;
    private Integer maxQuestions;
    private boolean finished;
    private String report;
    private InterviewMessageVO userMessage;
    private InterviewMessageVO feedback;
    private InterviewMessageVO nextQuestion;
}
