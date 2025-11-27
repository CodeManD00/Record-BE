package com.example.record.review.repository;

import com.example.record.review.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
     * @return 해당 사용자의 티켓 목록 (공연 일시 내림차순)
     */
    @Query("SELECT t FROM Ticket t WHERE t.user.id = :userId ORDER BY t.viewDate DESC, t.createdAt DESC")
    List<Ticket> findByUser_IdOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * 특정 사용자의 공개 티켓 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 공개 티켓 목록 (공연 일시 내림차순)
     */
    @Query("SELECT t FROM Ticket t WHERE t.user.id = :userId AND t.isPublic = true ORDER BY t.viewDate DESC, t.createdAt DESC")
    List<Ticket> findPublicTicketsByUserId(@Param("userId") String userId);

    /**
     * 고급 검색: 조건에 맞는 티켓 목록 조회
     */
    @Query("SELECT t FROM Ticket t WHERE t.user.id = :userId " +
           "AND (:startDate IS NULL OR t.viewDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.viewDate <= :endDate) " +
           "AND (:genre IS NULL OR t.genre = :genre) " +
           "AND (:venue IS NULL OR t.venue LIKE %:venue%) " +
           "AND (:artist IS NULL OR t.artist LIKE %:artist%) " +
           "AND (:performanceTitle IS NULL OR t.performanceTitle LIKE %:performanceTitle%) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'viewDate' AND :sortDirection = 'ASC' THEN t.viewDate END ASC, " +
           "CASE WHEN :sortBy = 'viewDate' AND :sortDirection = 'DESC' THEN t.viewDate END DESC, " +
           "CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'ASC' THEN t.createdAt END ASC, " +
           "CASE WHEN :sortBy IS NULL OR :sortBy = 'createdAt' THEN t.viewDate END DESC")
    List<Ticket> searchTickets(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("genre") String genre,
            @Param("venue") String venue,
            @Param("artist") String artist,
            @Param("performanceTitle") String performanceTitle,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection
    );

    /**
     * 특정 연도의 티켓 개수 조회
     * PostgreSQL: EXTRACT(YEAR FROM date)
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.id = :userId AND EXTRACT(YEAR FROM t.viewDate) = :year")
    long countByUserAndYear(@Param("userId") String userId, @Param("year") int year);

    /**
     * 월별 관람 추이 조회
     * PostgreSQL: EXTRACT(YEAR/MONTH FROM date)
     */
    @Query(value = "SELECT EXTRACT(YEAR FROM t.view_date) as year, EXTRACT(MONTH FROM t.view_date) as month, COUNT(t.id) as count " +
           "FROM tickets t " +
           "WHERE t.user_id = :userId AND EXTRACT(YEAR FROM t.view_date) = :year " +
           "GROUP BY EXTRACT(YEAR FROM t.view_date), EXTRACT(MONTH FROM t.view_date) " +
           "ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyTrend(@Param("userId") String userId, @Param("year") int year);

    /**
     * 장르별 통계 조회
     * PostgreSQL: EXTRACT(YEAR FROM date)
     */
    @Query("SELECT t.genre, COUNT(t) as count FROM Ticket t " +
           "WHERE t.user.id = :userId AND EXTRACT(YEAR FROM t.viewDate) = :year " +
           "GROUP BY t.genre ORDER BY count DESC")
    List<Object[]> getGenreStatistics(@Param("userId") String userId, @Param("year") int year);

    /**
     * 공연장별 통계 조회 (TOP N)
     * PostgreSQL: EXTRACT(YEAR FROM date)
     */
    @Query("SELECT t.venue, COUNT(t) as count FROM Ticket t " +
           "WHERE t.user.id = :userId AND EXTRACT(YEAR FROM t.viewDate) = :year AND t.venue IS NOT NULL " +
           "GROUP BY t.venue ORDER BY count DESC")
    List<Object[]> getVenueStatistics(@Param("userId") String userId, @Param("year") int year);

    /**
     * 작품별 통계 조회 (TOP N)
     * PostgreSQL: EXTRACT(YEAR FROM date)
     */
    @Query("SELECT t.performanceTitle, COUNT(t) as count FROM Ticket t " +
           "WHERE t.user.id = :userId AND EXTRACT(YEAR FROM t.viewDate) = :year " +
           "GROUP BY t.performanceTitle ORDER BY count DESC")
    List<Object[]> getPerformanceStatistics(@Param("userId") String userId, @Param("year") int year);

    /**
     * 아티스트별 통계 조회 (TOP N)
     * PostgreSQL: EXTRACT(YEAR FROM date)
     */
    @Query("SELECT t.artist, COUNT(t) as count FROM Ticket t " +
           "WHERE t.user.id = :userId AND EXTRACT(YEAR FROM t.viewDate) = :year AND t.artist IS NOT NULL " +
           "GROUP BY t.artist ORDER BY count DESC")
    List<Object[]> getArtistStatistics(@Param("userId") String userId, @Param("year") int year);

    /**
     * 요일별 통계 조회
     * PostgreSQL: EXTRACT(DOW FROM date) - 0(일요일)~6(토요일)
     */
    @Query(value = "SELECT EXTRACT(DOW FROM t.view_date) as dayOfWeek, COUNT(t.id) as count FROM tickets t " +
           "WHERE t.user_id = :userId AND EXTRACT(YEAR FROM t.view_date) = :year " +
           "GROUP BY EXTRACT(DOW FROM t.view_date)", nativeQuery = true)
    List<Object[]> getDayOfWeekStatistics(@Param("userId") String userId, @Param("year") int year);
}