package com.example.record.STT;

import com.example.record.user.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stt")
public class SttController {

    private final SttService sttService;              // 음성 → 텍스트
    private final SttGptService sttGptService;        // 요약/질문 생성
    private final TranscriptionRepository repo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 업로드 → STT 실행 */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> transcribe(@RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal User user) throws Exception {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        String original = file.getOriginalFilename();
        String suffix = resolveSuffix(original);

        Path temp = Files.createTempFile("stt_", suffix);
        try {
            file.transferTo(temp.toFile());

            // 변환 + Google STT
            String result = sttService.transcribeLocalFile(temp.toString());
            if (!StringUtils.hasText(result)) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("음성에서 텍스트를 추출하지 못했습니다.");
            }

            Transcription t = Transcription.builder()
                    .fileName(original != null ? original : temp.getFileName().toString())
                    .resultText(result)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build();

            repo.save(t);

            return ResponseEntity.ok(new SttCreateResponse(
                    t.getId(),
                    t.getFileName(),
                    t.getResultText(),
                    t.getCreatedAt()
            ));
        } finally {
            try { Files.deleteIfExists(temp); } catch (Exception ignore) {}
        }
    }

    /** 요약 생성 */
    @PostMapping("/gpt")
    public ResponseEntity<GptResponse> summarize(@RequestParam Long id,
                                                 @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).body(new GptResponse("Unauthorized"));

        return repo.findById(id)
                .map(t -> {
                    if (!t.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(new GptResponse("Forbidden"));
                    }
                    String baseText = t.getResultText();
                    if (!StringUtils.hasText(baseText)) {
                        return ResponseEntity.status(422).body(new GptResponse("요약할 텍스트가 비어 있습니다."));
                    }
                    String summary = sttGptService.summarize(baseText);
                    t.setSummary(summary);
                    repo.save(t);
                    return ResponseEntity.ok(new GptResponse(summary));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(new GptResponse("Not Found")));
    }

    /** 후기 기반 질문 생성 */
    @PostMapping("/gpt/questions")
    public ResponseEntity<GptQuestionsResponse> createQuestions(@RequestParam Long id,
                                                                @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).body(new GptQuestionsResponse(List.of("Unauthorized")));

        return repo.findById(id)
                .map(t -> {
                    if (!t.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(new GptQuestionsResponse(List.of("Forbidden")));
                    }
                    String baseText = t.getResultText();
                    if (!StringUtils.hasText(baseText)) {
                        return ResponseEntity.status(422).body(new GptQuestionsResponse(List.of("질문을 만들 원문이 비어 있습니다.")));
                    }

                    List<String> qs = sttGptService.generateQuestions(baseText);

                    try {
                        // DB에는 JSON 문자열로 저장
                        t.setQuestion(objectMapper.writeValueAsString(qs));
                        repo.save(t);
                    } catch (Exception e) {
                        return ResponseEntity.status(500).body(new GptQuestionsResponse(List.of("저장 오류: " + e.getMessage())));
                    }

                    return ResponseEntity.ok(new GptQuestionsResponse(qs));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(new GptQuestionsResponse(List.of("Not Found"))));
    }

    /** 단건 조회: 질문(JSON) 파싱 포함 */
    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> getOne(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();

        return repo.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(t -> ResponseEntity.ok(toResponse(t)))
                .orElse(ResponseEntity.status(404).build());
    }

    /** 목록 조회: 간단 페이지네이션 with 질문 파싱 */
    @GetMapping("/list")
    public ResponseEntity<List<TranscriptionResponse>> list(@AuthenticationPrincipal User user,
                                                            @RequestParam(required = false, defaultValue = "0") int page,
                                                            @RequestParam(required = false, defaultValue = "50") int size) {
        if (user == null) return ResponseEntity.status(401).build();

        List<Transcription> all = repo.findByUser(user);
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());

        List<TranscriptionResponse> response = all.subList(from, to).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ===== helpers =====

    private static String resolveSuffix(String original) {
        if (original != null && original.lastIndexOf('.') != -1) {
            return original.substring(original.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private TranscriptionResponse toResponse(Transcription t) {
        return new TranscriptionResponse(
                t.getId(),
                t.getFileName(),
                t.getResultText(),
                t.getCreatedAt(),
                t.getSummary(),
                parseQuestions(t.getQuestion())
        );
    }

    private List<String> parseQuestions(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 파싱 실패 시 안전하게 빈 배열 반환
            return Collections.emptyList();
        }
    }
}
