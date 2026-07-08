package com.resume.user_identify.dto;

public class SendCodeResponse {
    private Integer expireSeconds;

    public SendCodeResponse() {}

    public SendCodeResponse(Integer expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public Integer getExpireSeconds() { return expireSeconds; }
    public void setExpireSeconds(Integer expireSeconds) { this.expireSeconds = expireSeconds; }
}
