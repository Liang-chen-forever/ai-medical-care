package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.entity.Encounter;

public interface EncounterService {
    Encounter complete(Long doctorUserId, Long appointmentId, String summary, String followUpAdvice);

    Encounter getForUser(Long userId, UserRole role, Long appointmentId);
}
