package com.liang.medical.waitlist.dto;

import com.liang.medical.waitlist.entity.WaitlistStatus;

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
