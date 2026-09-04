package com.Liang.java.ai.langchain4j.triage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageEvidencePolicyTest {

    private final TriageEvidencePolicy policy = new TriageEvidencePolicy();

    @Test
    void policyKeepsOnlyCompleteHighConfidenceEvidenceAndUsesTopDepartment() {
        List<RetrievedEvidence> valid = policy.validAndRanked(List.of(
                evidence("neuro", "n-1", "神经内科", "2026.09", 0.91),
                evidence("dental", "d-1", "口腔科", "2026.09", 0.73),
                evidence("bad", "", "口腔科", "2026.09", 0.99),
                evidence("low", "l-1", "神经内科", "2026.09", 0.71)));

        assertThat(valid).extracting(RetrievedEvidence::chunkId).containsExactly("n-1", "d-1");
        assertThat(policy.recommend(valid)).isEqualTo(new TriageRecommendation("神经内科", 0.91, "2026.09"));
    }

    private RetrievedEvidence evidence(String documentId, String chunkId, String department,
                                       String version, double score) {
        return new RetrievedEvidence(documentId, chunkId, department, version, "受限摘录", score);
    }
}
