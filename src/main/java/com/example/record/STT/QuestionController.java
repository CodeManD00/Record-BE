package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final SttGptService sttGptService;
    private final TranscriptionRepository repo;
    private final QuestionService questionService;

    /**
     * GPT로 질문 생성 (메인 엔드포인트)
     * - transcriptionId가 있으면: 해당 STT 텍스트로 질문 생성 후 DB 저장
     * - rawText만 있으면: 질문 생성만 수행 (저장 안 함)
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuestions(@RequestBody GenerateQuestionsRequest req,
                                               @AuthenticationPrincipal com.example.record.user.User user) {
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        // transcriptionId로 생성 및 저장
        if (req.transcriptionId() != null) {
            var opt = repo.findById(req.transcriptionId());
            if (opt.isEmpty() || !opt.get().getUser().equals(user)) {
                return ResponseEntity.status(404).body("Transcription not found");
            }

            String context = opt.get().getResultText();
            if (!StringUtils.hasText(context)) {
                return ResponseEntity.badRequest().body("Transcription has no text");
            }

            List<String> questions = sttGptService.generateQuestions(context);

            // DB에 저장 (||| 구분자)
            opt.get().setQuestion(String.join("|||", questions));
            repo.save(opt.get());

            return ResponseEntity.ok(new QuestionsResponse(opt.get().getId(), questions));
        }

        // rawText로만 질문 생성 (저장 안 함)
        if (!StringUtils.hasText(req.rawText())) {
            return ResponseEntity.badRequest().body("Either transcriptionId or rawText is required");
        }

        List<String> questions = sttGptService.generateQuestions(req.rawText());
        return ResponseEntity.ok(new QuestionsResponse(null, questions));
    }

    /**
     * 카테고리별 프리셋 질문 조회
     * 예: GET /questions/preset?category=뮤지컬
     */
    @GetMapping("/preset")
    public ResponseEntity<?> getPresetQuestions(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(
                new QuestionsResponse(null, questionService.getPresetQuestions(category))
        );
    }

    /**
     * 사용 가능한 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(questionService.getCategories());
    }
}