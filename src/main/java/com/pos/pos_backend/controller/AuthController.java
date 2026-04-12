package com.pos.pos_backend.controller;

import com.pos.pos_backend.dto.ApiResponse;
import com.pos.pos_backend.dto.request.LoginRequest;
import com.pos.pos_backend.dto.response.AuthResponse;
import com.pos.pos_backend.security.CustomUserDetails;
import com.pos.pos_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Login, logout, token refresh")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse data = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", data));
    }

    @Operation(summary = "Refresh access token using HttpOnly cookie")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthResponse data = authService.refresh(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", data));
    }

    @Operation(summary = "Logout and invalidate refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) {

        authService.logout(userDetails.getUserId(), response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @Operation(summary = "Get current authenticated user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> me(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AuthResponse.UserInfo info = AuthResponse.UserInfo.builder()
                .id(userDetails.getUserId())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .tenantId(userDetails.getTenantId())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("User profile", info));
    }
}