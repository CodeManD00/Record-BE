
package com.example.record.STTorText.dto;

public record FinalizeRequest(
        Long transcriptionId,  // 필수
        String extraNotes      // 선택
) {}
