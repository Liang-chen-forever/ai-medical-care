package com.liang.medical.config;

import com.liang.medical.auth.LoginRequiredInterceptor;
import com.liang.medical.auth.LoginUserArgumentResolver;
import com.liang.medical.auth.RoleRequiredInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginRequiredInterceptor loginRequiredInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final RoleRequiredInterceptor roleRequiredInterceptor;

    public WebMvcConfig(LoginRequiredInterceptor loginRequiredInterceptor,
                        LoginUserArgumentResolver loginUserArgumentResolver,
                        RoleRequiredInterceptor roleRequiredInterceptor) {
        this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.loginUserArgumentResolver = loginUserArgumentResolver;
        this.roleRequiredInterceptor = roleRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/api/v1/appointments/**", "/api/v1/chat/**", "/api/v1/doctor/**", "/api/v1/admin/**", "/api/v1/triage/**", "/api/v1/waitlist/**");
        registry.addInterceptor(roleRequiredInterceptor)
                .addPathPatterns("/api/v1/doctor/**", "/api/v1/admin/**", "/api/v1/triage/**", "/api/v1/waitlist/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
}
