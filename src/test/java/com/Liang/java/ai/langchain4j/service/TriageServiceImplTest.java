package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.dto.triage.TriageCaseResponse;
import com.Liang.java.ai.langchain4j.dto.triage.TriageEvidenceResponse;
import com.Liang.java.ai.langchain4j.entity.TriageCase;
import com.Liang.java.ai.langchain4j.entity.TriageEvidence;
import com.Liang.java.ai.langchain4j.mapper.TriageCaseMapper;
import com.Liang.java.ai.langchain4j.mapper.TriageEvidenceMapper;
import com.Liang.java.ai.langchain4j.service.impl.TriageServiceImpl;
import com.Liang.java.ai.langchain4j.triage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageServiceImplTest {
    @Mock TriageCaseMapper caseMapper;
    @Mock TriageEvidenceMapper evidenceMapper;
    @Mock EmergencyRiskRuleEngine riskRuleEngine;
    @Mock TriageEvidenceRetriever evidenceRetriever;
    private TriageServiceImpl service;

    @BeforeEach void setUp() { service = new TriageServiceImpl(caseMapper, evidenceMapper, riskRuleEngine, new TriageEvidencePolicy(), evidenceRetriever); }

    @Test
    void emergencyCaseDoesNotRetrieveEvidenceAndStoresRuleCode() {
        when(riskRuleEngine.match("胸痛而且喘不过气")).thenReturn(Optional.of(new RiskRuleMatch("CHEST_PAIN_WITH_BREATHLESSNESS")));
        assignId(31L);

        TriageCaseResponse response = service.create(7L, "胸痛而且喘不过气");

        assertThat(response.status()).isEqualTo(TriageCaseStatus.EMERGENCY_BLOCKED);
        assertThat(response.ruleCode()).isEqualTo("CHEST_PAIN_WITH_BREATHLESSNESS");
        assertThat(response.recommendedDepartment()).isNull();
        assertThat(response.retrievalConfidence()).isNull();
        assertThat(response.evidence()).isEmpty();
        verifyNoInteractions(evidenceRetriever, evidenceMapper);
    }

    @Test
    void validEvidencePersistsCaseAndRankedSnapshots() {
        when(riskRuleEngine.match("反复头痛")).thenReturn(Optional.empty());
        when(evidenceRetriever.retrieve("反复头痛")).thenReturn(List.of(
                evidence("neuro", "b", "神经内科", "2026.09", 0.82), evidence("neuro", "a", "神经内科", "2026.09", 0.91)));
        assignId(32L);

        TriageCaseResponse response = service.create(7L, "反复头痛");

        assertThat(response.status()).isEqualTo(TriageCaseStatus.EVIDENCE_BACKED);
        assertThat(response.recommendedDepartment()).isEqualTo("神经内科");
        assertThat(response.evidence()).extracting(TriageEvidenceResponse::rank).containsExactly(1, 2);
        verify(evidenceMapper, times(2)).insert(any(TriageEvidence.class));
    }

    @Test
    void unavailableRetrieverPersistsFallbackWithoutDepartment() {
        when(riskRuleEngine.match("牙痛")).thenReturn(Optional.empty());
        when(evidenceRetriever.retrieve("牙痛")).thenThrow(new IllegalStateException("vector unavailable"));
        assignId(33L);

        TriageCaseResponse response = service.create(7L, "牙痛");

        assertThat(response.status()).isEqualTo(TriageCaseStatus.FALLBACK);
        assertThat(response.fallbackReason()).isEqualTo(TriageFallbackReason.RETRIEVAL_UNAVAILABLE);
        assertThat(response.recommendedDepartment()).isNull();
        assertThat(response.retrievalConfidence()).isNull();
        verifyNoInteractions(evidenceMapper);
    }

    private void assignId(long id) {
        when(caseMapper.insert(any(TriageCase.class))).thenAnswer(inv -> { inv.getArgument(0, TriageCase.class).setId(id); return 1; });
    }
    private RetrievedEvidence evidence(String documentId, String chunkId, String department, String version, double score) {
        return new RetrievedEvidence(documentId, chunkId, department, version, "受限摘录", score);
    }
}
