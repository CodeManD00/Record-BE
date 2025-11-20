package com.example.record.auth.email;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 인증 대상 이메일 */
    @Column(nullable = false, length = 255)
    private String email;

    /** 6자리 인증코드 */
    @Column(nullable = false, length = 6)
    private String code;

    /** 만료 시각 (예: 발급 후 10분) */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 이미 인증 성공했는지 여부 */
    @Column(nullable = false)
    private boolean verified;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
