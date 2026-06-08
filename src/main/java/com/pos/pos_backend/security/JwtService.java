package com.pos.pos_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long      accessExpirationMs;
    private final long      refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}")               String accessSecret,
            @Value("${app.jwt.refresh-secret}")       String refreshSecret,
            @Value("${app.jwt.expiration-ms}")        long   accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}")long   refreshExpirationMs) {
        this.accessKey          = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey         = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs  = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // ── Access Token ──────────────────────────────────────────────────────────

    // updated — now accepts username and shopName so /me works correctly
    public String generateAccessToken(Long userId, String email, String username,
                                      String role, Long tenantId,
                                      String schemaName, String shopName) {
        return Jwts.builder()
                .subject(email)
                .claims(Map.of(
                        "userId",     userId,
                        "username",   username,    // added
                        "role",       role,
                        "tenantId",   tenantId,
                        "schemaName", schemaName,
                        "shopName",   shopName     // added
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(accessKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessKey);
    }

    public String extractEmailFromAccessToken(String token) {
        return extractClaim(token, accessKey, Claims::getSubject);
    }

    public Long extractUserIdFromAccessToken(String token) {
        return extractClaim(token, accessKey,
                claims -> claims.get("userId", Long.class));
    }

    public String extractRoleFromAccessToken(String token) {
        return extractClaim(token, accessKey,
                claims -> claims.get("role", String.class));
    }

    public Long extractTenantIdFromAccessToken(String token) {
        return extractClaim(token, accessKey,
                claims -> claims.get("tenantId", Long.class));
    }

    public String extractSchemaNameFromAccessToken(String token) {
        return extractClaim(token, accessKey,
                claims -> claims.get("schemaName", String.class));
    }

    // added — extracted from token so JwtFilter can populate CustomUserDetails
    public String extractUsernameFromAccessToken(String token) {
        return extractClaim(token, accessKey,
                claims -> claims.get("username", String.class));
    }

    // added — extracted from token so JwtFilter can populate CustomUserDetails
    public String extractShopNameFromAccessToken(String token) {
        return extractClaim(token, accessKey,
                claims -> claims.get("shopName", String.class));
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    public String generateRefreshToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("userId", userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(refreshKey)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshKey);
    }

    public String extractEmailFromRefreshToken(String token) {
        return extractClaim(token, refreshKey, Claims::getSubject);
    }

    public Long extractUserIdFromRefreshToken(String token) {
        return extractClaim(token, refreshKey,
                claims -> claims.get("userId", Long.class));
    }

    // ── Shared Internals ──────────────────────────────────────────────────────

    private boolean validateToken(String token, SecretKey key) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT empty or null: {}", e.getMessage());
        }
        return false;
    }

    private <T> T extractClaim(String token, SecretKey key,
                               Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    public boolean isTokenExpired(String token, SecretKey key) {
        try {
            Date expiration = extractClaim(token, key, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}