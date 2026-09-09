package com.liang.medical.config;

import com.liang.medical.auth.JwtProperties;
import com.liang.medical.auth.JwtTokenService;
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
