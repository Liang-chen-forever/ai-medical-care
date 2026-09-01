package com.Liang.java.ai.langchain4j;

import com.Liang.java.ai.langchain4j.auth.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class MedicalCareApp {
    public static void main(String[] args) {
        SpringApplication.run(MedicalCareApp.class, args);
    }
}
