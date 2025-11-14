package com.example.record.STT.controller;

import com.example.record.STT.dto.TranscriptionResponse;
import com.example.record.STT.entres.Transcription;
import com.example.record.STT.entres.TranscriptionRepository;
import com.example.record.STT.service.SttGptService;
import com.example.record.STT.service.SttService;
import com.example.record.STT.service.WhisperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stt")
public class SttController {

    private final WhisperService whisperService;
    private final SttService sttService;
    private final SttGptService sttGptService;
    private final TranscriptionRepository repo;

    /** 🔥 1) STT 수행 후 DB 저장 (이거 하나만 사용) */
    @PostMapping("/transcribe-and-save")
    public ResponseEntity<?> transcribeAndSave(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal com.example.record.user.User user) {

        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        final String original = file.getOriginalFilename();
        final String suffix = resolveSuffix(original);

        try {
            byte[] bytes = file.getBytes();
            bytes = sttService.maybeReencodeToM4a(bytes, suffix);

            String transcript = whisperService.transcribe(bytes, original, "ko");
            if (!StringUtils.hasText(transcript)) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("Failed to extract text from audio");
            }

            Transcription t = Transcription.builder()
                    .fileName(original != null ? original : "uploaded_audio")
                    .resultText(transcript)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build();
            repo.save(t);

            return ResponseEntity.ok(toResponse(t));

        } catch (Exception e) {
            return ResponseEntity.status(422).body("Processing failed: " + e.getMessage());
        }
    }

    /** 🔍 2) 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTranscription(
            @PathVariable Long id,
            @AuthenticationPrincipal com.example.record.user.User user) {

        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        return repo.findById(id)
                .filter(t -> t.getUser().equals(user))
                .map(t -> ResponseEntity.ok(toResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 📄 3) 사용자 소유 목록 조회 */
    @GetMapping("/list")
    public ResponseEntity<?> listTranscriptions(
            @AuthenticationPrincipal com.example.record.user.User user) {

        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Transcription> list = repo.findByUser(user);

        return ResponseEntity.ok(
                list.stream().map(t -> Map.of(
                        "id", t.getId(),
                        "fileName", t.getFileName(),
                        "createdAt", t.getCreatedAt(),
                        "hasSummary", StringUtils.hasText(t.getSummary())
                )).toList()
        );
    }

    /* ========================= 헬퍼 ========================= */

    private static String resolveSuffix(String original) {
        if (original != null && original.lastIndexOf('.') != -1) {
            return original.substring(original.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private TranscriptionResponse toResponse(Transcription t) {
        return TranscriptionResponse.builder()
                .id(t.getId())
                .fileName(t.getFileName())
                .createdAt(t.getCreatedAt())
                .transcript(t.getResultText())
                .summary(t.getSummary())
                .finalReview(null)
                .build();
    }
}
