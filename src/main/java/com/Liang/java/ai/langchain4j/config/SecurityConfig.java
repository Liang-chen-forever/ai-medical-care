package com.Liang.java.ai.langchain4j.config;

import com.Liang.java.ai.langchain4j.auth.JwtProperties;
import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties.getUserSecretKey(), properties.getUserTtl());
    }
}
