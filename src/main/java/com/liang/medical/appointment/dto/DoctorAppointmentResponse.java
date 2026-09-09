package com.liang.medical.appointment.dto;

import com.liang.medical.appointment.entity.AppointmentStatus;

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
