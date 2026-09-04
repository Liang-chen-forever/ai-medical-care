package com.Liang.java.ai.langchain4j.dto.triage;
import com.Liang.java.ai.langchain4j.triage.*;
import java.time.LocalDateTime;
public record TriageCaseSummaryResponse(Long id, TriageRiskLevel riskLevel, TriageCaseStatus status, String recommendedDepartment, LocalDateTime createdAt) {}
