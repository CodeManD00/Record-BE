package com.example.record.review.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreateRequest {
    private String userId;
    private String performanceTitle;
    private String venue;  // 공연장 (theater에서 변경)
    private String seat;  // 좌석
    private String artist;  // 아티스트
    private String posterUrl;
    private String genre;
    private LocalDate viewDate;
    private String imageUrl;  // 티켓 이미지 URL
    private String imagePrompt;  // 이미지 생성 프롬프트
    private String reviewText;
    private Boolean isPublic;
}

