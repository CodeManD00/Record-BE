// com/example/record/STT/FinalizeResponse.java
package com.example.record.STT;

public record FinalizeResponse(
        Long transcriptionId,
        String finalReview
) {}
