package com.pos.pos_backend.controller;

import com.pos.pos_backend.dto.ApiResponse;
import com.pos.pos_backend.dto.request.ForgotPasswordRequest;
import com.pos.pos_backend.dto.request.LoginRequest;
import com.pos.pos_backend.dto.request.ResetPasswordRequest;
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

@Tag(name = "Authentication", description = "Login, logout, token refresh, password reset")
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
            HttpServletRequest request,
            HttpServletResponse response) {

        // Extract refresh token from cookie — no access token needed
        authService.logoutFromCookie(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @Operation(summary = "Get current authenticated user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> me(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // username and shopName are now populated from JWT claims via JwtFilter
        AuthResponse.UserInfo info = AuthResponse.UserInfo.builder()
                .id(userDetails.getUserId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .tenantId(userDetails.getTenantId())
                .shopName(userDetails.getShopName())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("User profile", info));
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.getEmail());
        // Always return 200 — never reveal whether the email exists
        return ResponseEntity.ok(
                ApiResponse.ok("If that email exists, a reset link has been sent", null));
    }

    @Operation(summary = "Reset password using token from email link")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully", null));
    }
}