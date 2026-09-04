package com.Liang.java.ai.langchain4j.service.impl;

import com.Liang.java.ai.langchain4j.common.BusinessException;
import com.Liang.java.ai.langchain4j.dto.triage.*;
import com.Liang.java.ai.langchain4j.entity.TriageCase;
import com.Liang.java.ai.langchain4j.entity.TriageEvidence;
import com.Liang.java.ai.langchain4j.mapper.TriageCaseMapper;
import com.Liang.java.ai.langchain4j.mapper.TriageEvidenceMapper;
import com.Liang.java.ai.langchain4j.service.TriageService;
import com.Liang.java.ai.langchain4j.triage.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TriageServiceImpl implements TriageService {
    private static final String DISCLAIMER = "非诊断结论，如症状加重请及时就医";
    private static final String EMERGENCY_INSTRUCTION = "请立即前往急诊或拨打 120";
    private static final String CARE_TIMING = "建议尽快线下确认";
    private final TriageCaseMapper caseMapper;
    private final TriageEvidenceMapper evidenceMapper;
    private final EmergencyRiskRuleEngine riskRuleEngine;
    private final TriageEvidencePolicy policy;
    private final TriageEvidenceRetriever evidenceRetriever;

    public TriageServiceImpl(TriageCaseMapper caseMapper, TriageEvidenceMapper evidenceMapper,
                             EmergencyRiskRuleEngine riskRuleEngine, TriageEvidencePolicy policy,
                             TriageEvidenceRetriever evidenceRetriever) {
        this.caseMapper = caseMapper;
        this.evidenceMapper = evidenceMapper;
        this.riskRuleEngine = riskRuleEngine;
        this.policy = policy;
        this.evidenceRetriever = evidenceRetriever;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TriageCaseResponse create(Long patientId, String chiefComplaint) {
        RiskRuleMatch emergency = riskRuleEngine.match(chiefComplaint).orElse(null);
        if (emergency != null) {
            TriageCase entity = base(patientId, chiefComplaint, TriageRiskLevel.EMERGENCY, TriageCaseStatus.EMERGENCY_BLOCKED);
            entity.setRuleCode(emergency.ruleCode());
            insert(entity);
            return response(entity, List.of(), EMERGENCY_INSTRUCTION);
        }
        List<RetrievedEvidence> raw;
        try {
            raw = evidenceRetriever.retrieve(chiefComplaint);
        } catch (RuntimeException ex) {
            return fallback(patientId, chiefComplaint, TriageFallbackReason.RETRIEVAL_UNAVAILABLE);
        }
        List<RetrievedEvidence> ranked = policy.validAndRanked(raw);
        if (ranked.isEmpty()) {
            return fallback(patientId, chiefComplaint,
                    raw != null && !raw.isEmpty() ? TriageFallbackReason.INVALID_EVIDENCE : TriageFallbackReason.NO_EVIDENCE);
        }
        TriageRecommendation recommendation = policy.recommend(ranked);
        TriageCase entity = base(patientId, chiefComplaint, TriageRiskLevel.ROUTINE, TriageCaseStatus.EVIDENCE_BACKED);
        entity.setRecommendedDepartment(recommendation.department());
        entity.setRetrievalConfidence(recommendation.confidence());
        entity.setKnowledgeVersion(recommendation.knowledgeVersion());
        insert(entity);
        List<TriageEvidenceResponse> snapshots = new ArrayList<>();
        int rank = 1;
        for (RetrievedEvidence item : ranked) {
            TriageEvidence row = new TriageEvidence();
            row.setTriageCaseId(entity.getId()); row.setDocumentId(item.documentId()); row.setChunkId(item.chunkId());
            row.setExcerpt(item.excerpt()); row.setScore(item.score()); row.setRankNo(rank); row.setKnowledgeVersion(item.knowledgeVersion());
            evidenceMapper.insert(row);
            snapshots.add(new TriageEvidenceResponse(item.documentId(), item.chunkId(), item.excerpt(), item.score(), rank++, item.knowledgeVersion()));
        }
        return response(entity, snapshots, CARE_TIMING);
    }

    private TriageCaseResponse fallback(Long patientId, String complaint, TriageFallbackReason reason) {
        TriageCase entity = base(patientId, complaint, TriageRiskLevel.UNKNOWN, TriageCaseStatus.FALLBACK);
        entity.setFallbackReason(reason);
        insert(entity);
        return response(entity, List.of(), CARE_TIMING);
    }

    private TriageCase base(Long patientId, String complaint, TriageRiskLevel risk, TriageCaseStatus status) {
        TriageCase e = new TriageCase(); e.setPatientId(patientId); e.setChiefComplaint(complaint); e.setRiskLevel(risk); e.setStatus(status); e.setCreatedAt(LocalDateTime.now()); return e;
    }
    private void insert(TriageCase e) { caseMapper.insert(e); }

    @Override
    public List<TriageCaseSummaryResponse> listMine(Long patientId) {
        List<TriageCase> cases = caseMapper.selectList(new QueryWrapper<TriageCase>().eq("patient_id", patientId).orderByDesc("created_at").last("LIMIT 20"));
        return cases.stream().map(e -> new TriageCaseSummaryResponse(e.getId(), e.getRiskLevel(), e.getStatus(), e.getRecommendedDepartment(), e.getCreatedAt())).toList();
    }

    @Override
    public TriageCaseResponse getMine(Long patientId, Long caseId) {
        TriageCase e = caseMapper.selectById(caseId);
        if (e == null) throw new BusinessException(HttpStatus.NOT_FOUND, 404, "分诊记录不存在");
        if (!patientId.equals(e.getPatientId())) throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权访问该资源");
        List<TriageEvidenceResponse> evidence = evidenceMapper.selectList(new QueryWrapper<TriageEvidence>().eq("triage_case_id", caseId).orderByAsc("rank_no")).stream()
                .map(r -> new TriageEvidenceResponse(r.getDocumentId(), r.getChunkId(), r.getExcerpt(), r.getScore(), r.getRankNo(), r.getKnowledgeVersion())).toList();
        return response(e, evidence, e.getStatus() == TriageCaseStatus.EMERGENCY_BLOCKED ? EMERGENCY_INSTRUCTION : CARE_TIMING);
    }

    private TriageCaseResponse response(TriageCase e, List<TriageEvidenceResponse> evidence, String timing) {
        boolean emergency = e.getStatus() == TriageCaseStatus.EMERGENCY_BLOCKED;
        return new TriageCaseResponse(e.getId(), e.getRiskLevel(), e.getStatus(), e.getRecommendedDepartment(), timing,
                e.getRetrievalConfidence(), e.getKnowledgeVersion(), e.getRuleCode(), e.getFallbackReason(), emergency ? EMERGENCY_INSTRUCTION : null,
                DISCLAIMER, evidence);
    }
}
