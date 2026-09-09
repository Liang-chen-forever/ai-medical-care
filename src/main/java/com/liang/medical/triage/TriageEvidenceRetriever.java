package com.liang.medical.triage;

import java.util.List;

public interface TriageEvidenceRetriever {
    List<RetrievedEvidence> retrieve(String chiefComplaint);
}
