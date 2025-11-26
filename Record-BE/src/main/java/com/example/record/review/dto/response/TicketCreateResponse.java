package com.example.record.review.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreateResponse {
    private Long ticketId;
    private LocalDateTime createdAt;
}

