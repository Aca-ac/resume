package com.resume;

import com.resume.config.AppProperties;
import com.resume.config.LLMConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.resume.module.**.mapper")
@EnableConfigurationProperties({LLMConfig.class, AppProperties.class})
public class ResumeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAssistantApplication.class, args);
    }
}
