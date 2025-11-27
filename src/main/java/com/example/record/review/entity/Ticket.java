package com.example.record.review.entity;

import com.example.record.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 티켓을 소유한 사용자와의 관계를 설정합니다.
     * 
     * 왜 이렇게 변경했나요?
     * 1. 데이터베이스 무결성: 외래키(FK) 관계를 통해 데이터의 일관성을 보장합니다.
     *    예를 들어, 존재하지 않는 사용자 ID로 티켓을 만들 수 없게 됩니다.
     * 
     * 2. 조인 쿼리 최적화: 사용자 정보와 티켓 정보를 한 번에 가져올 수 있어서
     *    데이터베이스 성능이 향상됩니다.
     * 
     * 3. 객체지향 설계: 코드에서 ticket.getUser().getNickname()처럼 
     *    자연스럽게 사용자 정보에 접근할 수 있습니다.
     * 
     * @JoinColumn: 데이터베이스의 user_id 컬럼과 연결
     * @ManyToOne: 여러 티켓이 하나의 사용자에게 속할 수 있음을 의미
     * FetchType.LAZY: 사용자 정보가 필요할 때만 데이터베이스에서 가져옴 (성능 최적화)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private User user;

    @Column(name = "performance_title", nullable = false, length = 100)
    private String performanceTitle;

    @Column(name = "venue", length = 100)
    private String venue;  // 공연장 (theater에서 변경)

    @Column(name = "seat", length = 50)
    private String seat;  // 좌석

    @Column(name = "artist", length = 100)
    private String artist;  // 아티스트

    @Column(name = "poster_url", length = 400)
    private String posterUrl;

    @Column(name = "genre", length = 20)
    private String genre;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    @Column(name = "image_prompt", columnDefinition = "TEXT")
    private String imagePrompt;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    /**
     * 티켓 생성 시간
     * 
     * @CreationTimestamp: 데이터베이스에 저장할 때 자동으로 현재 시간 설정
     * 이는 DB의 DEFAULT now()와 동일한 효과를 가집니다.
     */
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 티켓 수정 시간
     * 
     * @UpdateTimestamp: 데이터베이스에 수정사항을 저장할 때 자동으로 현재 시간 설정
     * 이는 애플리케이션에서 수동으로 업데이트하는 것과 동일한 효과를 가집니다.
     */
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}