package com.liang.medical.triage;

public record TriageRecommendation(String department, double confidence,
                                   String knowledgeVersion) {
}
