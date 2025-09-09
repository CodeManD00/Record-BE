package com.example.record.STT;

import java.time.LocalDateTime;

public record SttCreateResponse(
        Long id,
        String fileName,
        String resultText,
        LocalDateTime createdAt
) {}
