package com.example.record.review.service;

import com.example.record.review.dto.request.TicketCreateRequest;
import com.example.record.review.dto.response.TicketCreateResponse;
import com.example.record.review.entity.Ticket;
import com.example.record.review.repository.TicketRepository;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

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

        // 티켓 생성
        Ticket ticket = Ticket.builder()
                .user(user)
                .performanceTitle(request.getPerformanceTitle())
                .theater(request.getTheater())
                .posterUrl(request.getPosterUrl())
                .genre(request.getGenre())
                .viewDate(request.getViewDate())
                .imageUrl(request.getImageUrl())  // 이미지 URL 저장
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
}

