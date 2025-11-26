// ApiKey: 사용자별 OpenAI API 키를 저장하는 엔티티 클래스입니다.

package com.example.record;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 ID
    private Long id;

    @Column(name = "user_id", length = 255)
    private String userId;  // API 키를 등록한 사용자 ID

    @Column(name = "api_key", length = 255)
    private String apiKey;  // 실제로 사용할 OpenAI API 키

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt; // 등록 시각

    // 생성자: 사용자 ID와 API 키를 받아 초기화
    public ApiKey(String userId, String apiKey) {
        this.userId = userId;
        this.apiKey = apiKey;
    }
}
