package com.Liang.java.ai.langchain4j.config;

import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginRequiredInterceptor loginRequiredInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    public WebMvcConfig(LoginRequiredInterceptor loginRequiredInterceptor,
                        LoginUserArgumentResolver loginUserArgumentResolver) {
        this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.loginUserArgumentResolver = loginUserArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/api/v1/appointments/**", "/api/v1/chat/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
}
