package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.LoginUser;
import com.Liang.java.ai.langchain4j.auth.RequireRole;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.triage.*;
import com.Liang.java.ai.langchain4j.service.TriageService;
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
