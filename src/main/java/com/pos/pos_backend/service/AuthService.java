package com.pos.pos_backend.service;

import com.pos.pos_backend.dto.request.ForgotPasswordRequest;
import com.pos.pos_backend.dto.request.LoginRequest;
import com.pos.pos_backend.dto.request.ResetPasswordRequest;
import com.pos.pos_backend.dto.response.AuthResponse;
import com.pos.pos_backend.exception.AuthException;
import com.pos.pos_backend.security.JwtService;
import com.pos.pos_backend.security.TenantContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final JwtService      jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate    jdbcTemplate;
    private final EmailService    emailService; // wired in — sends reset emails via Resend

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public AuthService(JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbcTemplate,
                       EmailService emailService) {
        this.jwtService      = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate    = jdbcTemplate;
        this.emailService    = emailService;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {

        Map<String, Object> tenant     = findTenantByEmail(request.getEmail());
        String              schemaName = (String) tenant.get("schema_name");
        Long                tenantId   = ((Number) tenant.get("id")).longValue();
        String              shopName   = (String) tenant.get("shop_name");
        String              status     = (String) tenant.get("status");

        if ("SUSPENDED".equals(status)) {
            throw new AuthException("Account suspended. Contact support.");
        }

        Map<String, Object> userData       = findUserByEmail(schemaName, request.getEmail());
        Long                userId         = ((Number) userData.get("id")).longValue();
        String              username       = (String)  userData.get("username");
        String              passwordHash   = (String)  userData.get("password_hash");
        String              role           = (String)  userData.get("role");
        Boolean             isActive       = (Boolean) userData.get("is_active");
        int                 failedAttempts = ((Number) userData.get("failed_login_attempts")).intValue();
        Object              lockedUntilRaw = userData.get("locked_until");

        if (!Boolean.TRUE.equals(isActive)) {
            throw new AuthException("Account is inactive.");
        }

        if (lockedUntilRaw != null) {
            LocalDateTime lockedUntil =
                    ((java.sql.Timestamp) lockedUntilRaw).toLocalDateTime();
            if (LocalDateTime.now().isBefore(lockedUntil)) {
                throw new AuthException("Account locked. Try again later.");
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), passwordHash)) {
            incrementFailedAttempts(schemaName, userId, failedAttempts);
            throw new AuthException("Invalid email or password");
        }

        resetFailedAttempts(schemaName, userId);
        updateLastLogin(schemaName, userId);

        TenantContext.setTenantSchema(schemaName);

        String accessToken  = jwtService.generateAccessToken(
                userId, request.getEmail(), username, role, tenantId, schemaName, shopName);
        String refreshToken = jwtService.generateRefreshToken(userId, request.getEmail());

        saveRefreshTokenHash(schemaName, userId, refreshToken);
        setRefreshTokenCookie(response, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900)
                .user(AuthResponse.UserInfo.builder()
                        .id(userId)
                        .username(username)
                        .email(request.getEmail())
                        .role(role)
                        .tenantId(tenantId)
                        .shopName(shopName)
                        .build())
                .build();
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

        Map<String, Object> tenant     = findTenantByEmail(email);
        String              schemaName = (String) tenant.get("schema_name");
        Long                tenantId   = ((Number) tenant.get("id")).longValue();
        String              shopName   = (String) tenant.get("shop_name");

        Map<String, Object> userData   = findUserByEmail(schemaName, email);
        Long                userId     = ((Number) userData.get("id")).longValue();
        String              username   = (String)  userData.get("username");
        String              role       = (String)  userData.get("role");
        String              storedHash = (String)  userData.get("refresh_token_hash");

        if (storedHash == null || !MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                hashToken(refreshToken).getBytes(StandardCharsets.UTF_8))) {
            throw new AuthException("Refresh token mismatch");
        }

        TenantContext.setTenantSchema(schemaName);

        String newAccessToken  = jwtService.generateAccessToken(
                userId, email, username, role, tenantId, schemaName, shopName);
        String newRefreshToken = jwtService.generateRefreshToken(userId, email);

        saveRefreshTokenHash(schemaName, userId, newRefreshToken);
        setRefreshTokenCookie(response, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(900)
                .user(AuthResponse.UserInfo.builder()
                        .id(userId)
                        .username(username)
                        .email(email)
                        .role(role)
                        .tenantId(tenantId)
                        .shopName(shopName)
                        .build())
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(Long userId, String schemaName, HttpServletResponse response) {
        jdbcTemplate.update(
                "UPDATE \"" + schemaName + "\".users " +
                        "SET refresh_token_hash = NULL WHERE id = ?", userId);

        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String email) {
        // Always return success — never reveal whether the email exists
        try {
            Map<String, Object> tenant     = findTenantByEmail(email);
            String              schemaName = (String) tenant.get("schema_name");

            Map<String, Object> user   = findUserByEmail(schemaName, email);
            Long                userId = ((Number) user.get("id")).longValue();

            // Generate a secure random token
            String        rawToken  = generateSecureToken();
            String        tokenHash = hashToken(rawToken);
            LocalDateTime expires   = LocalDateTime.now().plusHours(1); // 1hr TTL per spec

            // Save the HASH to DB — never store raw tokens
            jdbcTemplate.update(
                    "UPDATE \"" + schemaName + "\".users " +
                            "SET password_reset_token = ?, " +
                            "    password_reset_expires = ?, " +
                            "    reset_token_used = FALSE " +
                            "WHERE id = ?",
                    tokenHash,
                    java.sql.Timestamp.valueOf(expires),
                    userId);

            // Send the RAW token in the email link — user clicks it, we hash and compare
            emailService.sendPasswordReset(email, rawToken);

            log.info("Password reset email sent for user {} in schema {}", userId, schemaName);

        } catch (AuthException e) {
            // Swallow silently — don't reveal that the email doesn't exist
            log.warn("Forgot password for unknown email: {}", email);
        }
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken());

        // Scan all active tenant schemas to find the token
        List<Map<String, Object>> tenants = jdbcTemplate.queryForList(
                "SELECT schema_name FROM public.tenants WHERE status != 'SUSPENDED'");

        for (Map<String, Object> tenant : tenants) {
            String schema = (String) tenant.get("schema_name");
            try {
                Map<String, Object> user = jdbcTemplate.queryForMap(
                        "SELECT id, password_reset_expires, reset_token_used " +
                                "FROM \"" + schema + "\".users " +
                                "WHERE password_reset_token = ?", tokenHash);

                Boolean used = (Boolean) user.get("reset_token_used");
                if (Boolean.TRUE.equals(used)) {
                    throw new AuthException("Reset token already used");
                }

                Object expiresRaw = user.get("password_reset_expires");
                if (expiresRaw == null) {
                    throw new AuthException("Invalid reset token");
                }

                LocalDateTime expires =
                        ((java.sql.Timestamp) expiresRaw).toLocalDateTime();
                if (LocalDateTime.now().isAfter(expires)) {
                    throw new AuthException("Reset token expired. Please request a new one.");
                }

                Long   userId  = ((Number) user.get("id")).longValue();
                String newHash = passwordEncoder.encode(request.getNewPassword());

                // Update password + mark token used + invalidate all sessions
                jdbcTemplate.update(
                        "UPDATE \"" + schema + "\".users " +
                                "SET password_hash = ?, " +
                                "    reset_token_used = TRUE, " +
                                "    password_reset_token = NULL, " +
                                "    refresh_token_hash = NULL " +
                                "WHERE id = ?",
                        newHash, userId);

                log.info("Password reset successful for user {} in schema {}", userId, schema);
                return;

            } catch (AuthException e) {
                throw e;
            } catch (Exception e) {
                // Token not in this schema — continue scanning
            }
        }

        throw new AuthException("Invalid or expired reset token");
    }

    // ── Logout from cookie (alternative logout using cookie directly) ─────────

    @Transactional
    public void logoutFromCookie(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> refreshTokenOpt = extractRefreshTokenFromCookie(request);

        if (refreshTokenOpt.isPresent()) {
            String refreshToken = refreshTokenOpt.get();
            if (jwtService.validateRefreshToken(refreshToken)) {
                String email = jwtService.extractEmailFromRefreshToken(refreshToken);
                try {
                    Map<String, Object> tenant = findTenantByEmail(email);
                    String schemaName = (String) tenant.get("schema_name");
                    Map<String, Object> userData = findUserByEmail(schemaName, email);
                    Long userId = ((Number) userData.get("id")).longValue();
                    jdbcTemplate.update(
                            "UPDATE \"" + schemaName + "\".users " +
                                    "SET refresh_token_hash = NULL WHERE id = ?", userId);
                } catch (Exception e) {
                    log.warn("Could not clear refresh token from DB during logout: {}", e.getMessage());
                }
            }
        }

        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // ── JDBC helpers ──────────────────────────────────────────────────────────

    private Map<String, Object> findTenantByEmail(String email) {
        String sql = "SELECT id, shop_name, schema_name, status " +
                "FROM public.tenants WHERE owner_email = ?";
        try {
            log.debug("Looking up tenant for email: '{}'", email);
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, email);
            log.debug("Found tenant: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Tenant lookup failed for email '{}': {}", email, e.getMessage());
            throw new AuthException("Invalid email or password");
        }
    }

    private Map<String, Object> findUserByEmail(String schema, String email) {
        String sql = "SELECT id, username, full_name, password_hash, role, " +
                "is_active, failed_login_attempts, locked_until, " +
                "refresh_token_hash " +
                "FROM \"" + schema + "\".users WHERE email = ?";
        try {
            log.debug("Looking up user in schema '{}' for email: '{}'", schema, email);
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, email);
            log.debug("Found user: id={}, role={}, is_active={}",
                    result.get("id"), result.get("role"), result.get("is_active"));
            return result;
        } catch (Exception e) {
            log.error("User lookup failed in schema '{}' for email '{}': {}",
                    schema, email, e.getMessage());
            throw new AuthException("Invalid email or password");
        }
    }

    private void incrementFailedAttempts(String schema, Long userId, int current) {
        int next = current + 1;
        if (next >= 5) {
            jdbcTemplate.update(
                    "UPDATE \"" + schema + "\".users " +
                            "SET failed_login_attempts = ?, locked_until = ? WHERE id = ?",
                    next,
                    java.sql.Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)),
                    userId);
        } else {
            jdbcTemplate.update(
                    "UPDATE \"" + schema + "\".users " +
                            "SET failed_login_attempts = ? WHERE id = ?",
                    next, userId);
        }
    }

    private void resetFailedAttempts(String schema, Long userId) {
        jdbcTemplate.update(
                "UPDATE \"" + schema + "\".users " +
                        "SET failed_login_attempts = 0, locked_until = NULL WHERE id = ?",
                userId);
    }

    private void updateLastLogin(String schema, Long userId) {
        jdbcTemplate.update(
                "UPDATE \"" + schema + "\".users SET last_login = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.now()), userId);
    }

    private void saveRefreshTokenHash(String schema, Long userId, String rawToken) {
        jdbcTemplate.update(
                "UPDATE \"" + schema + "\".users " +
                        "SET refresh_token_hash = ? WHERE id = ?",
                hashToken(rawToken), userId);
    }

    /**
     * SHA-256 hash for refresh tokens and reset tokens.
     * BCrypt is for passwords (short secrets, needs slowness).
     * SHA-256 is for tokens (long random strings, already high entropy).
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[]        hash   = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    private Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}