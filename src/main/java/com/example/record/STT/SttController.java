package com.example.record.STT;

import com.example.record.DB.User;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stt")
public class SttController {

    private final SttService sttService;              // 음성 → 텍스트
    private final SttGptService sttGptService;        // 요약
    private final TranscriptionRepository repo;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> transcribe(@RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal User user) throws Exception {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        // (선택) 대략적인 타입/확장자 체크
        String original = file.getOriginalFilename();
        String suffix = resolveSuffix(original);
        String contentType = file.getContentType();

        Path temp = Files.createTempFile("stt_", suffix);
        try {
            file.transferTo(temp.toFile());

            // 변환+STT
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
                    if (baseText == null || baseText.isBlank()) {
                        return ResponseEntity.status(422).body(new GptResponse("요약할 텍스트가 비어 있습니다."));
                    }
                    String summary = sttGptService.summarize(baseText);
                    t.setSummary(summary);
                    repo.save(t);
                    return ResponseEntity.ok(new GptResponse(summary));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(new GptResponse("Not Found")));
    }

    // 상세 조회(선택): id로 내 기록 하나 가져오기
    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> getOne(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return repo.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(t -> ResponseEntity.ok(new TranscriptionResponse(
                        t.getId(), t.getFileName(), t.getResultText(), t.getCreatedAt(), t.getSummary()
                )))
                .orElse(ResponseEntity.status(404).build());
    }

    // 간단 페이지네이션: ?page=0&size=20 (둘 다 선택)
    @GetMapping("/list")
    public ResponseEntity<List<TranscriptionResponse>> list(@AuthenticationPrincipal User user,
                                                            @RequestParam(required = false, defaultValue = "0") int page,
                                                            @RequestParam(required = false, defaultValue = "50") int size) {
        if (user == null) return ResponseEntity.status(401).build();
        // 간단 구현: findByUser 전체 → stream skip/limit (엔트리 수가 많아지면 Pageable 리포지토리로 전환 권장)
        List<Transcription> all = repo.findByUser(user);
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());

        List<TranscriptionResponse> response = all.subList(from, to).stream()
                .map(t -> new TranscriptionResponse(
                        t.getId(), t.getFileName(), t.getResultText(), t.getCreatedAt(), t.getSummary()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    private static String resolveSuffix(String original) {
        if (original != null && original.lastIndexOf('.') != -1) {
            return original.substring(original.lastIndexOf('.'));
        }
        return ".tmp";
    }
}
