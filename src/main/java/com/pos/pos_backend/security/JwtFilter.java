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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.validateAccessToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long   userId   = jwtService.extractUserIdFromAccessToken(token);
            Long   tenantId = jwtService.extractTenantIdFromAccessToken(token);
            String email    = jwtService.extractEmailFromAccessToken(token);
            String role     = jwtService.extractRoleFromAccessToken(token);
            String schema   = jwtService.extractSchemaNameFromAccessToken(token);
            String username = jwtService.extractUsernameFromAccessToken(token); // added
            String shopName = jwtService.extractShopNameFromAccessToken(token); // added

            // Set tenant schema in ThreadLocal so Hibernate routes to correct schema
            TenantContext.setTenantSchema(schema);

            CustomUserDetails userDetails = CustomUserDetails.builder()
                    .userId(userId)
                    .tenantId(tenantId)
                    .email(email)
                    .username(username)  // added
                    .shopName(shopName)  // added
                    .role(role)
                    .schemaName(schema)
                    .active(true)
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("Failed to set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear tenant context after request — prevents schema leaking
            TenantContext.clear();
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}