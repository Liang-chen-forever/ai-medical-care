package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.bean.Result;
import com.Liang.java.ai.langchain4j.entity.Doctor;
import com.Liang.java.ai.langchain4j.entity.Schedule;
import com.Liang.java.ai.langchain4j.service.DoctorService;
import com.Liang.java.ai.langchain4j.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "科室与医生")
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ScheduleService scheduleService;

    @Operation(summary = "根据科室获取医生列表")
    @GetMapping("/doctors")
    public Result<List<Doctor>> getDoctors(@RequestParam String department) {
        return Result.success(doctorService.getByDepartment(department));
    }

    @Operation(summary = "获取科室可预约号源")
    @GetMapping("/schedules")
    public Result<List<Schedule>> getSchedules(
            @RequestParam String department,
            @RequestParam String date,
            @RequestParam String time) {
        return Result.success(scheduleService.getAvailableSlots(department, date, time));
    }
}