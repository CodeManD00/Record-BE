package com.example.record.STT;

import java.time.LocalDateTime;

// 클라이언트에 전달하는 STT 기록 DTO
public record TranscriptionResponse(
        Long id,
        String fileName,
        String resultText,
        LocalDateTime createdAt,
        String summary

) {}
