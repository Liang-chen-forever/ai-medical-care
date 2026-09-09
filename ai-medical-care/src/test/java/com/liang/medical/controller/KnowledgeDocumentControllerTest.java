package com.liang.medical.controller;

import com.liang.medical.knowledge.controller.KnowledgeDocumentController;
import com.liang.medical.auth.JwtTokenService;
import com.liang.medical.auth.LoginRequiredInterceptor;
import com.liang.medical.auth.LoginUserArgumentResolver;
import com.liang.medical.auth.RoleRequiredInterceptor;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.GlobalExceptionHandler;
import com.liang.medical.config.WebMvcConfig;
import com.liang.medical.knowledge.entity.KnowledgeDocument;
import com.liang.medical.knowledge.KnowledgeDocumentStatus;
import com.liang.medical.knowledge.service.KnowledgeDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeDocumentController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, RoleRequiredInterceptor.class,
        KnowledgeDocumentControllerTest.JwtTestConfig.class})
class KnowledgeDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private KnowledgeDocumentService documentService;

    @Test
    void patientCannotManageDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge/documents")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(7L, "alice", UserRole.PATIENT))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(documentService);
    }

    @Test
    void adminUploadUsesAuthenticatedAdminAndReturnsMetadataOnly() throws Exception {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(12L);
        document.setDocumentKey("triage");
        document.setDocumentName("triage.md");
        document.setContentSha256("a".repeat(64));
        document.setVersionNo(1);
        document.setStatus(KnowledgeDocumentStatus.DRAFT);
        when(documentService.upload(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(document);

        MockMultipartFile file = new MockMultipartFile("file", "triage.md", "text/markdown",
                "内容".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/admin/knowledge/documents")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(1L, "admin", UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.contentText").doesNotExist());

        verify(documentService).upload(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    void adminCanPublishDocumentVersion() throws Exception {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(12L);
        document.setDocumentKey("triage");
        document.setVersionNo(2);
        document.setStatus(KnowledgeDocumentStatus.PUBLISHED);
        when(documentService.publish(1L, 12L)).thenReturn(document);

        mockMvc.perform(post("/api/v1/admin/knowledge/documents/12/publish")
                        .header(HttpHeaders.AUTHORIZATION, bearer(new UserPrincipal(1L, "admin", UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(documentService).publish(1L, 12L);
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
