package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.appointment.AppointmentStatus;
import com.Liang.java.ai.langchain4j.auth.LoginUser;
import com.Liang.java.ai.langchain4j.auth.RequireRole;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.doctor.DoctorAppointmentResponse;
import com.Liang.java.ai.langchain4j.dto.doctor.RejectAppointmentRequest;
import com.Liang.java.ai.langchain4j.service.AppointmentBookingService;
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
                throw new com.Liang.java.ai.langchain4j.common.BusinessException(
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
