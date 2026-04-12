package com.pos.pos_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Runs once per request — extracts JWT, validates it,
     * and sets the authenticated user into Spring Security context.
     *
     * If no valid token → request continues unauthenticated.
     * SecurityConfig then decides if the endpoint requires auth.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        // No token present — let the request continue.
        // SecurityConfig will reject it if the endpoint requires auth.
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token present but invalid — reject silently.
        // Never reveal WHY the token failed (security best practice).
        if (!jwtService.validateAccessToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract claims from the token — no DB call needed.
            // All user identity data lives inside the JWT payload.
            Long userId   = jwtService.extractUserIdFromAccessToken(token);
            Long tenantId = jwtService.extractTenantIdFromAccessToken(token);
            String email  = jwtService.extractEmailFromAccessToken(token);
            String role   = jwtService.extractRoleFromAccessToken(token);

            // Build the authenticated principal Spring Security understands.
            // We attach userId and tenantId so any service can read them
            // without hitting the database again.
            CustomUserDetails userDetails = CustomUserDetails.builder()
                    .userId(userId)
                    .tenantId(tenantId)
                    .email(email)
                    .role(role)
                    .active(true)
                    .build();

            // UsernamePasswordAuthenticationToken with 3 args = authenticated.
            // With 2 args = not authenticated. Always use 3 args here.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials null — password not needed after login
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            // Attach request details (IP, session) to the auth object.
            // Useful for audit logging later.
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Store auth in SecurityContext — this is what makes the user
            // "logged in" for the duration of this request.
            // ThreadLocal storage — cleared automatically after response.
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Something unexpected happened during claim extraction.
            // Clear context to be safe and let request continue unauthenticated.
            log.error("Failed to set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts Bearer token from Authorization header.
     * Expected format: "Authorization: Bearer <token>"
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // StringUtils.hasText checks null, empty, and blank in one call
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}