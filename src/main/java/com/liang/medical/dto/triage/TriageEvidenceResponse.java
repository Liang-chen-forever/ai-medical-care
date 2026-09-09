package com.liang.medical.dto.triage;
public record TriageEvidenceResponse(String documentId, String chunkId, String excerpt, double score, int rank, String knowledgeVersion) {}
