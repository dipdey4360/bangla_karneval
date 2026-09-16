package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.AdminRegisterRequest;
import com.bangla.karneval.dto.request.ChangePasswordRequest;
import com.bangla.karneval.dto.request.LoginRequest;
import com.bangla.karneval.dto.response.AuthResponse;
import com.bangla.karneval.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AdminRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        authService.changePassword(email, request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }

}
