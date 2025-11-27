package com.example.record.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 티켓 통계 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatisticsResponse {
    /**
     * 올해 총 관람 수
     */
    private Long totalCountThisYear;
    
    /**
     * 작년 총 관람 수
     */
    private Long totalCountLastYear;
    
    /**
     * 작년 대비 증가/감소 수
     */
    private Long yearOverYearChange;
    
    /**
     * 월별 관람 추이 (월 -> 관람 수)
     */
    private Map<String, Long> monthlyTrend;
    
    /**
     * 장르별 관람 수 및 비율
     */
    private List<GenreStatistics> genreStatistics;
    
    /**
     * 공연장별 관람 수 (TOP 10)
     */
    private List<VenueStatistics> topVenues;
    
    /**
     * 작품별 관람 수 (TOP 10)
     */
    private List<PerformanceStatistics> topPerformances;
    
    /**
     * 아티스트별 관람 수 (TOP 10)
     */
    private List<ArtistStatistics> topArtists;
    
    /**
     * 요일별 관람 통계
     */
    private Map<String, Long> dayOfWeekStatistics;
    
    /**
     * 평일/주말 관람 비율
     */
    private WeekdayWeekendRatio weekdayWeekendRatio;
    
    /**
     * 상반기 vs 하반기 관람 수
     */
    private HalfYearComparison halfYearComparison;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreStatistics {
        private String genre;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueStatistics {
        private String venue;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceStatistics {
        private String performanceTitle;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtistStatistics {
        private String artist;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekdayWeekendRatio {
        private Long weekdayCount;
        private Long weekendCount;
        private Double weekdayPercentage;
        private Double weekendPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HalfYearComparison {
        private Long firstHalfCount;
        private Long secondHalfCount;
        private Double firstHalfPercentage;
        private Double secondHalfPercentage;
    }
}

