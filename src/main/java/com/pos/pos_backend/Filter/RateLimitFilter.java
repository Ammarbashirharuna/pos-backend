package com.pos.pos_backend.Filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Order(1) // runs before JwtFilter — rate limit before anything else
public class RateLimitFilter extends OncePerRequestFilter {

    // Per-IP bucket maps — in-memory, fine for single instance
    private final Map<String, Bucket> loginBuckets         = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPasswordBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();
        String ip     = getClientIp(request);

        Bucket bucket = null;

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            // 10 requests per minute per IP — per spec
            bucket = loginBuckets.computeIfAbsent(ip, k -> Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(10)
                            .refillGreedy(10, Duration.ofMinutes(1))
                            .build())
                    .build());

        } else if ("POST".equals(method) && "/api/auth/forgot-password".equals(path)) {
            // 5 requests per hour per IP — per spec
            bucket = forgotPasswordBuckets.computeIfAbsent(ip, k -> Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(5)
                            .refillGreedy(5, Duration.ofHours(1))
                            .build())
                    .build());
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP {} on {}", ip, path);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{" +
                            "\"success\":false," +
                            "\"error\":\"RATE_LIMIT_EXCEEDED\"," +
                            "\"message\":\"Too many requests. Please try again later.\"," +
                            "\"timestamp\":\"" + Instant.now() + "\"" +
                            "}");
            return;
        }

        chain.doFilter(request, response);
    }

    // Respects X-Forwarded-For for Railway/Vercel proxy headers
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}