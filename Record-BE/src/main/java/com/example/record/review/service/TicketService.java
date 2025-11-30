package com.example.record.review.service;

import com.example.record.review.dto.request.TicketCreateRequest;
import com.example.record.review.dto.request.TicketSearchRequest;
import com.example.record.review.dto.request.TicketUpdateRequest;
import com.example.record.review.dto.response.*;
import com.example.record.review.entity.Ticket;
import com.example.record.review.repository.TicketRepository;
import com.example.record.review.repository.TicketLikeRepository;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketLikeRepository ticketLikeRepository;

    /**
     * 티켓 생성
     * @param request 티켓 생성 요청 (imageUrl 포함)
     * @return 생성된 티켓 정보
     */
    @Transactional
    public TicketCreateResponse createTicket(TicketCreateRequest request) {
        // 사용자 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: id=" + request.getUserId()));

        // 이미지 URL에서 쿼리 파라미터 제거 (DB에는 순수 경로만 저장)
        String cleanImageUrl = request.getImageUrl();
        if (cleanImageUrl != null && cleanImageUrl.contains("?")) {
            cleanImageUrl = cleanImageUrl.split("\\?")[0];
            log.info("이미지 URL 쿼리 파라미터 제거: {} -> {}", request.getImageUrl(), cleanImageUrl);
        }

        // 티켓 생성
        Ticket ticket = Ticket.builder()
                .user(user)
                .performanceTitle(request.getPerformanceTitle())
                .venue(request.getVenue())
                .seat(request.getSeat())
                .artist(request.getArtist())
                .posterUrl(request.getPosterUrl())
                .genre(request.getGenre())
                .viewDate(request.getViewDate())
                .imageUrl(cleanImageUrl)  // 쿼리 파라미터가 제거된 이미지 URL 저장
                .imagePrompt(request.getImagePrompt())  // 이미지 프롬프트 저장
                .reviewText(request.getReviewText())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        log.info("티켓 생성 완료: ticketId={}, userId={}, imageUrl={}", 
                saved.getId(), request.getUserId(), request.getImageUrl());

        return TicketCreateResponse.builder()
                .ticketId(saved.getId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * 사용자의 티켓 목록 조회 (전체 - 본인 조회용)
     * @param userId 사용자 ID
     * @return 해당 사용자의 티켓 목록
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByUserId(String userId) {
        List<Ticket> tickets = ticketRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        // LAZY 로딩을 트랜잭션 내에서 강제로 로드
        tickets.forEach(t -> t.getUser().getId());
        return tickets.stream()
                .map(TicketResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 공개 티켓 목록 조회 (친구 조회용)
     * @param userId 사용자 ID
     * @return 해당 사용자의 공개 티켓 목록
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getPublicTicketsByUserId(String userId, String currentUserId) {
        log.info("🔍 공개 티켓 조회 시작: userId={}, currentUserId={}", userId, currentUserId);
        List<Ticket> tickets = ticketRepository.findPublicTicketsByUserId(userId);
        log.info("✅ 공개 티켓 조회 완료: userId={}, count={}", userId, tickets.size());
        // LAZY 로딩을 트랜잭션 내에서 강제로 로드
        tickets.forEach(t -> t.getUser().getId());
        return tickets.stream()
                .map(ticket -> {
                    boolean isLiked = currentUserId != null && 
                        ticketLikeRepository.findByTicket_IdAndUser_Id(ticket.getId(), currentUserId).isPresent();
                    long likeCount = ticketLikeRepository.countByTicket_Id(ticket.getId());
                    return TicketResponse.from(ticket, isLiked, likeCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 티켓 수정
     * @param ticketId 수정할 티켓 ID
     * @param requesterUserId 요청하는 사용자 ID
     * @param request 티켓 수정 요청
     * @throws IllegalArgumentException 티켓이 없거나 권한이 없는 경우
     */
    @Transactional
    public void updateTicket(Long ticketId, String requesterUserId, TicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓이 없습니다. id=" + ticketId));
        
        // 티켓 수정 권한 확인
        if (!ticket.getUser().getId().equals(requesterUserId)) {
            throw new SecurityException("본인 티켓만 수정 가능합니다.");
        }
        
        // 필드 업데이트 (null이 아닌 경우만)
        if (request.getPerformanceTitle() != null) {
            ticket.setPerformanceTitle(request.getPerformanceTitle());
        }
        if (request.getVenue() != null) {
            ticket.setVenue(request.getVenue());
        }
        if (request.getSeat() != null) {
            ticket.setSeat(request.getSeat());
        }
        if (request.getArtist() != null) {
            ticket.setArtist(request.getArtist());
        }
        if (request.getPosterUrl() != null) {
            ticket.setPosterUrl(request.getPosterUrl());
        }
        if (request.getGenre() != null) {
            ticket.setGenre(request.getGenre());
        }
        if (request.getViewDate() != null) {
            ticket.setViewDate(request.getViewDate());
        }
        if (request.getImageUrl() != null) {
            // 이미지 URL에서 쿼리 파라미터 제거 (DB에는 순수 경로만 저장)
            String cleanImageUrl = request.getImageUrl();
            if (cleanImageUrl.contains("?")) {
                cleanImageUrl = cleanImageUrl.split("\\?")[0];
                log.info("이미지 URL 쿼리 파라미터 제거: {} -> {}", request.getImageUrl(), cleanImageUrl);
            }
            ticket.setImageUrl(cleanImageUrl);
        }
        if (request.getImagePrompt() != null) {
            ticket.setImagePrompt(request.getImagePrompt());
        }
        if (request.getReviewText() != null) {
            ticket.setReviewText(request.getReviewText());
        }
        if (request.getIsPublic() != null) {
            ticket.setIsPublic(request.getIsPublic());
        }
        
        ticketRepository.save(ticket);
        log.info("티켓 수정 완료: ticketId={}, userId={}", ticketId, requesterUserId);
    }

    /**
     * 티켓 삭제
     * @param ticketId 삭제할 티켓 ID
     * @param requesterUserId 요청하는 사용자 ID
     * @throws IllegalArgumentException 티켓이 없거나 권한이 없는 경우
     */
    @Transactional
    public void deleteTicket(Long ticketId, String requesterUserId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓이 없습니다. id=" + ticketId));
        
        // 티켓 삭제 권한 확인
        if (!ticket.getUser().getId().equals(requesterUserId)) {
            throw new SecurityException("본인 티켓만 삭제 가능합니다.");
        }
        
        // 1. 관련된 ticket_likes 먼저 삭제 (외래 키 제약 조건 해결)
        long likeCount = ticketLikeRepository.countByTicket_Id(ticketId);
        if (likeCount > 0) {
            ticketLikeRepository.deleteByTicket_Id(ticketId);
            log.info("티켓 좋아요 {}개 삭제 완료: ticketId={}", likeCount, ticketId);
        }
        
        // 2. 티켓 삭제
        ticketRepository.delete(ticket);
        log.info("티켓 삭제 완료: ticketId={}, userId={}", ticketId, requesterUserId);
    }

    /**
     * 고급 검색: 조건에 맞는 티켓 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> searchTickets(String userId, TicketSearchRequest request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        String sortDirection = request.getSortDirection() != null ? request.getSortDirection() : "DESC";
        
        List<Ticket> tickets = ticketRepository.searchTickets(
                userId,
                request.getStartDate(),
                request.getEndDate(),
                request.getGenre(),
                request.getVenue(),
                request.getArtist(),
                request.getPerformanceTitle(),
                sortBy,
                sortDirection
        );
        
        tickets.forEach(t -> t.getUser().getId()); // LAZY 로딩
        return tickets.stream()
                .map(TicketResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 티켓 통계 분석
     */
    @Transactional(readOnly = true)
    public TicketStatisticsResponse getTicketStatistics(String userId, Integer year) {
        final int targetYear = (year != null) ? year : LocalDate.now().getYear();
        
        // 올해/작년 총 관람 수
        long totalCountThisYear = ticketRepository.countByUserAndYear(userId, targetYear);
        long totalCountLastYear = ticketRepository.countByUserAndYear(userId, targetYear - 1);
        long yearOverYearChange = totalCountThisYear - totalCountLastYear;
        
        // 월별 추이
        Map<String, Long> monthlyTrend = new LinkedHashMap<>();
        List<Object[]> monthlyData = ticketRepository.getMonthlyTrend(userId, targetYear);
        for (Object[] row : monthlyData) {
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            monthlyTrend.put(String.format("%02d", month), count);
        }
        // 빈 월은 0으로 채우기
        IntStream.rangeClosed(1, 12).forEach(month -> {
            String key = String.format("%02d", month);
            monthlyTrend.putIfAbsent(key, 0L);
        });
        
        // 장르별 통계
        List<TicketStatisticsResponse.GenreStatistics> genreStats = new ArrayList<>();
        List<Object[]> genreData = ticketRepository.getGenreStatistics(userId, targetYear);
        long totalForGenre = genreData.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
        for (Object[] row : genreData) {
            String genre = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double percentage = totalForGenre > 0 ? (count * 100.0 / totalForGenre) : 0.0;
            genreStats.add(TicketStatisticsResponse.GenreStatistics.builder()
                    .genre(genre)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }
        
        // 공연장별 통계 (TOP 10)
        List<TicketStatisticsResponse.VenueStatistics> topVenues = ticketRepository.getVenueStatistics(userId, targetYear)
                .stream()
                .limit(10)
                .map(row -> TicketStatisticsResponse.VenueStatistics.builder()
                        .venue((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        // 작품별 통계 (TOP 10)
        List<TicketStatisticsResponse.PerformanceStatistics> topPerformances = ticketRepository.getPerformanceStatistics(userId, targetYear)
                .stream()
                .limit(10)
                .map(row -> TicketStatisticsResponse.PerformanceStatistics.builder()
                        .performanceTitle((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        // 아티스트별 통계 (TOP 10)
        List<TicketStatisticsResponse.ArtistStatistics> topArtists = ticketRepository.getArtistStatistics(userId, targetYear)
                .stream()
                .limit(10)
                .map(row -> TicketStatisticsResponse.ArtistStatistics.builder()
                        .artist((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        // 요일별 통계
        Map<String, Long> dayOfWeekStats = new LinkedHashMap<>();
        List<Object[]> dayData = ticketRepository.getDayOfWeekStatistics(userId, targetYear);
        // PostgreSQL EXTRACT(DOW FROM date): 0=일요일, 1=월요일, ..., 6=토요일
        String[] dayNames = {"일", "월", "화", "수", "목", "금", "토"};
        for (Object[] row : dayData) {
            int dayOfWeek = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            dayOfWeekStats.put(dayNames[dayOfWeek], count);
        }
        
        // 평일/주말 비율
        long weekdayCount = 0;
        long weekendCount = 0;
        List<Ticket> allTickets = ticketRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        for (Ticket ticket : allTickets) {
            if (ticket.getViewDate().getYear() == targetYear) {
                DayOfWeek dayOfWeek = ticket.getViewDate().getDayOfWeek();
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    weekendCount++;
                } else {
                    weekdayCount++;
                }
            }
        }
        long totalDayCount = weekdayCount + weekendCount;
        double weekdayPercentage = totalDayCount > 0 ? (weekdayCount * 100.0 / totalDayCount) : 0.0;
        double weekendPercentage = totalDayCount > 0 ? (weekendCount * 100.0 / totalDayCount) : 0.0;
        
        TicketStatisticsResponse.WeekdayWeekendRatio weekdayWeekendRatio = 
                TicketStatisticsResponse.WeekdayWeekendRatio.builder()
                        .weekdayCount(weekdayCount)
                        .weekendCount(weekendCount)
                        .weekdayPercentage(weekdayPercentage)
                        .weekendPercentage(weekendPercentage)
                        .build();
        
        // 상반기 vs 하반기
        long firstHalfCount = 0;
        long secondHalfCount = 0;
        for (Ticket ticket : allTickets) {
            if (ticket.getViewDate().getYear() == targetYear) {
                int month = ticket.getViewDate().getMonthValue();
                if (month <= 6) {
                    firstHalfCount++;
                } else {
                    secondHalfCount++;
                }
            }
        }
        long totalHalfYearCount = firstHalfCount + secondHalfCount;
        double firstHalfPercentage = totalHalfYearCount > 0 ? (firstHalfCount * 100.0 / totalHalfYearCount) : 0.0;
        double secondHalfPercentage = totalHalfYearCount > 0 ? (secondHalfCount * 100.0 / totalHalfYearCount) : 0.0;
        
        TicketStatisticsResponse.HalfYearComparison halfYearComparison = 
                TicketStatisticsResponse.HalfYearComparison.builder()
                        .firstHalfCount(firstHalfCount)
                        .secondHalfCount(secondHalfCount)
                        .firstHalfPercentage(firstHalfPercentage)
                        .secondHalfPercentage(secondHalfPercentage)
                        .build();
        
        return TicketStatisticsResponse.builder()
                .totalCountThisYear(totalCountThisYear)
                .totalCountLastYear(totalCountLastYear)
                .yearOverYearChange(yearOverYearChange)
                .monthlyTrend(monthlyTrend)
                .genreStatistics(genreStats)
                .topVenues(topVenues)
                .topPerformances(topPerformances)
                .topArtists(topArtists)
                .dayOfWeekStatistics(dayOfWeekStats)
                .weekdayWeekendRatio(weekdayWeekendRatio)
                .halfYearComparison(halfYearComparison)
                .build();
    }

    /**
     * 연말 결산 (Year-in-Review)
     */
    @Transactional(readOnly = true)
    public YearInReviewResponse getYearInReview(String userId, Integer year) {
        final int targetYear = (year != null) ? year : LocalDate.now().getYear();
        final int lastYear = targetYear - 1;
        
        List<Ticket> allTickets = ticketRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        List<Ticket> yearTickets = allTickets.stream()
                .filter(t -> t.getViewDate().getYear() == targetYear)
                .sorted(Comparator.comparing(Ticket::getViewDate))
                .collect(Collectors.toList());
        
        List<Ticket> lastYearTickets = allTickets.stream()
                .filter(t -> t.getViewDate().getYear() == lastYear)
                .collect(Collectors.toList());
        
        long totalCount = yearTickets.size();
        long lastYearCount = lastYearTickets.size();
        long change = totalCount - lastYearCount;
        double changePercentage = lastYearCount > 0 ? (change * 100.0 / lastYearCount) : 0.0;
        String trend = change > 0 ? "증가" : (change < 0 ? "감소" : "동일");
        
        YearInReviewResponse.YearComparison yearComparison = YearInReviewResponse.YearComparison.builder()
                .lastYearCount(lastYearCount)
                .change(change)
                .changePercentage(changePercentage)
                .trend(trend)
                .build();
        
        // 장르 TOP 3
        Map<String, Long> genreCounts = yearTickets.stream()
                .filter(t -> t.getGenre() != null)
                .collect(Collectors.groupingBy(Ticket::getGenre, Collectors.counting()));
        
        List<YearInReviewResponse.GenreRanking> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    double percentage = totalCount > 0 ? (entry.getValue() * 100.0 / totalCount) : 0.0;
                    return YearInReviewResponse.GenreRanking.builder()
                            .genre(entry.getKey())
                            .count(entry.getValue())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 가장 많이 간 공연장
        String mostVisitedVenue = yearTickets.stream()
                .filter(t -> t.getVenue() != null)
                .collect(Collectors.groupingBy(Ticket::getVenue, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // 가장 많이 본 작품
        String mostWatchedPerformance = yearTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getPerformanceTitle, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // 가장 많이 본 아티스트
        String mostWatchedArtist = yearTickets.stream()
                .filter(t -> t.getArtist() != null)
                .collect(Collectors.groupingBy(Ticket::getArtist, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // 요일별 통계
        Map<String, Long> dayCounts = new LinkedHashMap<>();
        long weekdayCount = 0;
        long weekendCount = 0;
        String[] dayNames = {"일", "월", "화", "수", "목", "금", "토"};
        for (Ticket ticket : yearTickets) {
            DayOfWeek dayOfWeek = ticket.getViewDate().getDayOfWeek();
            int dayIndex = dayOfWeek.getValue() % 7; // 0=월, 6=일
            dayCounts.put(dayNames[dayIndex], dayCounts.getOrDefault(dayNames[dayIndex], 0L) + 1);
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                weekendCount++;
            } else {
                weekdayCount++;
            }
        }
        String preferredDayType = weekdayCount > weekendCount ? "평일" : (weekendCount > weekdayCount ? "주말" : "균형");
        
        YearInReviewResponse.DayOfWeekStatistics dayOfWeekStats = YearInReviewResponse.DayOfWeekStatistics.builder()
                .countByDay(dayCounts)
                .weekdayCount(weekdayCount)
                .weekendCount(weekendCount)
                .preferredDayType(preferredDayType)
                .build();
        
        // 상반기 vs 하반기
        long firstHalfCount = yearTickets.stream()
                .filter(t -> t.getViewDate().getMonthValue() <= 6)
                .count();
        long secondHalfCount = yearTickets.size() - firstHalfCount;
        String pattern = firstHalfCount > secondHalfCount ? "상반기 집중" : 
                        (secondHalfCount > firstHalfCount ? "하반기 집중" : "균형");
        
        YearInReviewResponse.HalfYearPattern halfYearPattern = YearInReviewResponse.HalfYearPattern.builder()
                .firstHalfCount(firstHalfCount)
                .secondHalfCount(secondHalfCount)
                .pattern(pattern)
                .build();
        
        // 스페셜 포인트
        YearInReviewResponse.TicketInfo firstTicket = null;
        YearInReviewResponse.TicketInfo lastTicket = null;
        YearInReviewResponse.TicketInfo mostMemorableTicket = null;
        
        if (!yearTickets.isEmpty()) {
            Ticket first = yearTickets.get(0);
            firstTicket = YearInReviewResponse.TicketInfo.builder()
                    .ticketId(first.getId())
                    .performanceTitle(first.getPerformanceTitle())
                    .viewDate(first.getViewDate())
                    .venue(first.getVenue())
                    .reviewPreview(first.getReviewText() != null && first.getReviewText().length() > 50 ? 
                            first.getReviewText().substring(0, 50) + "..." : first.getReviewText())
                    .build();
            
            Ticket last = yearTickets.get(yearTickets.size() - 1);
            lastTicket = YearInReviewResponse.TicketInfo.builder()
                    .ticketId(last.getId())
                    .performanceTitle(last.getPerformanceTitle())
                    .viewDate(last.getViewDate())
                    .venue(last.getVenue())
                    .reviewPreview(last.getReviewText() != null && last.getReviewText().length() > 50 ? 
                            last.getReviewText().substring(0, 50) + "..." : last.getReviewText())
                    .build();
            
            // 가장 긴 후기를 가진 티켓을 가장 기억에 남는 티켓으로 선택
            mostMemorableTicket = yearTickets.stream()
                    .filter(t -> t.getReviewText() != null)
                    .max(Comparator.comparing(t -> t.getReviewText().length()))
                    .map(t -> YearInReviewResponse.TicketInfo.builder()
                            .ticketId(t.getId())
                            .performanceTitle(t.getPerformanceTitle())
                            .viewDate(t.getViewDate())
                            .venue(t.getVenue())
                            .reviewPreview(t.getReviewText().length() > 50 ? 
                                    t.getReviewText().substring(0, 50) + "..." : t.getReviewText())
                            .build())
                    .orElse(null);
        }
        
        YearInReviewResponse.SpecialPoints specialPoints = YearInReviewResponse.SpecialPoints.builder()
                .firstTicket(firstTicket)
                .lastTicket(lastTicket)
                .mostMemorableTicket(mostMemorableTicket)
                .build();
        
        // 공연 소비 성향 유형 분석
        String consumptionType = determineConsumptionType(yearTickets, genreCounts, mostVisitedVenue);
        YearInReviewResponse.ConsumptionType consumptionTypeObj = YearInReviewResponse.ConsumptionType.builder()
                .type(consumptionType)
                .description(getConsumptionTypeDescription(consumptionType))
                .confidence(0.8) // 간단한 분석이므로 고정값
                .build();
        
        // 가장 사랑한 아티스트 TOP 5
        List<YearInReviewResponse.FavoriteArtist> favoriteArtists = yearTickets.stream()
                .filter(t -> t.getArtist() != null)
                .collect(Collectors.groupingBy(Ticket::getArtist, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    double percentage = totalCount > 0 ? (entry.getValue() * 100.0 / totalCount) : 0.0;
                    return YearInReviewResponse.FavoriteArtist.builder()
                            .artist(entry.getKey())
                            .count(entry.getValue())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 공연 수집 성향 지표
        long ticketsWithImages = yearTickets.stream()
                .filter(t -> t.getImageUrl() != null && !t.getImageUrl().isEmpty())
                .count();
        double imageGenerationRate = totalCount > 0 ? (ticketsWithImages * 100.0 / totalCount) : 0.0;
        
        YearInReviewResponse.CollectionMetrics collectionMetrics = YearInReviewResponse.CollectionMetrics.builder()
                .totalImageGenerations(ticketsWithImages)
                .ticketsWithImages(ticketsWithImages)
                .imageGenerationRate(imageGenerationRate)
                .build();
        
        return YearInReviewResponse.builder()
                .year(targetYear)
                .totalCount(totalCount)
                .yearComparison(yearComparison)
                .topGenres(topGenres)
                .mostVisitedVenue(mostVisitedVenue)
                .mostWatchedPerformance(mostWatchedPerformance)
                .mostWatchedArtist(mostWatchedArtist)
                .dayOfWeekStatistics(dayOfWeekStats)
                .halfYearPattern(halfYearPattern)
                .specialPoints(specialPoints)
                .consumptionType(consumptionTypeObj)
                .favoriteArtists(favoriteArtists)
                .collectionMetrics(collectionMetrics)
                .build();
    }
    
    private String determineConsumptionType(List<Ticket> tickets, Map<String, Long> genreCounts, String mostVisitedVenue) {
        // 극장 러버형: 같은 공연장을 많이 방문
        long sameVenueCount = tickets.stream()
                .filter(t -> t.getVenue() != null && t.getVenue().equals(mostVisitedVenue))
                .count();
        double venueRatio = tickets.size() > 0 ? (sameVenueCount * 100.0 / tickets.size()) : 0.0;
        if (venueRatio > 40) {
            return "극장 러버형";
        }
        
        // 배우 추종형: 같은 아티스트를 많이 봄
        Map<String, Long> artistCounts = tickets.stream()
                .filter(t -> t.getArtist() != null)
                .collect(Collectors.groupingBy(Ticket::getArtist, Collectors.counting()));
        if (!artistCounts.isEmpty()) {
            long maxArtistCount = artistCounts.values().stream().max(Long::compare).orElse(0L);
            double artistRatio = tickets.size() > 0 ? (maxArtistCount * 100.0 / tickets.size()) : 0.0;
            if (artistRatio > 30) {
                return "배우 추종형";
            }
        }
        
        // 신작 탐험가형: 같은 작품을 반복해서 보지 않음
        Map<String, Long> performanceCounts = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getPerformanceTitle, Collectors.counting()));
        long uniquePerformances = performanceCounts.size();
        double uniquenessRatio = tickets.size() > 0 ? (uniquePerformances * 100.0 / tickets.size()) : 0.0;
        if (uniquenessRatio > 80) {
            return "신작 탐험가형";
        }
        
        return "균형잡힌 관람자형";
    }
    
    private String getConsumptionTypeDescription(String type) {
        switch (type) {
            case "극장 러버형":
                return "특정 공연장을 자주 방문하는 당신은 그 공연장의 분위기와 시설을 사랑하는 타입입니다.";
            case "배우 추종형":
                return "특정 배우나 아티스트의 작품을 자주 보는 당신은 그들의 연기나 음악을 깊이 즐기는 타입입니다.";
            case "신작 탐험가형":
                return "다양한 작품을 탐험하는 당신은 새로운 경험을 추구하는 모험가 타입입니다.";
            default:
                return "균형잡힌 관람 패턴을 보이는 당신은 다양한 공연을 즐기는 타입입니다.";
        }
    }
}

