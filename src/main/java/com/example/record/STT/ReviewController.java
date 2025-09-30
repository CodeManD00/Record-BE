package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final SttGptService sttGptService;       // 요약/정리
    private final TranscriptionRepository repo;       // STT/요약 저장소

    /**
     * 요약 생성
     * - transcriptionId가 있으면 해당 레코드 summary 갱신 후 TranscriptionResponse 스냅샷 반환
     * - 없으면 rawText로 요약만 수행
     */
    @PostMapping("/summaries")
    public ResponseEntity<?> summarize(@RequestBody SummarizeRequest req,
                                       @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        if (req.transcriptionId() != null) {
            var opt = repo.findById(req.transcriptionId());
            if (opt.isEmpty() || !opt.get().getUser().equals(user)) {
                return ResponseEntity.status(404).body("Transcription not found");
            }
            String base = StringUtils.hasText(req.rawText())
                    ? req.rawText()
                    : opt.get().getResultText();

            if (!StringUtils.hasText(base)) {
                return ResponseEntity.badRequest().body("No text to summarize");
            }

            String summary = sttGptService.summarize(base);
            opt.get().setSummary(summary);
            repo.save(opt.get());

            return ResponseEntity.ok(toResponse(opt.get()));
        }

        // transcriptionId 없이 단독 요약
        if (!StringUtils.hasText(req.rawText())) {
            return ResponseEntity.badRequest().body("rawText or transcriptionId is required");
        }
        String summary = sttGptService.summarize(req.rawText());
        return ResponseEntity.ok(new SummaryResponse(null, summary));
    }

    /**
     * 최종 후기 생성(간단판)
     * - transcriptionId 필수
     * - summary(+extraNotes)를 다듬어 finalReview 생성
     * - 스키마상 별도 필드가 없으므로 summary 필드에 최종본 저장(일관 유지)
     */
    @PostMapping("/final")
    public ResponseEntity<?> finalizeReview(@RequestBody FinalizeRequest req,
                                            @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        if (req.transcriptionId() == null) {
            return ResponseEntity.badRequest().body("transcriptionId is required");
        }

        var opt = repo.findById(req.transcriptionId());
        if (opt.isEmpty() || !opt.get().getUser().equals(user)) {
            return ResponseEntity.status(404).body("Transcription not found");
        }

        String base = StringUtils.hasText(opt.get().getSummary())
                ? opt.get().getSummary()
                : opt.get().getResultText();

        if (!StringUtils.hasText(base) && !StringUtils.hasText(req.extraNotes())) {
            return ResponseEntity.badRequest().body("Nothing to finalize");
        }

        String composed = (StringUtils.hasText(base) ? base + "\n\n" : "") +
                (StringUtils.hasText(req.extraNotes()) ? "추가 메모: " + req.extraNotes() : "");

        // 문장 다듬기: QA 통합이 아니므로 improveReview 사용
        String finalReview = sttGptService.improveReview(composed);

        // summary 필드에 최종본 보관(스키마 호환)
        opt.get().setSummary(finalReview);
        repo.save(opt.get());

        return ResponseEntity.ok(
                TranscriptionResponse.builder()
                        .id(opt.get().getId())
                        .fileName(opt.get().getFileName())
                        .createdAt(opt.get().getCreatedAt())
                        .transcript(opt.get().getResultText())
                        .summary(finalReview)     // 최종본
                        .questions(parseQuestions(opt.get().getQuestion()))
                        .finalReview(finalReview) // 편의상 응답에도 노출
                        .build()
        );
    }

    /* ===== 헬퍼 ===== */

    private static List<String> parseQuestions(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        List<String> list = Arrays.stream(raw.split("\\|\\|\\|"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        return list.isEmpty() ? null : list;
    }

    private static TranscriptionResponse toResponse(Transcription t) {
        return TranscriptionResponse.builder()
                .id(t.getId())
                .fileName(t.getFileName())
                .createdAt(t.getCreatedAt())
                .transcript(t.getResultText())
                .summary(t.getSummary())
                .questions(parseQuestions(t.getQuestion()))
                .finalReview(null) // finalReview는 finalize() 응답에서만 채움
                .build();
    }
}
