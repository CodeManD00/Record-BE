package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /**
     * 1) STT만 수행 (저장 없음) → TranscriptionResponse로 임시 스냅샷 반환
     */
    @PostMapping("/transcribe-only")
    public ResponseEntity<?> transcribeOnly(@RequestParam("file") MultipartFile file) {
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

            TranscriptionResponse resp = TranscriptionResponse.builder()
                    .id(null)
                    .fileName(original != null ? original : "uploaded_audio")
                    .createdAt(LocalDateTime.now())
                    .transcript(transcript)
                    .summary(null)
                    .questions(null)
                    .finalReview(null)
                    .build();

            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException tooBig) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(tooBig.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(422).body("Failed to process audio: " + e.getMessage());
        }
    }

    /**
     * 2) STT 수행 후 DB 저장 → TranscriptionResponse 반환
     */
    @PostMapping("/transcribe-and-save")
    public ResponseEntity<?> transcribeAndSave(@RequestParam("file") MultipartFile file,
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

    /**
     * 3) 질문-답변들을 합쳐 최종 후기 생성 → 새 Transcription 저장 후 반환
     */
    @PostMapping("/merge-answers")
    public ResponseEntity<?> mergeAnswers(@RequestBody Map<String, Object> request,
                                          @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> qaList = (List<Map<String, String>>) request.get("qaList");
        String baseReview = (String) request.get("baseReview");

        if (qaList == null || qaList.isEmpty()) {
            return ResponseEntity.badRequest().body("qaList is required");
        }

        try {
            // QA 텍스트 병합
            StringBuilder fullText = new StringBuilder();
            if (StringUtils.hasText(baseReview)) {
                fullText.append("기존 후기:\n").append(baseReview).append("\n\n");
            }
            fullText.append("추가 질문과 답변:\n");
            for (Map<String, String> qa : qaList) {
                String q = qa.get("question");
                String a = qa.get("answer");
                if (StringUtils.hasText(q) && StringUtils.hasText(a)) {
                    fullText.append("Q: ").append(q).append("\n");
                    fullText.append("A: ").append(a).append("\n\n");
                }
            }

            // GPT로 최종 후기 생성
            String finalReview = sttGptService.mergeIntoFinalReview(fullText.toString());

            // 새 Transcription으로 저장
            Transcription finalT = Transcription.builder()
                    .fileName("merged_review_" + System.currentTimeMillis())
                    .resultText(fullText.toString())
                    .summary(finalReview)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build();
            repo.save(finalT);

            TranscriptionResponse resp = TranscriptionResponse.builder()
                    .id(finalT.getId())
                    .fileName(finalT.getFileName())
                    .createdAt(finalT.getCreatedAt())
                    .transcript(finalT.getResultText())
                    .summary(finalT.getSummary())
                    .questions(null)
                    .finalReview(finalReview)
                    .build();

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to generate final review: " + e.getMessage());
        }
    }

    /**
     * 4) 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTranscription(@PathVariable Long id,
                                              @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        return repo.findById(id)
                .filter(t -> t.getUser().equals(user))
                .map(t -> ResponseEntity.ok(toResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 5) 사용자 소유 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<?> listTranscriptions(@AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Transcription> list = repo.findByUser(user);
        return ResponseEntity.ok(list.stream()
                .map(t -> Map.of(
                        "id", t.getId(),
                        "fileName", t.getFileName(),
                        "createdAt", t.getCreatedAt(),
                        "hasSummary", StringUtils.hasText(t.getSummary()),
                        "hasQuestions", StringUtils.hasText(t.getQuestion())
                ))
                .toList());
    }

    /* ========================= 헬퍼 ========================= */

    private static String resolveSuffix(String original) {
        if (original != null && original.lastIndexOf('.') != -1) {
            return original.substring(original.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private TranscriptionResponse toResponse(Transcription t) {
        List<String> qs = parseQuestions(t.getQuestion());
        return TranscriptionResponse.builder()
                .id(t.getId())
                .fileName(t.getFileName())
                .createdAt(t.getCreatedAt())
                .transcript(t.getResultText())
                .summary(t.getSummary())
                .questions(qs)
                .finalReview(null)
                .build();
    }

    private static List<String> parseQuestions(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String[] parts = raw.split("\\|\\|\\|");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (StringUtils.hasText(p)) out.add(p.trim());
        }
        return out.isEmpty() ? null : out;
    }
}