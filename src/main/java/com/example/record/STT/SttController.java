package com.example.record.STT;

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
    private final QuestionService questionService; // 새로 추가

    /**
     * 1. 순수 STT만 수행 (저장 없이)
     * 프론트에서 임시로 텍스트만 필요할 때 사용
     */
    @PostMapping("/transcribe-only")
    public ResponseEntity<?> transcribeOnly(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        String original = file.getOriginalFilename();
        String suffix = resolveSuffix(original);

        try {
            byte[] bytes = file.getBytes();
            bytes = sttService.maybeReencodeToM4a(bytes, suffix);

            String result = whisperService.transcribe(bytes, original, "ko");
            if (!StringUtils.hasText(result)) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("음성에서 텍스트를 추출하지 못했습니다.");
            }

            return ResponseEntity.ok(Map.of(
                    "text", result,
                    "fileName", original != null ? original : "uploaded_audio"
            ));
        } catch (IllegalArgumentException tooBig) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(tooBig.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(422).body("음성에서 텍스트를 추출하지 못했습니다. " + e.getMessage());
        }
    }

    /**
     * 2. STT 수행 후 DB에 저장
     * 나중에 요약/질문 생성을 위해 저장만 함
     */
    @PostMapping("/transcribe-and-save")
    public ResponseEntity<?> transcribeAndSave(@RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal com.example.record.user.User user) throws Exception {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        String original = file.getOriginalFilename();
        String suffix = resolveSuffix(original);

        try {
            byte[] bytes = file.getBytes();
            bytes = sttService.maybeReencodeToM4a(bytes, suffix);

            String result = whisperService.transcribe(bytes, original, "ko");
            if (!StringUtils.hasText(result)) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("음성에서 텍스트를 추출하지 못했습니다.");
            }

            Transcription t = Transcription.builder()
                    .fileName(original != null ? original : "uploaded_audio")
                    .resultText(result)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build();
            repo.save(t);

            return ResponseEntity.ok(new SttCreateResponse(
                    t.getId(), t.getFileName(), t.getResultText(), t.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(422).body("처리 실패: " + e.getMessage());
        }
    }

    /**
     * 3. 텍스트를 받아서 GPT 요약만 수행
     * STT 없이 텍스트를 직접 받아서 요약
     */
    @PostMapping("/summarize")
    public ResponseEntity<?> summarizeText(@RequestBody Map<String, String> request,
                                           @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        String text = request.get("text");
        Long transcriptionId = request.get("transcriptionId") != null ?
                Long.parseLong(request.get("transcriptionId")) : null;

        if (!StringUtils.hasText(text)) {
            return ResponseEntity.badRequest().body("텍스트가 필요합니다.");
        }

        try {
            String summary = sttGptService.summarize(text);

            // transcriptionId가 있으면 해당 레코드 업데이트
            if (transcriptionId != null) {
                repo.findById(transcriptionId).ifPresent(t -> {
                    if (t.getUser().equals(user)) {
                        t.setSummary(summary);
                        repo.save(t);
                    }
                });
            }

            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("요약 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 4. 텍스트를 받아서 GPT 질문 생성
     * 후기 바탕으로 질문 생성
     */
    @PostMapping("/generate-questions")
    public ResponseEntity<?> generateQuestions(@RequestBody Map<String, String> request,
                                               @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        String text = request.get("text");
        Long transcriptionId = request.get("transcriptionId") != null ?
                Long.parseLong(request.get("transcriptionId")) : null;

        if (!StringUtils.hasText(text)) {
            return ResponseEntity.badRequest().body("텍스트가 필요합니다.");
        }

        try {
            List<String> questions = sttGptService.generateQuestions(text);

            // transcriptionId가 있으면 해당 레코드에 질문 저장
            if (transcriptionId != null) {
                repo.findById(transcriptionId).ifPresent(t -> {
                    if (t.getUser().equals(user)) {
                        t.setQuestion(String.join("|||", questions)); // 구분자로 저장
                        repo.save(t);
                    }
                });
            }

            return ResponseEntity.ok(Map.of("questions", questions));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("질문 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 5. DB에서 미리 준비된 질문 가져오기
     * 카테고리별 질문 템플릿 제공
     */
    @GetMapping("/preset-questions")
    public ResponseEntity<?> getPresetQuestions(@RequestParam(required = false) String category) {
        try {
            List<String> questions = questionService.getPresetQuestions(category);
            return ResponseEntity.ok(Map.of("questions", questions, "category", category));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("질문 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 6. 질문에 대한 답변들을 병합하여 최종 후기 생성
     * 여러 답변을 하나의 후기로 통합
     */
    @PostMapping("/merge-answers")
    public ResponseEntity<?> mergeAnswers(@RequestBody Map<String, Object> request,
                                          @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> qaList = (List<Map<String, String>>) request.get("qaList");
        String baseReview = (String) request.get("baseReview"); // 기존 후기 (있으면)

        if (qaList == null || qaList.isEmpty()) {
            return ResponseEntity.badRequest().body("질문-답변 목록이 필요합니다.");
        }

        try {
            // QA 리스트를 텍스트로 변환
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

            // GPT로 통합 후기 생성
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

            return ResponseEntity.ok(Map.of(
                    "finalReview", finalReview,
                    "transcriptionId", finalT.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("최종 후기 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 7. 저장된 Transcription 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTranscription(@PathVariable Long id,
                                              @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        return repo.findById(id)
                .filter(t -> t.getUser().equals(user))
                .map(t -> {
                    List<String> questions = null;
                    if (StringUtils.hasText(t.getQuestion())) {
                        questions = List.of(t.getQuestion().split("\\|\\|\\|"));
                    }
                    return ResponseEntity.ok(new TranscriptionResponse(
                            t.getId(), t.getFileName(), t.getResultText(),
                            t.getCreatedAt(), t.getSummary(), questions
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 8. 사용자의 모든 Transcription 목록 조회
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

    private static String resolveSuffix(String original) {
        if (original != null && original.lastIndexOf('.') != -1) {
            return original.substring(original.lastIndexOf('.'));
        }
        return ".tmp";
    }
}