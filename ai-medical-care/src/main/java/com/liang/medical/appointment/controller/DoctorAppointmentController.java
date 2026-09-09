package com.liang.medical.appointment.controller;

import com.liang.medical.appointment.entity.AppointmentStatus;
import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.appointment.dto.DoctorAppointmentResponse;
import com.liang.medical.appointment.dto.RejectAppointmentRequest;
import com.liang.medical.appointment.service.AppointmentBookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctor/appointments")
@RequireRole(UserRole.DOCTOR)
public class DoctorAppointmentController {
    private final AppointmentBookingService bookingService;

    public DoctorAppointmentController(AppointmentBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ApiResponse<List<DoctorAppointmentResponse>> list(@LoginUser UserPrincipal principal,
                                                               @RequestParam(required = false) String status) {
        AppointmentStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = AppointmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.liang.medical.common.BusinessException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, 400, "状态参数无效");
            }
        }
        return ApiResponse.success(bookingService.listForDoctor(principal.userId(), parsed));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@LoginUser UserPrincipal principal,
                                     @PathVariable @Positive(message = "预约ID必须大于0") Long id) {
        bookingService.confirmByDoctor(principal.userId(), id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@LoginUser UserPrincipal principal,
                                    @PathVariable @Positive(message = "预约ID必须大于0") Long id,
                                    @Valid @RequestBody RejectAppointmentRequest request) {
        bookingService.rejectByDoctor(principal.userId(), id, request.reason());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(@LoginUser UserPrincipal principal,
                                      @PathVariable @Positive(message = "预约ID必须大于0") Long id) {
        bookingService.completeByDoctor(principal.userId(), id);
        return ApiResponse.success();
    }
}
