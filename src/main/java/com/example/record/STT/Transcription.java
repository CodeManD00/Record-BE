// Transcription: 음성 → 텍스트 변환(STT) 결과와 GPT 요약/질문을 저장하는 엔티티 클래스입니다.

package com.example.record.STT;

import com.example.record.DB.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 ID
    private Long id;

    private String fileName; // 업로드된 음성 파일 이름

    @Lob
    private String resultText; // 변환된 텍스트(STT 결과)

    private LocalDateTime createdAt; // 생성 시각

    @ManyToOne(fetch = FetchType.LAZY) // STT 기록과 사용자 간 N:1 관계
    private User user;

    @Column(columnDefinition = "TEXT")
    private String summary; // GPT로 생성된 요약

    @Column(columnDefinition = "TEXT")
    private String question; // GPT로 생성된 질문
}
