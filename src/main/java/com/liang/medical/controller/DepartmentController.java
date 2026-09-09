package com.liang.medical.controller;

import com.liang.medical.bean.Result;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.common.BusinessException;
import com.liang.medical.entity.Doctor;
import com.liang.medical.entity.Schedule;
import com.liang.medical.service.DoctorService;
import com.liang.medical.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "科室与医生")
@RestController
@RequestMapping("/api/v1")
public class DepartmentController {

    private final DoctorService doctorService;
    private final ScheduleService scheduleService;

    public DepartmentController(DoctorService doctorService, ScheduleService scheduleService) {
        this.doctorService = doctorService;
        this.scheduleService = scheduleService;
    }

    @Operation(summary = "获取科室列表")
    @GetMapping("/departments")
    public ApiResponse<List<String>> getDepartments() {
        List<Doctor> doctors = doctorService.list();
        if (doctors == null || doctors.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        Set<String> departments = doctors.stream()
                .map(Doctor::getDepartment)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(department -> !department.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ApiResponse.success(List.copyOf(departments));
    }

    @Operation(summary = "根据科室获取医生列表")
    @GetMapping("/departments/{department}/doctors")
    public ApiResponse<List<Doctor>> getDoctors(@PathVariable("department") String department) {
        return ApiResponse.success(doctorService.getByDepartment(requireText(department, "科室不能为空")));
    }

    @Operation(summary = "获取科室可预约号源")
    @GetMapping("/schedules")
    public ApiResponse<List<Schedule>> getSchedules(
            @RequestParam("department") String department,
            @RequestParam("date") String date,
            @RequestParam("period") String period) {
        return ApiResponse.success(scheduleService.getAvailableSlots(
                requireText(department, "科室不能为空"),
                requireText(date, "日期不能为空"),
                requireText(period, "时段不能为空")));
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, message);
        }
        return value.trim();
    }
}
