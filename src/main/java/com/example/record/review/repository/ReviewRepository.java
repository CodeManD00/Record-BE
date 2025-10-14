package com.example.record.review.repository;

import com.example.record.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    /**
     * 특정 사용자의 리뷰를 페이지네이션과 함께 조회합니다.
     * 
     * 메서드명 변경 이유:
     * - findByTicket_UserId → findByTicket_User_Id
     * - Ticket 엔티티에서 userId(String) → user(User 객체)로 변경했기 때문
     * - JPA는 객체 관계를 통해 쿼리를 생성하므로, user.id로 접근해야 합니다.
     * 
     * 이 메서드는 다음과 같은 SQL을 생성합니다:
     * SELECT * FROM reviews r 
     * JOIN tickets t ON r.ticket_id = t.id 
     * WHERE t.user_id = ? 
     * ORDER BY ... LIMIT ... OFFSET ...
     */
    Page<Review> findByTicket_User_Id(String userId, Pageable pageable);
}