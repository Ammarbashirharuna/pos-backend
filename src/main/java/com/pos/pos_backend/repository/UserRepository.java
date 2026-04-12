package com.pos.pos_backend.repository;

import com.pos.pos_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String token);
    boolean existsByEmail(String email);

    // Single query update — avoids loading the full entity just to update one field
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :time WHERE u.id = :id")
    void updateLastLogin(Long id, LocalDateTime time);

    @Modifying
    @Query("UPDATE User u SET u.refreshTokenHash = :hash WHERE u.id = :id")
    void updateRefreshTokenHash(Long id, String hash);
}