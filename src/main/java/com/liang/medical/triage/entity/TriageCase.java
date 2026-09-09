package com.liang.medical.triage.entity;

import com.liang.medical.triage.TriageCaseStatus;
import com.liang.medical.triage.TriageFallbackReason;
import com.liang.medical.triage.TriageRiskLevel;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("triage_case")
public class TriageCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private String chiefComplaint;
    private TriageRiskLevel riskLevel;
    private TriageCaseStatus status;
    private String recommendedDepartment;
    private Double retrievalConfidence;
    private String knowledgeVersion;
    private String ruleCode;
    private TriageFallbackReason fallbackReason;
    private LocalDateTime createdAt;
}
