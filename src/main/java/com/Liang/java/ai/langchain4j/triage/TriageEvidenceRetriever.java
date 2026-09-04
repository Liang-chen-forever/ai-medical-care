package com.Liang.java.ai.langchain4j.triage;

import java.util.List;

public interface TriageEvidenceRetriever {
    List<RetrievedEvidence> retrieve(String chiefComplaint);
}
