package com.Liang.java.ai.langchain4j.release;

import com.Liang.java.ai.langchain4j.service.TriageService;
import com.Liang.java.ai.langchain4j.store.MongoChatMemoryStore;
import com.Liang.java.ai.langchain4j.triage.TriageEvidenceRetriever;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("external")
@SpringBootTest(properties = {
        "spring.main.web-application-type=none",
        "spring.profiles.active=release-validation"
})
class TriageTransactionExternalIntegrationTest {

    private static final String TRIGGER = "release_validation_reject_evidence";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TriageService triageService;

    @MockBean(name = "contentRetrieverXiaozhiPincone")
    private ContentRetriever contentRetriever;
    @MockBean
    private EmbeddingStore<?> embeddingStore;
    @MockBean
    private EmbeddingModel embeddingModel;
    @MockBean
    private MongoChatMemoryStore mongoChatMemoryStore;

    @DynamicPropertySource
    static void releaseValidationProperties(DynamicPropertyRegistry registry) {
        if (ExternalValidationEnvironment.isConfigured()) {
            registry.add("spring.datasource.url", ExternalValidationEnvironment::requireValidationJdbcUrl);
            registry.add("spring.datasource.username", ExternalValidationEnvironment::requireDbUser);
            registry.add("spring.datasource.password", ExternalValidationEnvironment::requireDbPassword);
        }
    }

    @BeforeEach
    void installRejectingTrigger() {
        Assumptions.assumeTrue(ExternalValidationEnvironment.isConfigured());
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + TRIGGER);
        jdbcTemplate.execute("CREATE TRIGGER " + TRIGGER
                + " BEFORE INSERT ON triage_evidence FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'release validation evidence rejection'");
        jdbcTemplate.update("DELETE FROM triage_evidence");
        jdbcTemplate.update("DELETE FROM triage_case");
    }

    @AfterEach
    void removeRejectingTrigger() {
        if (ExternalValidationEnvironment.isConfigured()) {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + TRIGGER);
            jdbcTemplate.update("DELETE FROM triage_evidence");
            jdbcTemplate.update("DELETE FROM triage_case");
        }
    }

    @Test
    void evidenceWriteFailureRollsBackCaseAndEvidenceRows() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of(Content.from(
                TextSegment.from("持续头痛，建议神经内科评估", new Metadata()
                        .put("documentId", "release-validation")
                        .put("department", "神经内科")
                        .put("knowledgeVersion", "2026.09")),
                Map.of(ContentMetadata.SCORE, 0.95))));

        assertThatThrownBy(() -> triageService.create(9001L, "反复头痛"))
                .isInstanceOf(RuntimeException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM triage_case WHERE patient_id = 9001", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM triage_evidence", Integer.class)).isZero();
    }
}
