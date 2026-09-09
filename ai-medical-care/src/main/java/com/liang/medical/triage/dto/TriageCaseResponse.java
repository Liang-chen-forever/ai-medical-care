package com.liang.medical.triage.dto;
import com.liang.medical.triage.*;
import java.util.List;
public record TriageCaseResponse(Long id, TriageRiskLevel riskLevel, TriageCaseStatus status, String recommendedDepartment,
        String careTiming, Double retrievalConfidence, String knowledgeVersion, String ruleCode, TriageFallbackReason fallbackReason,
        String emergencyInstruction, String disclaimer, List<TriageEvidenceResponse> evidence) {}
