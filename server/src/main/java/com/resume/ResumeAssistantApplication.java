package com.resume;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.resume"})
public class ResumeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAssistantApplication.class, args);
    }
}
