package com.liang.medical.triage;

public record RetrievedEvidence(String documentId, String chunkId, String department,
                                String knowledgeVersion, String excerpt, double score) {
}
