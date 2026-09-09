package com.liang.medical.waitlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record JoinWaitlistRequest(
        @NotNull(message = "scheduleId不能为空")
        @Positive(message = "scheduleId必须大于0") Long scheduleId,
        @Positive(message = "triageCaseId必须大于0") Long triageCaseId
) {
}
