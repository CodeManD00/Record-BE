// User: 회원 정보를 저장하는 엔티티 클래스. 이메일, 비밀번호, 닉네임, 역할, 생성/수정 시간을 포함합니다.

package com.example.record.DB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 ID
    private Long id;

    @Column(length = 30, nullable = false, unique = true)
    private String email; // 사용자 이메일 (유일)

    @Column(length = 30, nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(length = 30, nullable = false)
    private String nickname; // 사용자 닉네임

    @Column(length = 10, nullable = false)
    private String role = "USER"; // 사용자 역할 (기본값: USER)

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt; // 생성 시간

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt; // 마지막 수정 시간

    // 엔티티 생성 시 자동으로 시간 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // 엔티티 수정 시 수정 시간 갱신
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
