package com.example.record.auth.forgot.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(String tokenHash, Instant now);
}
