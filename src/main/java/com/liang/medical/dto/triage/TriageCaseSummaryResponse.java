package com.liang.medical.dto.triage;
import com.liang.medical.triage.*;
import java.time.LocalDateTime;
public record TriageCaseSummaryResponse(Long id, TriageRiskLevel riskLevel, TriageCaseStatus status, String recommendedDepartment, LocalDateTime createdAt) {}
