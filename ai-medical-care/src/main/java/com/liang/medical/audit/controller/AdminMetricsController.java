package com.liang.medical.audit.controller;

import com.liang.medical.appointment.entity.AppointmentStatus;
import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.audit.dto.AdminMetricsResponse;
import com.liang.medical.appointment.mapper.AppointmentMapper;
import com.liang.medical.triage.mapper.TriageCaseMapper;
import com.liang.medical.waitlist.mapper.WaitlistEntryMapper;
import com.liang.medical.triage.TriageCaseStatus;
import com.liang.medical.waitlist.entity.WaitlistStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequireRole(UserRole.ADMIN)
public class AdminMetricsController {
    private final AppointmentMapper appointmentMapper;
    private final TriageCaseMapper triageCaseMapper;
    private final WaitlistEntryMapper waitlistEntryMapper;

    public AdminMetricsController(AppointmentMapper appointmentMapper, TriageCaseMapper triageCaseMapper,
                                  WaitlistEntryMapper waitlistEntryMapper) {
        this.appointmentMapper = appointmentMapper;
        this.triageCaseMapper = triageCaseMapper;
        this.waitlistEntryMapper = waitlistEntryMapper;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminMetricsResponse> overview() {
        Map<String, Long> appointments = new LinkedHashMap<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            appointments.put(status.name(), appointmentMapper.countByStatus(status));
        }
        Map<String, Long> triageCases = new LinkedHashMap<>();
        for (TriageCaseStatus status : TriageCaseStatus.values()) {
            triageCases.put(status.name(), triageCaseMapper.countByStatus(status.name()));
        }
        Map<String, Long> waitlistEntries = new LinkedHashMap<>();
        for (WaitlistStatus status : WaitlistStatus.values()) {
            waitlistEntries.put(status.name(), waitlistEntryMapper.countByStatus(status));
        }
        return ApiResponse.success(new AdminMetricsResponse(appointments, triageCases, waitlistEntries));
    }
}
