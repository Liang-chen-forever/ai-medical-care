package com.Liang.java.ai.langchain4j.entity;

import com.Liang.java.ai.langchain4j.triage.TriageCaseStatus;
import com.Liang.java.ai.langchain4j.triage.TriageFallbackReason;
import com.Liang.java.ai.langchain4j.triage.TriageRiskLevel;
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
