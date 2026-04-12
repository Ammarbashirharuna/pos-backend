package com.pos.pos_backend.repository;

import com.pos.pos_backend.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByOwnerEmail(String email);
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findByVerificationToken(String token);
    boolean existsByOwnerEmail(String email);
    boolean existsBySlug(String slug);
}