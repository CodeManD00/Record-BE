
package com.example.record.STT.dto;

public record FinalizeRequest(
        Long transcriptionId,  // 필수
        String extraNotes      // 선택
) {}
