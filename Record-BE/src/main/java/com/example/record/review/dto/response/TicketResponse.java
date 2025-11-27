package com.example.record.review.dto.response;

import com.example.record.review.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String userId;
    private String performanceTitle;
    private String venue;  // 공연장 (theater에서 변경)
    private String seat;  // 좌석
    private String artist;  // 아티스트
    private String posterUrl;
    private String genre;
    private LocalDate viewDate;
    private String imageUrl;
    private String imagePrompt;
    private String reviewText;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isLiked;  // 현재 사용자가 좋아요를 눌렀는지 여부
    private Long likeCount;   // 좋아요 개수

    public static TicketResponse from(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .userId(ticket.getUser().getId())
                .performanceTitle(ticket.getPerformanceTitle())
                .venue(ticket.getVenue())
                .seat(ticket.getSeat())
                .artist(ticket.getArtist())
                .posterUrl(ticket.getPosterUrl())
                .genre(ticket.getGenre())
                .viewDate(ticket.getViewDate())
                .imageUrl(ticket.getImageUrl())
                .imagePrompt(ticket.getImagePrompt())
                .reviewText(ticket.getReviewText())
                .isPublic(ticket.getIsPublic())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .isLiked(false)  // 기본값, 서비스에서 설정
                .likeCount(0L)    // 기본값, 서비스에서 설정
                .build();
    }

    public static TicketResponse from(Ticket ticket, Boolean isLiked, Long likeCount) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .userId(ticket.getUser().getId())
                .performanceTitle(ticket.getPerformanceTitle())
                .venue(ticket.getVenue())
                .seat(ticket.getSeat())
                .artist(ticket.getArtist())
                .posterUrl(ticket.getPosterUrl())
                .genre(ticket.getGenre())
                .viewDate(ticket.getViewDate())
                .imageUrl(ticket.getImageUrl())
                .imagePrompt(ticket.getImagePrompt())
                .reviewText(ticket.getReviewText())
                .isPublic(ticket.getIsPublic())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }
}

