package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import com.Liang.java.ai.langchain4j.auth.RoleRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.GlobalExceptionHandler;
import com.Liang.java.ai.langchain4j.config.WebMvcConfig;
import com.Liang.java.ai.langchain4j.entity.Encounter;
import com.Liang.java.ai.langchain4j.service.EncounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EncounterController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class, EncounterControllerTest.JwtTestConfig.class})
class EncounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private EncounterService encounterService;

    @Test
    void doctorCompletesAppointmentWithValidatedSummary() throws Exception {
        Encounter encounter = new Encounter();
        encounter.setAppointmentId(55L);
        encounter.setSummary("完成问诊摘要");
        when(encounterService.complete(17L, 55L, "完成问诊摘要", "一周后复诊")).thenReturn(encounter);

        mockMvc.perform(post("/api/v1/doctor/appointments/55/encounter")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(17L, "doctor1", UserRole.DOCTOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"完成问诊摘要\",\"followUpAdvice\":\"一周后复诊\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.appointmentId").value(55))
                .andExpect(jsonPath("$.data.summary").value("完成问诊摘要"));

        verify(encounterService).complete(17L, 55L, "完成问诊摘要", "一周后复诊");
    }

    @Test
    void encounterSummaryRejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/v1/doctor/appointments/55/encounter")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(17L, "doctor1", UserRole.DOCTOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("就诊摘要不能为空"));
    }

    @Test
    void patientReadsEncounterUsingAuthenticatedPatientId() throws Exception {
        Encounter encounter = new Encounter();
        encounter.setAppointmentId(55L);
        encounter.setPatientId(7L);
        encounter.setSummary("已完成摘要");
        when(encounterService.getForUser(7L, UserRole.PATIENT, 55L)).thenReturn(encounter);

        mockMvc.perform(get("/api/v1/appointments/55/encounter")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(7L, "alice", UserRole.PATIENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientId").value(7))
                .andExpect(jsonPath("$.data.summary").value("已完成摘要"));

        verify(encounterService).getForUser(7L, UserRole.PATIENT, 55L);
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
