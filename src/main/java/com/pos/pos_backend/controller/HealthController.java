package com.pos.pos_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("{\"status\":\"UP\"}");
    }

    // TEMPORARY — delete after first login
    @GetMapping("/api/seed")
    public ResponseEntity<String> seed() {
        try {
            // Create tenant
            jdbcTemplate.execute("""
                INSERT INTO tenants (shop_name, slug, owner_email, schema_name, status)
                VALUES ('Sovent Test', 'sovent-test', 'admin@sovent.com', 'tenant_sovent', 'ACTIVE')
                ON CONFLICT (slug) DO NOTHING;
            """);

            // Create tenant schema
            jdbcTemplate.execute("SELECT create_tenant_schema('tenant_sovent')");

            // Hash password
            String hash = passwordEncoder.encode("Admin@123456");

            // Insert admin user
            jdbcTemplate.execute(String.format("""
                INSERT INTO tenant_sovent.users 
                (username, full_name, email, password_hash, role, is_active)
                VALUES ('admin', 'Admin User', 'admin@sovent.com', '%s', 'ADMIN', true)
                ON CONFLICT (email) DO NOTHING;
            """, hash));

            return ResponseEntity.ok("Admin seeded. Login with admin@sovent.com / Admin@123456. DELETE THIS ENDPOINT NOW.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}