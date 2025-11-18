// com/example/record/STT/SummaryResponse.java
package com.example.record.STTorText.dto;

public record SummaryResponse(
        Long transcriptionId,
        String summary
) {}
