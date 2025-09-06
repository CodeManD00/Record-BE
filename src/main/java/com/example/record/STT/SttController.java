package com.example.record.STT;

import com.example.record.DB.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stt")
public class SttController {

    private final SttService sttService; // 음성 → 텍스트
    private final SttGptService sttGptService; // 요약
    private final TranscriptionRepository transcriptionRepository;

    // [1] 음성 파일 업로드 → 텍스트 변환 → DB 저장
    @PostMapping
    public ResponseEntity<String> transcribe(@RequestParam MultipartFile file,
                                             @AuthenticationPrincipal User user) throws Exception {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        // 원본 확장자 보존(없으면 .tmp)
        String suffix = ".tmp";
        String original = file.getOriginalFilename();
        if (original != null && original.lastIndexOf('.') != -1) {
            suffix = original.substring(original.lastIndexOf('.'));
        }

        Path temp = Files.createTempFile("stt_", suffix);
        try {
            file.transferTo(temp.toFile());

            String result = sttService.transcribeLocalFile(temp.toString());

            Transcription t = Transcription.builder()
                    .fileName(temp.getFileName().toString())
                    .resultText(result)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build();
            transcriptionRepository.save(t);

            return ResponseEntity.ok(result);
        } finally {
            try { Files.deleteIfExists(temp); } catch (Exception ignore) {}
        }
    }

    // 회고 요약 생성 (본인 기록만)
    @PostMapping("/gpt")
    public ResponseEntity<GptResponse> summarize(@RequestParam Long id,
                                                 @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(new GptResponse("Unauthorized"));
        }

        return transcriptionRepository.findById(id)
                .map(t -> {
                    if (!t.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(new GptResponse("Forbidden"));
                    }
                    String summary = sttGptService.summarize(t.getResultText());
                    t.setSummary(summary);
                    transcriptionRepository.save(t);
                    return ResponseEntity.ok(new GptResponse(summary));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(new GptResponse("Not Found")));
    }

    // 내 기록 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<TranscriptionResponse>> list(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();

        List<Transcription> transcriptions = transcriptionRepository.findByUser(user);
        List<TranscriptionResponse> response = transcriptions.stream()
                .map(t -> new TranscriptionResponse(
                        t.getId(),
                        t.getFileName(),
                        t.getResultText(),
                        t.getCreatedAt(),
                        t.getSummary()
                )).toList();
        return ResponseEntity.ok(response);
    }
}
