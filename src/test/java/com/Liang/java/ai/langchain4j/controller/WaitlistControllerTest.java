package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import com.Liang.java.ai.langchain4j.auth.RoleRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.GlobalExceptionHandler;
import com.Liang.java.ai.langchain4j.config.WebMvcConfig;
import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.entity.WaitlistEntry;
import com.Liang.java.ai.langchain4j.service.WaitlistService;
import com.Liang.java.ai.langchain4j.waitlist.WaitlistStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaitlistController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class, WaitlistControllerTest.JwtTestConfig.class})
class WaitlistControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenService jwtTokenService;
    @MockBean
    private WaitlistService waitlistService;

    @Test
    void patientCanJoinAndControllerDerivesPatientIdFromJwt() throws Exception {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(55L);
        entry.setScheduleId(101L);
        entry.setStatus(WaitlistStatus.WAITING);
        entry.setPriority(20);
        entry.setCreatedAt(LocalDateTime.of(2026, 9, 5, 12, 0));
        org.mockito.Mockito.when(waitlistService.join(7L, 101L, null)).thenReturn(entry);

        mockMvc.perform(post("/api/v1/waitlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(7L, "patient", UserRole.PATIENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\":101}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(55))
                .andExpect(jsonPath("$.data.patientId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        verify(waitlistService).join(7L, 101L, null);
    }

    @Test
    void doctorCannotUsePatientWaitlistEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/waitlist/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(17L, "doctor", UserRole.DOCTOR))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(waitlistService);
    }

    @Test
    void invalidJoinRequestIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/v1/waitlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(7L, "patient", UserRole.PATIENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(waitlistService);
    }

    private String bearer(UserPrincipal principal) {
        return "Bearer " + jwtTokenService.createToken(principal);
    }

    @TestConfiguration
    static class JwtTestConfig {
        @Bean
        JwtTokenService jwtTokenService() {
            return new JwtTokenService("01234567890123456789012345678901", 3600);
        }
    }
}
