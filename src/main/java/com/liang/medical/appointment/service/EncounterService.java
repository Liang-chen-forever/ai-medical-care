package com.liang.medical.appointment.service;

import com.liang.medical.auth.UserRole;
import com.liang.medical.appointment.entity.Encounter;

public interface EncounterService {
    Encounter complete(Long doctorUserId, Long appointmentId, String summary, String followUpAdvice);

    Encounter getForUser(Long userId, UserRole role, Long appointmentId);
}
