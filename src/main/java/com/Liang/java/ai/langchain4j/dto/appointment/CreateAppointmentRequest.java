package com.Liang.java.ai.langchain4j.dto.appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAppointmentRequest(
        @NotNull(message = "scheduleId不能为空")
        @Positive(message = "scheduleId必须大于0") Long scheduleId
) {
}
