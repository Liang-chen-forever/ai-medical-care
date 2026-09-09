package com.liang.medical.dto.waitlist;

import com.liang.medical.appointment.entity.AppointmentStatus;

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
