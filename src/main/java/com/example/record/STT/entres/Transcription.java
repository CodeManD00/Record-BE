
package com.example.record.STT.entres;

import com.example.record.user.User;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;         // 업로드된 음성 파일 이름

    @Lob
    private String resultText;       // 변환된 텍스트(STT 결과)

    private LocalDateTime createdAt; // 생성 시각

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String summary;          // GPT 요약/최종본
}
