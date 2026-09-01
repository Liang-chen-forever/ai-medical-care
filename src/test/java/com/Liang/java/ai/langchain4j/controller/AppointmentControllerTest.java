package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.common.GlobalExceptionHandler;
import com.Liang.java.ai.langchain4j.config.WebMvcConfig;
import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.service.AppointmentBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, AppointmentControllerTest.JwtTestConfig.class})
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppointmentBookingService appointmentBookingService;

    @Test
    void requiresAuthenticationBeforeListingAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void bookingUsesTheUserIdFromBearerToken() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setId(9L);
        appointment.setUserId(7L);
        appointment.setScheduleId(101L);
        when(appointmentBookingService.book(7L, 101L)).thenReturn(appointment);

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearerForUser7())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\":101}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.scheduleId").value(101));

        verify(appointmentBookingService).book(7L, 101L);
    }

    @Test
    void bookingRejectsARequestWithoutScheduleId() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearerForUser7())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("scheduleId不能为空"));
    }

    @Test
    void cancellationUsesTheRestfulAppointmentPath() throws Exception {
        mockMvc.perform(delete("/api/v1/appointments/55")
                        .header(HttpHeaders.AUTHORIZATION, bearerForUser7()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(appointmentBookingService).cancel(7L, 55L);
    }

    private String bearerForUser7() {
        return "Bearer " + jwtTokenService.createToken(new UserPrincipal(7L, "alice"));
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtTokenService jwtTokenService() {
            return new JwtTokenService("01234567890123456789012345678901", 3600);
        }
    }
}
