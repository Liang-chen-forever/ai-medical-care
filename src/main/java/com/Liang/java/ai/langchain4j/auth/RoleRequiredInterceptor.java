package com.Liang.java.ai.langchain4j.auth;

import com.Liang.java.ai.langchain4j.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class RoleRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireRole requirement = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireRole.class);
        if (requirement == null) {
            requirement = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requirement == null) {
            return true;
        }
        Object principal = request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE);
        if (!(principal instanceof UserPrincipal user)
                || Arrays.stream(requirement.value()).noneMatch(role -> role == user.role())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权访问该资源");
        }
        return true;
    }
}
