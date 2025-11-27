package com.example.record.review.service;

import com.example.record.review.entity.Ticket;
import com.example.record.review.entity.TicketLike;
import com.example.record.review.repository.TicketLikeRepository;
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
public class TicketLikeService {

    private final TicketLikeRepository ticketLikeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요 추가 또는 취소 (토글)
     * @param ticketId 티켓 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 (true: 좋아요 추가됨, false: 좋아요 취소됨)
     */
    @Transactional
    public boolean toggleLike(Long ticketId, String userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다: id=" + ticketId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: id=" + userId));

        return ticketLikeRepository.findByTicket_IdAndUser_Id(ticketId, userId)
                .map(like -> {
                    // 이미 좋아요가 있으면 취소
                    ticketLikeRepository.delete(like);
                    log.info("좋아요 취소: ticketId={}, userId={}", ticketId, userId);
                    return false;
                })
                .orElseGet(() -> {
                    // 좋아요가 없으면 추가
                    TicketLike like = TicketLike.builder()
                            .ticket(ticket)
                            .user(user)
                            .build();
                    ticketLikeRepository.save(like);
                    log.info("좋아요 추가: ticketId={}, userId={}", ticketId, userId);
                    return true;
                });
    }

    /**
     * 특정 티켓의 좋아요 개수 조회
     */
    @Transactional(readOnly = true)
    public long getLikeCount(Long ticketId) {
        return ticketLikeRepository.countByTicket_Id(ticketId);
    }

    /**
     * 특정 사용자가 특정 티켓에 좋아요를 눌렀는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(Long ticketId, String userId) {
        return ticketLikeRepository.findByTicket_IdAndUser_Id(ticketId, userId).isPresent();
    }

    /**
     * 특정 티켓의 좋아요한 사용자 목록 조회 (티켓 소유자만 조회 가능)
     */
    @Transactional(readOnly = true)
    public List<String> getLikedUserIds(Long ticketId, String ticketOwnerId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다: id=" + ticketId));

        // 티켓 소유자만 좋아요 리스트 조회 가능
        if (!ticket.getUser().getId().equals(ticketOwnerId)) {
            throw new IllegalArgumentException("좋아요 리스트는 티켓 소유자만 조회할 수 있습니다.");
        }

        return ticketLikeRepository.findByTicket_IdWithUser(ticketId).stream()
                .map(like -> like.getUser().getId())
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 좋아요를 누른 티켓 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getLikedTicketIds(String userId) {
        return ticketLikeRepository.findTicketIdsByUser_Id(userId);
    }
}

