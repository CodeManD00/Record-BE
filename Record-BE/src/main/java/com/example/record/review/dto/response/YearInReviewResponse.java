package com.example.record.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 연말 결산(Year-in-Review) 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearInReviewResponse {
    /**
     * 분석 대상 연도
     */
    private Integer year;
    
    /**
     * 올해 총 관람 수
     */
    private Long totalCount;
    
    /**
     * 작년 대비 증가/감소 수 및 비율
     */
    private YearComparison yearComparison;
    
    /**
     * 장르 TOP 3
     */
    private List<GenreRanking> topGenres;
    
    /**
     * 가장 많이 간 공연장
     */
    private String mostVisitedVenue;
    
    /**
     * 가장 많이 본 작품
     */
    private String mostWatchedPerformance;
    
    /**
     * 가장 많이 본 아티스트
     */
    private String mostWatchedArtist;
    
    /**
     * 관람 요일 통계
     */
    private DayOfWeekStatistics dayOfWeekStatistics;
    
    /**
     * 관람 패턴 변화 (상반기 vs 하반기)
     */
    private HalfYearPattern halfYearPattern;
    
    /**
     * 스페셜 포인트
     */
    private SpecialPoints specialPoints;
    
    /**
     * 공연 소비 성향 유형
     */
    private ConsumptionType consumptionType;
    
    /**
     * 가장 사랑한 배우/밴드/캐스트 TOP 5
     */
    private List<FavoriteArtist> favoriteArtists;
    
    /**
     * 공연 수집 성향 지표
     */
    private CollectionMetrics collectionMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearComparison {
        private Long lastYearCount;
        private Long change;
        private Double changePercentage;
        private String trend; // "증가", "감소", "동일"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreRanking {
        private Integer rank;
        private String genre;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayOfWeekStatistics {
        private Map<String, Long> countByDay;
        private Long weekdayCount;
        private Long weekendCount;
        private String preferredDayType; // "평일", "주말"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HalfYearPattern {
        private Long firstHalfCount;
        private Long secondHalfCount;
        private String pattern; // "상반기 집중", "하반기 집중", "균형"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialPoints {
        private TicketInfo firstTicket;
        private TicketInfo lastTicket;
        private TicketInfo mostMemorableTicket;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private Long ticketId;
        private String performanceTitle;
        private LocalDate viewDate;
        private String venue;
        private String reviewPreview;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumptionType {
        private String type; // "극장 러버형", "배우 추종형", "신작 탐험가형" 등
        private String description;
        private Double confidence; // 유형 일치도 (0.0 ~ 1.0)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteArtist {
        private String artist;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionMetrics {
        private Long totalImageGenerations;
        private Long ticketsWithImages;
        private Double imageGenerationRate; // 이미지 생성 비율
    }
}

