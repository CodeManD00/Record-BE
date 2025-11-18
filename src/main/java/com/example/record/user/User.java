package com.example.record.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
    @Column(length = 15)
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String email;

    @Column(length = 300, nullable = false)
    private String password;

    @Column(length = 30, nullable = false)
    private String nickname;

    @Column(length = 10, nullable = false)
    @Builder.Default
    private String role = "USER";

    @Column(columnDefinition = "TEXT")
    private String favorite;

    // ✅ 프로필 이미지 (로컬에 저장된 파일 URL 또는 경로)
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
