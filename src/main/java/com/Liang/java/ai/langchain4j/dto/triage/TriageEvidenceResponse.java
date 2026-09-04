package com.Liang.java.ai.langchain4j.dto.triage;
public record TriageEvidenceResponse(String documentId, String chunkId, String excerpt, double score, int rank, String knowledgeVersion) {}
