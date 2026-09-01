package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.LoginUser;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.appointment.CreateAppointmentRequest;
import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.service.AppointmentBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "预约管理")
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentBookingService appointmentBookingService;

    public AppointmentController(AppointmentBookingService appointmentBookingService) {
        this.appointmentBookingService = appointmentBookingService;
    }

    @Operation(summary = "查询当前用户的预约")
    @GetMapping("/me")
    public ApiResponse<List<Appointment>> listMine(@LoginUser UserPrincipal user) {
        return ApiResponse.success(appointmentBookingService.listMine(user.userId()));
    }

    @Operation(summary = "预约挂号")
    @PostMapping
    public ApiResponse<Appointment> book(@LoginUser UserPrincipal user,
                                          @Valid @RequestBody CreateAppointmentRequest request) {
        return ApiResponse.success(appointmentBookingService.book(user.userId(), request.scheduleId()));
    }

    @Operation(summary = "取消预约")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@LoginUser UserPrincipal user,
                                    @PathVariable @Positive(message = "预约ID必须大于0") Long id) {
        appointmentBookingService.cancel(user.userId(), id);
        return ApiResponse.success();
    }
}
