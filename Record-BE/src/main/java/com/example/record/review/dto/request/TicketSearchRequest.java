package com.example.record.review.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 티켓 고급 검색 요청 DTO
 * 날짜, 장르, 공연장, 출연진 기준으로 검색
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSearchRequest {
    /**
     * 검색 시작 날짜 (viewDate 기준)
     */
    private LocalDate startDate;
    
    /**
     * 검색 종료 날짜 (viewDate 기준)
     */
    private LocalDate endDate;
    
    /**
     * 장르 필터 (BAND, MUSICAL, PLAY)
     */
    private String genre;
    
    /**
     * 공연장 필터 (부분 일치 검색)
     */
    private String venue;
    
    /**
     * 출연진/아티스트 필터 (부분 일치 검색)
     */
    private String artist;
    
    /**
     * 공연 제목 필터 (부분 일치 검색)
     */
    private String performanceTitle;
    
    /**
     * 정렬 기준 (viewDate, createdAt)
     * 기본값: createdAt DESC
     */
    private String sortBy;
    
    /**
     * 정렬 방향 (ASC, DESC)
     */
    private String sortDirection;
}

