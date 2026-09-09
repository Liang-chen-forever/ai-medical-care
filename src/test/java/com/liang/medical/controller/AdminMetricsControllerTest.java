package com.liang.medical.controller;

import com.liang.medical.appointment.entity.AppointmentStatus;
import com.liang.medical.auth.JwtTokenService;
import com.liang.medical.auth.LoginRequiredInterceptor;
import com.liang.medical.auth.LoginUserArgumentResolver;
import com.liang.medical.auth.RoleRequiredInterceptor;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.GlobalExceptionHandler;
import com.liang.medical.config.WebMvcConfig;
import com.liang.medical.appointment.mapper.AppointmentMapper;
import com.liang.medical.mapper.TriageCaseMapper;
import com.liang.medical.mapper.WaitlistEntryMapper;
import com.liang.medical.triage.TriageCaseStatus;
import com.liang.medical.waitlist.WaitlistStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMetricsController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class,
        AdminMetricsControllerTest.JwtTestConfig.class})
class AdminMetricsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppointmentMapper appointmentMapper;

    @MockBean
    private TriageCaseMapper triageCaseMapper;

    @MockBean
    private WaitlistEntryMapper waitlistEntryMapper;

    @Test
    void patientCannotReadAdminMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/overview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(7L, "alice", UserRole.PATIENT))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(appointmentMapper, triageCaseMapper, waitlistEntryMapper);
    }

    @Test
    void adminReceivesStatusCounts() throws Exception {
        when(appointmentMapper.countByStatus(AppointmentStatus.PENDING)).thenReturn(3L);
        when(triageCaseMapper.countByStatus(TriageCaseStatus.FALLBACK.name())).thenReturn(2L);
        when(waitlistEntryMapper.countByStatus(WaitlistStatus.WAITING)).thenReturn(4L);

        mockMvc.perform(get("/api/v1/admin/metrics/overview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(1L, "admin", UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointments.PENDING").value(3))
                .andExpect(jsonPath("$.data.triageCases.FALLBACK").value(2))
                .andExpect(jsonPath("$.data.waitlistEntries.WAITING").value(4));
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
