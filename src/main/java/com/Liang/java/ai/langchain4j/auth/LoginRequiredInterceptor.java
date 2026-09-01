package com.Liang.java.ai.langchain4j.auth;

import com.Liang.java.ai.langchain4j.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;

    public LoginRequiredInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "请先登录");
        }
        request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, jwtTokenService.parseToken(header.substring(7)));
        return true;
    }
}
