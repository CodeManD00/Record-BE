package com.example.record.review.controller;

import com.example.record.review.service.TicketLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketLikeController {

    private final TicketLikeService ticketLikeService;

    /**
     * 좋아요 추가 또는 취소 (토글)
     * 
     * @param ticketId 티켓 ID
     * @param userId 사용자 ID (X-User-Id 헤더)
     * @return 좋아요 상태
     */
    @PostMapping("/{ticketId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId) {
        boolean isLiked = ticketLikeService.toggleLike(ticketId, userId);
        long likeCount = ticketLikeService.getLikeCount(ticketId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", likeCount);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 티켓의 좋아요 개수 조회
     */
    @GetMapping("/{ticketId}/like/count")
    public ResponseEntity<Map<String, Object>> getLikeCount(
            @PathVariable("ticketId") Long ticketId) {
        long likeCount = ticketLikeService.getLikeCount(ticketId);
        Map<String, Object> response = new HashMap<>();
        response.put("likeCount", likeCount);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자가 특정 티켓에 좋아요를 눌렀는지 확인
     */
    @GetMapping("/{ticketId}/like/status")
    public ResponseEntity<Map<String, Object>> getLikeStatus(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId) {
        boolean isLiked = ticketLikeService.isLiked(ticketId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 티켓의 좋아요한 사용자 목록 조회 (티켓 소유자만 조회 가능)
     */
    @GetMapping("/{ticketId}/likes")
    public ResponseEntity<Map<String, Object>> getLikedUsers(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId) {
        List<String> likedUserIds = ticketLikeService.getLikedUserIds(ticketId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("likedUserIds", likedUserIds);
        return ResponseEntity.ok(response);
    }
}

