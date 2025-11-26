package com.example.record.review.controller;

import com.example.record.review.dto.request.TicketCreateRequest;
import com.example.record.review.dto.request.TicketUpdateRequest;
import com.example.record.review.dto.response.TicketCreateResponse;
import com.example.record.review.dto.response.TicketResponse;
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
     * 사용자의 티켓 목록 조회
     * 
     * 시뮬레이터를 껐다가 켰을 때 DB에서 사용자의 티켓을 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 티켓 목록 (생성 시간 내림차순)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByUserId(
            @PathVariable("userId") String userId) {
        List<TicketResponse> tickets = ticketService.getTicketsByUserId(userId);
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
}

