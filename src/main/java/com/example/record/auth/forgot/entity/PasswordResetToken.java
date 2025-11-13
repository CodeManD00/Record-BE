package com.example.record.auth.forgot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name="idx_prt_token_hash", columnList = "tokenHash"),
        @Index(name="idx_prt_expires_at", columnList = "expiresAt")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 64)
    private String userId;          // ⬅️ Long → String

    @Column(nullable = false, length = 128)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdAt;
}
