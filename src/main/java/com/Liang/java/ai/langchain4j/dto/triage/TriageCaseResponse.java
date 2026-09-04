package com.Liang.java.ai.langchain4j.dto.triage;
import com.Liang.java.ai.langchain4j.triage.*;
import java.util.List;
public record TriageCaseResponse(Long id, TriageRiskLevel riskLevel, TriageCaseStatus status, String recommendedDepartment,
        String careTiming, Double retrievalConfidence, String knowledgeVersion, String ruleCode, TriageFallbackReason fallbackReason,
        String emergencyInstruction, String disclaimer, List<TriageEvidenceResponse> evidence) {}
