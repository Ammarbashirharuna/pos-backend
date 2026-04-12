package com.pos.pos_backend.service;

import com.pos.pos_backend.dto.request.LoginRequest;
import com.pos.pos_backend.dto.response.AuthResponse;
import com.pos.pos_backend.entity.Tenant;
import com.pos.pos_backend.entity.User;
import com.pos.pos_backend.exception.AuthException;
import com.pos.pos_backend.repository.TenantRepository;
import com.pos.pos_backend.repository.UserRepository;
import com.pos.pos_backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {

        // Load user — same error for wrong email or wrong password (prevent enumeration)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (user.isLocked()) {
            throw new AuthException("Account locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new AuthException("Invalid email or password");
        }

        // Load tenant to include shop name in response
        Tenant tenant = tenantRepository.findById(user.getId())
                .orElseThrow(() -> new AuthException("Tenant not found"));

        if ("SUSPENDED".equals(tenant.getStatus())) {
            throw new AuthException("Account suspended. Contact support.");
        }

        // Successful login — reset lockout and update last login
        user.resetFailedAttempts();
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), tenant.getId());

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(), user.getEmail());

        // Hash refresh token before storing — raw token never touches DB
        String refreshTokenHash = passwordEncoder.encode(refreshToken);
        userRepository.updateRefreshTokenHash(user.getId(), refreshTokenHash);

        // Set refresh token as HttpOnly cookie — JS cannot read it
        setRefreshTokenCookie(response, refreshToken);

        return buildAuthResponse(user, tenant, accessToken);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(HttpServletRequest request,
                                HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new AuthException("Refresh token missing"));

        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmailFromRefreshToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));

        // Verify stored hash matches the incoming token
        if (!passwordEncoder.matches(refreshToken, user.getRefreshTokenHash())) {
            throw new AuthException("Refresh token mismatch");
        }

        Tenant tenant = tenantRepository.findById(user.getId())
                .orElseThrow(() -> new AuthException("Tenant not found"));

        // Rotate refresh token — old one is invalidated
        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), tenant.getId());

        String newRefreshToken = jwtService.generateRefreshToken(
                user.getId(), user.getEmail());

        userRepository.updateRefreshTokenHash(
                user.getId(), passwordEncoder.encode(newRefreshToken));

        setRefreshTokenCookie(response, newRefreshToken);

        return buildAuthResponse(user, tenant, newAccessToken);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        // Invalidate stored refresh token
        userRepository.updateRefreshTokenHash(userId, null);

        // Clear the cookie by setting max age to 0
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setRefreshTokenCookie(HttpServletResponse response,
                                       String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);   // JS cannot access
        cookie.setSecure(true);     // HTTPS only
        cookie.setPath("/");        // available to all paths
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    private Optional<String> extractRefreshTokenFromCookie(
            HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private AuthResponse buildAuthResponse(User user, Tenant tenant,
                                           String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .tenantId(tenant.getId())
                        .shopName(tenant.getShopName())
                        .build())
                .build();
    }
}