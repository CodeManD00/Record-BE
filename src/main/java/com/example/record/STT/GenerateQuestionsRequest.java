// com/example/record/STT/GenerateQuestionsRequest.java
package com.example.record.STT;

public record GenerateQuestionsRequest(
        Long transcriptionId, // 선택
        String rawText,       // 선택
        Integer count         // 선택(기본 4)
) {}
