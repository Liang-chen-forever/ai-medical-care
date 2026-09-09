package com.liang.medical.controller;

import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.dto.triage.*;
import com.liang.medical.service.TriageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/triage/cases")
@RequireRole(UserRole.PATIENT)
public class TriageController {
    private final TriageService triageService;
    public TriageController(TriageService triageService) { this.triageService = triageService; }

    @PostMapping
    public ApiResponse<TriageCaseResponse> create(@LoginUser UserPrincipal principal,
                                                    @Valid @RequestBody CreateTriageCaseRequest request) {
        return ApiResponse.success(triageService.create(principal.userId(), request.chiefComplaint().trim()));
    }
    @GetMapping
    public ApiResponse<List<TriageCaseSummaryResponse>> list(@LoginUser UserPrincipal principal) {
        return ApiResponse.success(triageService.listMine(principal.userId()));
    }
    @GetMapping("/{id}")
    public ApiResponse<TriageCaseResponse> get(@LoginUser UserPrincipal principal,
                                                @PathVariable @Positive(message = "分诊ID必须大于0") Long id) {
        return ApiResponse.success(triageService.getMine(principal.userId(), id));
    }
}
