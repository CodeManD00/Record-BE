package com.example.record.STTorText.entity;

import com.example.record.STTorText.review.ReviewType;
import com.example.record.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcription")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Transcription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "result_text", columnDefinition = "TEXT", length = 65535)
    private String resultText;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_type")
    private ReviewType summaryType;
}
