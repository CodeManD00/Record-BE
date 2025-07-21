//사용자별 결과 조회 API (/stt/list)

package com.example.record.STT;

import java.time.LocalDateTime;

public record TranscriptionResponse(
        Long id,
        String fileName,
        String resultText,
        LocalDateTime createdAt,
        String summary,
        String question
) {}