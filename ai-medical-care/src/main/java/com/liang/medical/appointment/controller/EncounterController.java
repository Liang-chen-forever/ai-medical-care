package com.liang.medical.appointment.controller;

import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.appointment.dto.CompleteEncounterRequest;
import com.liang.medical.appointment.entity.Encounter;
import com.liang.medical.appointment.service.EncounterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EncounterController {
    private final EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @PostMapping("/doctor/appointments/{id}/encounter")
    @RequireRole(UserRole.DOCTOR)
    public ApiResponse<Encounter> complete(@LoginUser UserPrincipal principal,
                                            @PathVariable @Positive(message = "预约ID必须大于0") Long id,
                                            @Valid @RequestBody CompleteEncounterRequest request) {
        return ApiResponse.success(encounterService.complete(principal.userId(), id,
                request.summary(), request.followUpAdvice()));
    }

    @GetMapping("/appointments/{id}/encounter")
    public ApiResponse<Encounter> get(@LoginUser UserPrincipal principal,
                                      @PathVariable @Positive(message = "预约ID必须大于0") Long id) {
        return ApiResponse.success(encounterService.getForUser(principal.userId(), principal.role(), id));
    }
}
