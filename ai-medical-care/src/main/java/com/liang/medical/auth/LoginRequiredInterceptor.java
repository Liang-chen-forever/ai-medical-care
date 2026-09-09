package com.liang.medical.auth;

import com.liang.medical.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public LoginRequiredInterceptor(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(jwtProperties.getUserTokenName());
        if (token == null || token.isBlank()) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7).trim();
            }
        } else if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "请先登录");
        }
        request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, jwtTokenService.parseToken(token));
        return true;
    }
}
