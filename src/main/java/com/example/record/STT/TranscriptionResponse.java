package com.example.record.STT;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * STT/요약/질문/최종후기 상태를 한 번에 보여주는 스냅샷 DTO.
 * - 저장 전(임시) 응답: id=null, createdAt=now
 * - 저장 후 응답: id != null
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionResponse {

    /** DB ID (저장 전이면 null) */
    private Long id;

    /** 원본 파일명(또는 가상명) */
    private String fileName;

    /** 생성/저장 시각 (임시 응답일 때는 now) */
    private LocalDateTime createdAt;

    /** Whisper STT 원문 */
    private String transcript;

    /** GPT 요약 결과 (없으면 null) */
    private String summary;

    /** GPT 질문 목록 (없으면 null/빈 배열) */
    private List<String> questions;

    /** 최종 후기 (merge-answers 호출 응답에서만 채움) */
    private String finalReview;
}
