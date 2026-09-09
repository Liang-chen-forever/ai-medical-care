package com.liang.medical.dto.doctor;

import com.liang.medical.appointment.AppointmentStatus;

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
