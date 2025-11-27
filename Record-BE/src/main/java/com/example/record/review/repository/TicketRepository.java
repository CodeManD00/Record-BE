package com.example.record.review.repository;

import com.example.record.review.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 티켓 레포지토리
 * 
 * 역할: 티켓을 데이터베이스에서 조회하고 관리
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * 특정 사용자의 티켓 개수를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 티켓 개수
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.id = :userId")
    long countByUser_Id(@Param("userId") String userId);

    /**
     * 특정 사용자의 티켓 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 티켓 목록 (생성 시간 내림차순)
     */
    @Query("SELECT t FROM Ticket t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Ticket> findByUser_IdOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * 특정 사용자의 공개 티켓 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 공개 티켓 목록 (생성 시간 내림차순)
     */
    @Query("SELECT t FROM Ticket t WHERE t.user.id = :userId AND t.isPublic = true ORDER BY t.createdAt DESC")
    List<Ticket> findPublicTicketsByUserId(@Param("userId") String userId);
}