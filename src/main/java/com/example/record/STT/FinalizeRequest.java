// com/example/record/STT/FinalizeRequest.java
package com.example.record.STT;

public record FinalizeRequest(
        Long transcriptionId,  // 필수
        String extraNotes      // 선택
) {}
