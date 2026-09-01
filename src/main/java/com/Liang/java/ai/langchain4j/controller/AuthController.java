package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.auth.AuthResponse;
import com.Liang.java.ai.langchain4j.dto.auth.LoginRequest;
import com.Liang.java.ai.langchain4j.dto.auth.RegisterRequest;
import com.Liang.java.ai.langchain4j.entity.User;
import com.Liang.java.ai.langchain4j.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public AuthController(UserService userService, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request);
        String token = jwtTokenService.createToken(new UserPrincipal(user.getId(), user.getUsername()));
        return ApiResponse.success(new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getIdCard(),
                user.getPhone()
        ));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.success();
    }
}
