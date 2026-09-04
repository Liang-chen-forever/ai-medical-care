package com.Liang.java.ai.langchain4j.auth;

import com.Liang.java.ai.langchain4j.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleRequiredInterceptorTest {

    @Test
    void patientCannotAccessDoctorHandler() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE))
                .thenReturn(new UserPrincipal(7L, "alice", UserRole.PATIENT));

        BusinessException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> new RoleRequiredInterceptor().preHandle(request, mock(HttpServletResponse.class), doctorHandler()),
                BusinessException.class);

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo(403);
        assertThat(exception).hasMessage("无权访问该资源");
    }

    @Test
    void doctorCanAccessDoctorHandler() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE))
                .thenReturn(new UserPrincipal(17L, "doctor1", UserRole.DOCTOR));

        assertThat(new RoleRequiredInterceptor().preHandle(request, mock(HttpServletResponse.class), doctorHandler()))
                .isTrue();
    }

    private HandlerMethod doctorHandler() throws NoSuchMethodException {
        Method method = DoctorHandler.class.getDeclaredMethod("queue");
        return new HandlerMethod(new DoctorHandler(), method);
    }

    private static class DoctorHandler {

        @RequireRole(UserRole.DOCTOR)
        public void queue() {
        }
    }
}
