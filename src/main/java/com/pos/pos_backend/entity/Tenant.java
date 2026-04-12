package com.pos.pos_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", schema = "public")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(unique = true, nullable = false, length = 50)
    private String slug;

    @Column(name = "owner_email", unique = true, nullable = false, length = 100)
    private String ownerEmail;

    @Column(name = "schema_name", unique = true, nullable = false, length = 60)
    private String schemaName;

    // PENDING → TRIAL → ACTIVE → SUSPENDED → CANCELLED
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}