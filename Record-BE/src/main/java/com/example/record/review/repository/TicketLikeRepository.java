package com.example.record.review.repository;

import com.example.record.review.entity.TicketLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TicketLikeRepository extends JpaRepository<TicketLike, Long> {

    /**
     * 특정 티켓에 특정 사용자가 좋아요를 눌렀는지 확인
     */
    Optional<TicketLike> findByTicket_IdAndUser_Id(Long ticketId, String userId);

    /**
     * 특정 티켓의 좋아요 개수
     */
    long countByTicket_Id(Long ticketId);

    /**
     * 특정 티켓의 좋아요 목록 (사용자 정보 포함)
     */
    @Query("SELECT tl FROM TicketLike tl JOIN FETCH tl.user WHERE tl.ticket.id = :ticketId ORDER BY tl.createdAt DESC")
    List<TicketLike> findByTicket_IdWithUser(@Param("ticketId") Long ticketId);

    /**
     * 특정 사용자가 좋아요를 누른 티켓 ID 목록
     */
    @Query("SELECT tl.ticket.id FROM TicketLike tl WHERE tl.user.id = :userId")
    List<Long> findTicketIdsByUser_Id(@Param("userId") String userId);

    /**
     * 특정 티켓의 모든 좋아요 삭제 (티켓 삭제 시 사용)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TicketLike tl WHERE tl.ticket.id = :ticketId")
    void deleteByTicket_Id(@Param("ticketId") Long ticketId);
}

