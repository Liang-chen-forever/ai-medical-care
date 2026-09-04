package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import com.Liang.java.ai.langchain4j.auth.RoleRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.GlobalExceptionHandler;
import com.Liang.java.ai.langchain4j.config.WebMvcConfig;
import com.Liang.java.ai.langchain4j.dto.triage.TriageCaseResponse;
import com.Liang.java.ai.langchain4j.dto.triage.TriageCaseSummaryResponse;
import com.Liang.java.ai.langchain4j.service.TriageService;
import com.Liang.java.ai.langchain4j.triage.TriageCaseStatus;
import com.Liang.java.ai.langchain4j.triage.TriageRiskLevel;
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

@WebMvcTest(TriageController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class, TriageControllerTest.JwtTestConfig.class})
class TriageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private TriageService triageService;

    @Test
    void patientCreatesCaseFromChiefComplaintOnly() throws Exception {
        when(triageService.create(7L, "反复头痛")).thenReturn(evidenceBackedCase(31L));

        mockMvc.perform(post("/api/v1/triage/cases")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chiefComplaint\":\"反复头痛\",\"patientId\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.recommendedDepartment").value("神经内科"))
                .andExpect(jsonPath("$.data.idCard").doesNotExist());

        verify(triageService).create(7L, "反复头痛");
    }

    @Test
    void doctorIsForbiddenBeforeTriageControllerRuns() throws Exception {
        mockMvc.perform(get("/api/v1/triage/cases")
                        .header(HttpHeaders.AUTHORIZATION, doctorBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权访问该资源"));

        verifyNoInteractions(triageService);
    }

    @Test
    void anonymousRequestIsRejectedBeforeTriageControllerRuns() throws Exception {
        mockMvc.perform(get("/api/v1/triage/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));

        verifyNoInteractions(triageService);
    }

    @Test
    void complaintMustHaveAtLeastTwoCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/triage/cases")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chiefComplaint\":\"痛\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("主诉长度需为2到1000个字符"));

        verifyNoInteractions(triageService);
    }

    @Test
    void patientListsOnlyCasesForAuthenticatedIdentity() throws Exception {
        when(triageService.listMine(7L)).thenReturn(List.of(
                new TriageCaseSummaryResponse(31L, TriageRiskLevel.ROUTINE,
                        TriageCaseStatus.EVIDENCE_BACKED, "神经内科", null)));

        mockMvc.perform(get("/api/v1/triage/cases")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(31))
                .andExpect(jsonPath("$.data[0].recommendedDepartment").value("神经内科"));

        verify(triageService).listMine(7L);
    }

    @Test
    void patientGetsCaseUsingAuthenticatedIdentity() throws Exception {
        when(triageService.getMine(7L, 31L)).thenReturn(evidenceBackedCase(31L));

        mockMvc.perform(get("/api/v1/triage/cases/31")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(31));

        verify(triageService).getMine(7L, 31L);
    }

    @Test
    void caseIdMustBePositive() throws Exception {
        mockMvc.perform(get("/api/v1/triage/cases/0")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("分诊ID必须大于0"));

        verifyNoInteractions(triageService);
    }

    private TriageCaseResponse evidenceBackedCase(Long id) {
        return new TriageCaseResponse(id, TriageRiskLevel.ROUTINE, TriageCaseStatus.EVIDENCE_BACKED,
                "神经内科", "尽快门诊", 0.91, "v1", null, null, null,
                "非诊断结论，如症状加重请及时就医", List.of());
    }

    private String patientBearer() {
        return bearerFor(new UserPrincipal(7L, "alice", UserRole.PATIENT));
    }

    private String doctorBearer() {
        return bearerFor(new UserPrincipal(17L, "doctor1", UserRole.DOCTOR));
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
