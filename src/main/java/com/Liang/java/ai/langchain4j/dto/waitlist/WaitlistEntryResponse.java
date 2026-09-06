package com.Liang.java.ai.langchain4j.dto.waitlist;

import com.Liang.java.ai.langchain4j.waitlist.WaitlistStatus;

import java.time.LocalDateTime;

public record WaitlistEntryResponse(
        Long id,
        Long scheduleId,
        Long triageCaseId,
        Long appointmentId,
        Integer priority,
        WaitlistStatus status,
        LocalDateTime offerExpiresAt,
        LocalDateTime offeredAt,
        LocalDateTime acceptedAt,
        LocalDateTime createdAt
) {
}
