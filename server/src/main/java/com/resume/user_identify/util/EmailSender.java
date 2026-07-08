package com.resume.user_identify.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {
    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.smtp.username}")
    private String fromEmail;
    
    /**
     * 发送简单文本邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // 发件人邮箱
            message.setTo(to); // 收件人邮箱
            message.setSubject(subject); // 邮件主题
            message.setText(content); // 邮件内容
            
            mailSender.send(message);
            System.out.println("邮件发送成功！收件人：" + to);
        } catch (Exception e) {
            System.err.println("邮件发送失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送验证码邮件
     * @param to 收件人邮箱
     * @param verificationCode 验证码
     */
    public void sendVerificationCode(String to, String verificationCode) {
        String subject = "职通车AI简历与求职助手验证码";
        String content = "你的验证码为" + verificationCode + "，有效期5分钟，请尽快使用";
        sendSimpleEmail(to, subject, content);
    }
}