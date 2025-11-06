// com/example/record/STT/SummaryResponse.java
package com.example.record.STT.dto;

public record SummaryResponse(
        Long transcriptionId,
        String summary
) {}
