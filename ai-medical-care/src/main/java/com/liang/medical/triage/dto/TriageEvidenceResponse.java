package com.liang.medical.triage.dto;
public record TriageEvidenceResponse(String documentId, String chunkId, String excerpt, double score, int rank, String knowledgeVersion) {}
