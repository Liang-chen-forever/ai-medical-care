package com.Liang.java.ai.langchain4j.triage;

public record RetrievedEvidence(String documentId, String chunkId, String department,
                                String knowledgeVersion, String excerpt, double score) {
}
