package com.liang.medical.controller;

import com.liang.medical.appointment.AppointmentStatus;
import com.liang.medical.auth.JwtTokenService;
import com.liang.medical.auth.LoginRequiredInterceptor;
import com.liang.medical.auth.LoginUserArgumentResolver;
import com.liang.medical.auth.RoleRequiredInterceptor;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.GlobalExceptionHandler;
import com.liang.medical.config.WebMvcConfig;
import com.liang.medical.dto.doctor.DoctorAppointmentResponse;
import com.liang.medical.service.AppointmentBookingService;
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

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorAppointmentController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class,
        DoctorAppointmentControllerTest.JwtTestConfig.class})
class DoctorAppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppointmentBookingService bookingService;

    @Test
    void doctorConfirmationUsesAuthenticatedDoctorId() throws Exception {
        mockMvc.perform(post("/api/v1/doctor/appointments/55/confirm")
                        .header(HttpHeaders.AUTHORIZATION, doctorBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(bookingService).confirmByDoctor(17L, 55L);
    }

    @Test
    void patientIsForbiddenBeforeDoctorControllerRuns() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/appointments")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权访问该资源"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void rejectionRequiresNonBlankReason() throws Exception {
        mockMvc.perform(post("/api/v1/doctor/appointments/55/reject")
                        .header(HttpHeaders.AUTHORIZATION, doctorBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("拒绝原因不能为空"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void doctorQueueUsesAuthenticatedDoctorAndReturnsSafeSnapshot() throws Exception {
        when(bookingService.listForDoctor(17L, AppointmentStatus.PENDING)).thenReturn(List.of(
                new DoctorAppointmentResponse(55L, 101L, "内科", "2026-09-04", "09:00-10:00",
                        "王医生", AppointmentStatus.PENDING, null)));

        mockMvc.perform(get("/api/v1/doctor/appointments")
                        .param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, doctorBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(55))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].idCard").doesNotExist())
                .andExpect(jsonPath("$.data[0].phone").doesNotExist());

        verify(bookingService).listForDoctor(17L, AppointmentStatus.PENDING);
    }

    private String doctorBearer() {
        return bearerFor(new UserPrincipal(17L, "doctor1", UserRole.DOCTOR));
    }

    private String patientBearer() {
        return bearerFor(new UserPrincipal(7L, "alice", UserRole.PATIENT));
    }

    private String bearerFor(UserPrincipal user) {
        return "Bearer " + jwtTokenService.createToken(user);
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtTokenService jwtTokenService() {
            return new JwtTokenService("01234567890123456789012345678901", 3600);
        }
    }
}
