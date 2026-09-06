package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.appointment.AppointmentStatus;
import com.Liang.java.ai.langchain4j.auth.RequireRole;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.metrics.AdminMetricsResponse;
import com.Liang.java.ai.langchain4j.mapper.AppointmentMapper;
import com.Liang.java.ai.langchain4j.mapper.TriageCaseMapper;
import com.Liang.java.ai.langchain4j.mapper.WaitlistEntryMapper;
import com.Liang.java.ai.langchain4j.triage.TriageCaseStatus;
import com.Liang.java.ai.langchain4j.waitlist.WaitlistStatus;
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
