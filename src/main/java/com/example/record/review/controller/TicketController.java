package com.example.record.review.controller;

import com.example.record.review.dto.request.TicketCreateRequest;
import com.example.record.review.dto.response.TicketCreateResponse;
import com.example.record.review.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

