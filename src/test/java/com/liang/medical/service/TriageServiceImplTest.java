package com.liang.medical.service;

import com.liang.medical.triage.dto.TriageCaseResponse;
import com.liang.medical.triage.dto.TriageEvidenceResponse;
import com.liang.medical.common.BusinessException;
import com.liang.medical.triage.entity.TriageCase;
import com.liang.medical.triage.entity.TriageEvidence;
import com.liang.medical.triage.mapper.TriageCaseMapper;
import com.liang.medical.triage.mapper.TriageEvidenceMapper;
import com.liang.medical.triage.service.TriageServiceImpl;
import com.liang.medical.triage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

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

    @Test
    void emptyRetrieverPersistsNoEvidenceFallback() {
        when(riskRuleEngine.match("未知不适")).thenReturn(Optional.empty());
        when(evidenceRetriever.retrieve("未知不适")).thenReturn(List.of());
        assignId(34L);

        TriageCaseResponse response = service.create(7L, "未知不适");

        assertThat(response.status()).isEqualTo(TriageCaseStatus.FALLBACK);
        assertThat(response.fallbackReason()).isEqualTo(TriageFallbackReason.NO_EVIDENCE);
        assertThat(response.recommendedDepartment()).isNull();
        assertThat(response.evidence()).isEmpty();
        verifyNoInteractions(evidenceMapper);
    }

    @Test
    void invalidEvidencePersistsInvalidEvidenceFallback() {
        when(riskRuleEngine.match("模糊症状")).thenReturn(Optional.empty());
        when(evidenceRetriever.retrieve("模糊症状")).thenReturn(List.of(
                new RetrievedEvidence("doc", "chunk", null, "2026.09", "摘录", 0.95)));
        assignId(35L);

        TriageCaseResponse response = service.create(7L, "模糊症状");

        assertThat(response.status()).isEqualTo(TriageCaseStatus.FALLBACK);
        assertThat(response.fallbackReason()).isEqualTo(TriageFallbackReason.INVALID_EVIDENCE);
        assertThat(response.recommendedDepartment()).isNull();
        verifyNoInteractions(evidenceMapper);
    }

    @Test
    void listMineScopesPatientNewestFirstAndLimitsToTwenty() {
        List<TriageCase> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            TriageCase row = new TriageCase(); row.setId((long) (25 - i)); row.setPatientId(7L);
            row.setRiskLevel(TriageRiskLevel.UNKNOWN); row.setStatus(TriageCaseStatus.FALLBACK);
            row.setCreatedAt(LocalDateTime.now().minusMinutes(i)); rows.add(row);
        }
        when(caseMapper.selectList(any())).thenReturn(rows);

        var result = service.listMine(7L);

        assertThat(result).hasSize(20);
        assertThat(result).extracting(r -> r.id()).containsExactlyElementsOf(rows.stream().limit(20).map(TriageCase::getId).toList());
        ArgumentCaptor<QueryWrapper<TriageCase>> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(caseMapper).selectList(queryCaptor.capture());
        QueryWrapper<TriageCase> captured = queryCaptor.getValue();
        String sql = captured.getSqlSegment();
        Matcher patientPredicate = Pattern.compile("patient_id\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.([A-Za-z0-9_]+)\\}", Pattern.CASE_INSENSITIVE).matcher(sql);
        assertThat(patientPredicate.find()).as("patient equality predicate should bind a named parameter").isTrue();
        String patientParameterKey = patientPredicate.group(1);
        assertThat(captured.getParamNameValuePairs().get(patientParameterKey)).isEqualTo(7L);
        String normalizedSql = sql.toLowerCase();
        assertThat(normalizedSql).contains("order by created_at desc");
        assertThat(normalizedSql).contains("limit 20");
    }

    @Test
    void getMineDistinguishesAbsentWrongOwnerAndCorrectOwner() {
        when(caseMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getMine(7L, 99L)).isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(404));

        TriageCase other = new TriageCase(); other.setId(100L); other.setPatientId(8L);
        other.setRiskLevel(TriageRiskLevel.ROUTINE); other.setStatus(TriageCaseStatus.EVIDENCE_BACKED);
        when(caseMapper.selectById(100L)).thenReturn(other);
        assertThatThrownBy(() -> service.getMine(7L, 100L)).isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(403));

        TriageCase mine = new TriageCase(); mine.setId(101L); mine.setPatientId(7L);
        mine.setRiskLevel(TriageRiskLevel.ROUTINE); mine.setStatus(TriageCaseStatus.EVIDENCE_BACKED);
        mine.setCreatedAt(LocalDateTime.now());
        when(caseMapper.selectById(101L)).thenReturn(mine);
        when(evidenceMapper.selectList(any())).thenReturn(List.of());
        TriageCaseResponse response = service.getMine(7L, 101L);
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.evidence()).isEmpty();
    }

    private void assignId(long id) {
        when(caseMapper.insert(any(TriageCase.class))).thenAnswer(inv -> { inv.getArgument(0, TriageCase.class).setId(id); return 1; });
    }
    private RetrievedEvidence evidence(String documentId, String chunkId, String department, String version, double score) {
        return new RetrievedEvidence(documentId, chunkId, department, version, "受限摘录", score);
    }
}
