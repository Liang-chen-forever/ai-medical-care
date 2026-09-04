package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import com.Liang.java.ai.langchain4j.auth.RoleRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.GlobalExceptionHandler;
import com.Liang.java.ai.langchain4j.config.WebMvcConfig;
import com.Liang.java.ai.langchain4j.dto.knowledge.KnowledgeReloadResponse;
import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class,
        KnowledgeControllerTest.JwtTestConfig.class})
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private KnowledgeSeedLoader knowledgeSeedLoader;

    @Test
    void patientCannotReloadKnowledge() throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge/reload")
                        .header(HttpHeaders.AUTHORIZATION, patientBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(knowledgeSeedLoader);
    }

    @Test
    void adminCanReloadKnowledge() throws Exception {
        when(knowledgeSeedLoader.reload()).thenReturn(new KnowledgeReloadResponse(4, "2026.09"));

        mockMvc.perform(post("/api/v1/admin/knowledge/reload")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentsLoaded").value(4));

        verify(knowledgeSeedLoader).reload();
    }

    private String patientBearer() {
        return bearerFor(new UserPrincipal(7L, "alice", UserRole.PATIENT));
    }

    private String adminBearer() {
        return bearerFor(new UserPrincipal(1L, "admin", UserRole.ADMIN));
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
