package com.liang.medical.controller;

import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.dto.appointment.CreateAppointmentRequest;
import com.liang.medical.entity.Appointment;
import com.liang.medical.service.AppointmentBookingService;
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
        Appointment appointment = request.triageCaseId() == null
                ? appointmentBookingService.book(user.userId(), request.scheduleId())
                : appointmentBookingService.book(user.userId(), request.scheduleId(), request.triageCaseId());
        return ApiResponse.success(appointment);
    }

    @Operation(summary = "取消预约")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@LoginUser UserPrincipal user,
                                    @PathVariable @Positive(message = "预约ID必须大于0") Long id) {
        appointmentBookingService.cancel(user.userId(), id);
        return ApiResponse.success();
    }
}
