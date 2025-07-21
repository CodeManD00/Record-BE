// TranscriptionResponse: /stt/list API에서 사용자에게 반환되는 STT 기록 요약 응답 DTO입니다.

package com.example.record.STT;

import java.time.LocalDateTime;

public record TranscriptionResponse(
        Long id,                 // STT 기록 ID
        String fileName,        // 업로드된 파일 이름
        String resultText,      // 변환된 텍스트(STT 결과)
        LocalDateTime createdAt,// 생성 시각
        String summary,         // GPT로 생성된 요약
        String question         // GPT로 생성된 질문
) {}
