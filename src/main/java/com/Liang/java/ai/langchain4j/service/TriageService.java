package com.Liang.java.ai.langchain4j.service;
import com.Liang.java.ai.langchain4j.dto.triage.*;
import java.util.List;
public interface TriageService {
    TriageCaseResponse create(Long patientId, String chiefComplaint);
    List<TriageCaseSummaryResponse> listMine(Long patientId);
    TriageCaseResponse getMine(Long patientId, Long caseId);
}
