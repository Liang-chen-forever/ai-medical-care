package com.liang.medical.controller;

import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.dto.waitlist.JoinWaitlistRequest;
import com.liang.medical.dto.waitlist.WaitlistAcceptedResponse;
import com.liang.medical.dto.waitlist.WaitlistEntryResponse;
import com.liang.medical.entity.Appointment;
import com.liang.medical.entity.WaitlistEntry;
import com.liang.medical.service.WaitlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/waitlist")
@RequireRole(UserRole.PATIENT)
public class WaitlistController {
    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public ApiResponse<WaitlistEntryResponse> join(@LoginUser UserPrincipal principal,
                                                    @Valid @RequestBody JoinWaitlistRequest request) {
        return ApiResponse.success(toResponse(waitlistService.join(
                principal.userId(), request.scheduleId(), request.triageCaseId())));
    }

    @GetMapping("/me")
    public ApiResponse<List<WaitlistEntryResponse>> listMine(@LoginUser UserPrincipal principal) {
        return ApiResponse.success(waitlistService.listMine(principal.userId()).stream()
                .map(WaitlistController::toResponse).toList());
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<WaitlistAcceptedResponse> accept(@LoginUser UserPrincipal principal,
                                                         @PathVariable @Positive(message = "候补ID必须大于0") Long id) {
        Appointment appointment = waitlistService.accept(principal.userId(), id);
        return ApiResponse.success(new WaitlistAcceptedResponse(appointment.getId(), appointment.getScheduleId(),
                appointment.getDepartment(), appointment.getDate(), appointment.getTime(), appointment.getStatus(),
                appointment.getTriageCaseId()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@LoginUser UserPrincipal principal,
                                    @PathVariable @Positive(message = "候补ID必须大于0") Long id) {
        waitlistService.cancel(principal.userId(), id);
        return ApiResponse.success();
    }

    private static WaitlistEntryResponse toResponse(WaitlistEntry entry) {
        return new WaitlistEntryResponse(entry.getId(), entry.getScheduleId(), entry.getTriageCaseId(),
                entry.getAppointmentId(), entry.getPriority(), entry.getStatus(), entry.getOfferExpiresAt(),
                entry.getOfferedAt(), entry.getAcceptedAt(), entry.getCreatedAt());
    }
}
