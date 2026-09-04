package com.Liang.java.ai.langchain4j.triage;

import java.util.Comparator;
import java.util.List;

public class TriageEvidencePolicy {

    private static final double MINIMUM_CONFIDENCE = 0.72;

    public List<RetrievedEvidence> validAndRanked(List<RetrievedEvidence> candidates) {
        if (candidates == null) {
            return List.of();
        }

        return candidates.stream()
                .filter(this::isValid)
                .sorted(Comparator.comparingDouble(RetrievedEvidence::score).reversed()
                        .thenComparing(RetrievedEvidence::chunkId))
                .toList();
    }

    public TriageRecommendation recommend(List<RetrievedEvidence> candidates) {
        return validAndRanked(candidates).stream()
                .findFirst()
                .map(evidence -> new TriageRecommendation(
                        evidence.department(), evidence.score(), evidence.knowledgeVersion()))
                .orElse(null);
    }

    private boolean isValid(RetrievedEvidence evidence) {
        return evidence != null
                && isPresent(evidence.documentId())
                && isPresent(evidence.chunkId())
                && isPresent(evidence.department())
                && isPresent(evidence.knowledgeVersion())
                && isPresent(evidence.excerpt())
                && evidence.score() >= MINIMUM_CONFIDENCE;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
