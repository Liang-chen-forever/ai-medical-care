package com.liang.medical.triage.service;
import com.liang.medical.triage.dto.*;
import java.util.List;
public interface TriageService {
    TriageCaseResponse create(Long patientId, String chiefComplaint);
    List<TriageCaseSummaryResponse> listMine(Long patientId);
    TriageCaseResponse getMine(Long patientId, Long caseId);
}
