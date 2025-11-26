package com.example.record.review.service;

import com.example.record.review.dto.request.TicketCreateRequest;
import com.example.record.review.dto.request.TicketUpdateRequest;
import com.example.record.review.dto.response.TicketCreateResponse;
import com.example.record.review.dto.response.TicketResponse;
import com.example.record.review.entity.Ticket;
import com.example.record.review.repository.TicketRepository;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
                .venue(request.getVenue())
                .seat(request.getSeat())
                .artist(request.getArtist())
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

    /**
     * 사용자의 티켓 목록 조회
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
            ticket.setImageUrl(request.getImageUrl());
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
        
        ticketRepository.delete(ticket);
        log.info("티켓 삭제 완료: ticketId={}, userId={}", ticketId, requesterUserId);
    }
}

