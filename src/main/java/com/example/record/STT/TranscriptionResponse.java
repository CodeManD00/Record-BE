// com/example/record/STT/TranscriptionResponse.java
package com.example.record.STT;

import java.time.LocalDateTime;
import java.util.List;

public record TranscriptionResponse(
        Long id,
        String fileName,
        String resultText,
        LocalDateTime createdAt,
        String summary,
        List<String> questions
) {}
