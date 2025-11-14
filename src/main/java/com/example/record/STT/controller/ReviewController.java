package com.example.record.STT.controller;

import com.example.record.STT.dto.FinalizeRequest;
import com.example.record.STT.dto.SummarizeRequest;
import com.example.record.STT.dto.SummaryResponse;
import com.example.record.STT.dto.TranscriptionResponse;
import com.example.record.STT.entres.Transcription;
import com.example.record.STT.entres.TranscriptionRepository;
import com.example.record.STT.service.SttGptService;
import com.example.record.auth.security.AuthUser;
import com.example.record.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final SttGptService sttGptService;       // 요약/개선
    private final TranscriptionRepository repo;      // 저장소

    /**
     * 요약 생성
     * - transcriptionId가 있으면 해당 레코드 summary 갱신 후 스냅샷 반환
     * - 없으면 rawText만 요약해서 반환(저장 없음)
     */
    @PostMapping("/summaries")
    public ResponseEntity<?> summarize(
            @RequestBody SummarizeRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = authUser.getUser();

        // ✅ transcriptionId가 있는 경우: 해당 레코드 찾아서 요약 저장 후 반환
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

        // ✅ transcriptionId가 없고 rawText만 있는 경우: 요약만 생성해서 반환 (DB 저장 X)
        if (!StringUtils.hasText(req.rawText())) {
            return ResponseEntity.badRequest().body("rawText or transcriptionId is required");
        }

        String summary = sttGptService.summarize(req.rawText());
        return ResponseEntity.ok(new SummaryResponse(null, summary));
    }

    /**
     * 최종 후기 생성(간단판)
     * - summary(+extraNotes)를 다듬어 finalReview 생성
     * - 스키마상 별도 필드가 없으므로 summary 필드에 최종본 저장
     */
    @PostMapping("/final")
    public ResponseEntity<?> finalizeReview(
            @RequestBody FinalizeRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = authUser.getUser();

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

        String finalReview = sttGptService.improveReview(composed);

        // ✅ 스키마상 summary 필드에 최종본 저장
        opt.get().setSummary(finalReview);
        repo.save(opt.get());

        return ResponseEntity.ok(
                TranscriptionResponse.builder()
                        .id(opt.get().getId())
                        .fileName(opt.get().getFileName())
                        .createdAt(opt.get().getCreatedAt())
                        .transcript(opt.get().getResultText())
                        .summary(finalReview)
                        .finalReview(finalReview)
                        .build()
        );
    }

    /* ===== 헬퍼 ===== */
    private static TranscriptionResponse toResponse(Transcription t) {
        return TranscriptionResponse.builder()
                .id(t.getId())
                .fileName(t.getFileName())
                .createdAt(t.getCreatedAt())
                .transcript(t.getResultText())
                .summary(t.getSummary())
                .finalReview(null) // finalReview는 finalize 응답에서만 채움
                .build();
    }
}
