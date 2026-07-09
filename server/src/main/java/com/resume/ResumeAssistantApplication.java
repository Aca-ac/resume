package com.resume;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.resume.**.mapper")
@SpringBootApplication
<<<<<<< HEAD
@MapperScan("com.resume.**.mapper")
=======
@ComponentScan(basePackages = {"com.resume"})
>>>>>>> origin/main
public class ResumeAssistantApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "dev");
        SpringApplication.run(ResumeAssistantApplication.class, args);
    }
}