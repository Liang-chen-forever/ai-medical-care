package com.Liang.java.ai.langchain4j.dto.doctor;

import com.Liang.java.ai.langchain4j.appointment.AppointmentStatus;

public record DoctorAppointmentResponse(
        Long id,
        Long scheduleId,
        String department,
        String date,
        String time,
        String doctorName,
        AppointmentStatus status,
        String cancelReason) {
}
