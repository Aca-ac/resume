package com.resume;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.resume.**.mapper")
@SpringBootApplication
@ComponentScan(basePackages = {"com.resume"})
@EnableScheduling
public class ResumeAssistantApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "dev");
        SpringApplication.run(ResumeAssistantApplication.class, args);
    }
}
