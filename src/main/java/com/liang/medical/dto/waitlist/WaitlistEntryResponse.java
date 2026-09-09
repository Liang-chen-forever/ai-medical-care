package com.liang.medical.dto.waitlist;

import com.liang.medical.waitlist.WaitlistStatus;

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
