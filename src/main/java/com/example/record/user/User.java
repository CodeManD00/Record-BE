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
    private String id;  // VARCHAR(15)로 변경

    @Column(length = 30, nullable = false, unique = true)
    private String email;

    @Column(length = 300, nullable = false)
    private String password;

    @Column(length = 30, nullable = false)
    private String nickname;

    /**
     * 사용자 역할
     * 
     * 변경 사항:
     * - nullable = false 추가
     * - 이유: DB 스키마에서 role이 NOT NULL로 정의되어 있기 때문
     * 
     * 기본값: "USER" (일반 사용자)
     */
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String role = "USER";

    /**
     * 사용자 생성 시간
     * 
     * @CreationTimestamp: 데이터베이스에 저장할 때 자동으로 현재 시간 설정
     * 이는 DB의 DEFAULT now()와 동일한 효과를 가집니다.
     */
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 사용자 수정 시간
     * 
     * @UpdateTimestamp: 데이터베이스에 수정사항을 저장할 때 자동으로 현재 시간 설정
     * 이는 애플리케이션에서 수동으로 업데이트하는 것과 동일한 효과를 가집니다.
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}