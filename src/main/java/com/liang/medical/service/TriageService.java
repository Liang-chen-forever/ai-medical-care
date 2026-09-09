package com.liang.medical.service;
import com.liang.medical.dto.triage.*;
import java.util.List;
public interface TriageService {
    TriageCaseResponse create(Long patientId, String chiefComplaint);
    List<TriageCaseSummaryResponse> listMine(Long patientId);
    TriageCaseResponse getMine(Long patientId, Long caseId);
}
