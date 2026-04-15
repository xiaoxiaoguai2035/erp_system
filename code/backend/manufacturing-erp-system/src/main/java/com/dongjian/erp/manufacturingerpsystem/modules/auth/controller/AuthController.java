package com.dongjian.erp.manufacturingerpsystem.modules.auth.controller;

import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.auth.dto.LoginRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.auth.service.AuthService;
import com.dongjian.erp.manufacturingerpsystem.modules.auth.vo.AuthProfileResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.auth.vo.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("logout success", null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthProfileResponse> me() {
        return ApiResponse.success(authService.profile());
    }
}
