package com.Liang.java.ai.langchain4j.triage;

public record TriageRecommendation(String department, double confidence,
                                   String knowledgeVersion) {
}
