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
     * 사용자의 선호 키워드 (후기 분석 결과)
     * 
     * 역할: 사용자가 작성한 후기들을 분석하여 추출한 주요 키워드들을 저장
     * 
     * 저장 형식: 쉼표로 구분된 키워드 문자열 (예: "연기,음악,무대연출,스토리")
     * 
     * 업데이트 시점:
     * - 사용자의 후기가 3개, 6개, 9개... 이런 식으로 3개씩 늘어날 때마다
     * - 새로 추가된 3개 후기를 분석하여 키워드를 추출하고 업데이트
     * 
     * 사용 목적:
     * - 맞춤형 질문 생성: 사용자가 자주 언급하는 주제와 관련된 질문을 생성
     * - 개인화된 경험: 사용자의 관심사에 맞는 질문 제공
     */
    @Column(columnDefinition = "TEXT")
    private String favorite;

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