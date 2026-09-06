package com.Liang.java.ai.langchain4j.dto.waitlist;

import com.Liang.java.ai.langchain4j.appointment.AppointmentStatus;

public record WaitlistAcceptedResponse(
        Long appointmentId,
        Long scheduleId,
        String department,
        String date,
        String time,
        AppointmentStatus status,
        Long triageCaseId
) {
}
