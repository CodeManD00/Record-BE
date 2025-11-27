package com.example.record.review.controller;

import com.example.record.review.dto.request.TicketCreateRequest;
import com.example.record.review.dto.request.TicketSearchRequest;
import com.example.record.review.dto.request.TicketUpdateRequest;
import com.example.record.review.dto.response.TicketCreateResponse;
import com.example.record.review.dto.response.TicketResponse;
import com.example.record.review.dto.response.TicketStatisticsResponse;
import com.example.record.review.dto.response.YearInReviewResponse;
import com.example.record.review.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 티켓 생성
     * 
     * 티켓 정보와 함께 image_url을 받아서 저장합니다.
     * 
     * @param request 티켓 생성 요청 (imageUrl 포함)
     * @return 생성된 티켓 정보
     */
    @PostMapping
    public ResponseEntity<TicketCreateResponse> createTicket(@RequestBody TicketCreateRequest request) {
        TicketCreateResponse response = ticketService.createTicket(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 티켓 목록 조회 (공개 티켓만 반환)
     * 
     * 친구 프로필에서 조회할 때 사용됩니다.
     * 공개 설정된 티켓만 반환하여 비공개 티켓을 보호합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 공개 티켓 목록 (생성 시간 내림차순)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByUserId(
            @PathVariable("userId") String userId,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        System.out.println("🔍 GET /api/tickets/user/" + userId + " 요청 받음, currentUserId=" + currentUserId);
        List<TicketResponse> tickets = ticketService.getPublicTicketsByUserId(userId, currentUserId);
        System.out.println("✅ 공개 티켓 반환: " + tickets.size() + "개");
        return ResponseEntity.ok(tickets);
    }

    /**
     * 티켓 수정
     * 
     * 보안: X-User-Id 헤더에서 사용자 ID를 추출하여 권한 확인
     * 
     * @param ticketId 수정할 티켓 ID
     * @param requesterUserId 요청하는 사용자 ID (X-User-Id 헤더)
     * @param request 티켓 수정 요청 정보
     * @return 수정 완료 응답 (204 No Content)
     */
    @PatchMapping("/{ticketId}")
    public ResponseEntity<Void> updateTicket(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String requesterUserId,
            @RequestBody TicketUpdateRequest request) {
        ticketService.updateTicket(ticketId, requesterUserId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 티켓 삭제
     * 
     * 보안: X-User-Id 헤더에서 사용자 ID를 추출하여 권한 확인
     * 
     * @param ticketId 삭제할 티켓 ID
     * @param requesterUserId 요청하는 사용자 ID (X-User-Id 헤더)
     * @return 삭제 완료 응답 (204 No Content)
     */
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String requesterUserId) {
        ticketService.deleteTicket(ticketId, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 고급 검색: 조건에 맞는 티켓 목록 조회
     * 
     * @param userId 사용자 ID
     * @param request 검색 조건
     * @return 검색된 티켓 목록
     */
    @PostMapping("/user/{userId}/search")
    public ResponseEntity<List<TicketResponse>> searchTickets(
            @PathVariable("userId") String userId,
            @RequestBody TicketSearchRequest request) {
        List<TicketResponse> tickets = ticketService.searchTickets(userId, request);
        return ResponseEntity.ok(tickets);
    }

    /**
     * 티켓 통계 분석
     * 
     * @param userId 사용자 ID
     * @param year 분석할 연도 (기본값: 현재 연도)
     * @return 통계 분석 결과
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<TicketStatisticsResponse> getTicketStatistics(
            @PathVariable("userId") String userId,
            @RequestParam(value = "year", required = false) Integer year) {
        TicketStatisticsResponse statistics = ticketService.getTicketStatistics(userId, year);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 연말 결산 (Year-in-Review)
     * 
     * @param userId 사용자 ID
     * @param year 분석할 연도 (기본값: 현재 연도)
     * @return 연말 결산 리포트
     */
    @GetMapping("/user/{userId}/year-in-review")
    public ResponseEntity<YearInReviewResponse> getYearInReview(
            @PathVariable("userId") String userId,
            @RequestParam(value = "year", required = false) Integer year) {
        YearInReviewResponse review = ticketService.getYearInReview(userId, year);
        return ResponseEntity.ok(review);
    }

}

