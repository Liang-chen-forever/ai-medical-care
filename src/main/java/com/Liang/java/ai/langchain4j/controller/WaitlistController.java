package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.LoginUser;
import com.Liang.java.ai.langchain4j.auth.RequireRole;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.waitlist.JoinWaitlistRequest;
import com.Liang.java.ai.langchain4j.dto.waitlist.WaitlistAcceptedResponse;
import com.Liang.java.ai.langchain4j.dto.waitlist.WaitlistEntryResponse;
import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.entity.WaitlistEntry;
import com.Liang.java.ai.langchain4j.service.WaitlistService;
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
