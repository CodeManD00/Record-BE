package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PromptService {

    private final ReviewAnalysisService reviewAnalysisService;

    public PromptService(ReviewAnalysisService reviewAnalysisService) {
        this.reviewAnalysisService = reviewAnalysisService;
    }

    private static String clamp(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }

    public PromptResponse generatePrompt(PromptRequest input) {
        String genre = input.getGenre();
        String basePrompt;

        if ("뮤지컬".equals(genre)) {
            basePrompt = generateMusicalPrompt(input);
        } else if ("밴드".equals(genre)) {
            basePrompt = generateBandPrompt(input);
        } else {
            throw new IllegalArgumentException("지원하지 않는 장르입니다: " + genre);
        }

        // 옵션 후처리 적용 (variantIndex=0)
        String afterOptions = PromptBuilder.applyOptions(basePrompt, input.getOptions(), 0);

        // ✅ 길이 관리 (과도하게 길면 품질 저하 방지)
        String finalPrompt = clamp(afterOptions, 1400);

        PromptResponse response = new PromptResponse();
        response.setPrompt(finalPrompt);

        Map<String, Object> meta = new HashMap<>();
        meta.put("structure", genre);
        meta.put("style", "gothic");
        meta.put("tone", "emotional");
        meta.put("inferred_keywords", new String[] { "obsession", "conflict" });
        response.setMeta(meta);

        return response;
    }

    // 뮤지컬: 후기 분석 기반
    private String generateMusicalPrompt(PromptRequest input) {
        Map<String, Object> data = reviewAnalysisService.analyzeReview(input.getReview());

        String characterPart = String.format("%s and %s", data.get("character1"), data.get("character2"));
        for (int i = 3; i <= 5; i++) {
            String key = "character" + i;
            if (data.containsKey(key)) characterPart += ", and " + data.get(key);
        }

        return String.format("""
                A %s musical theater scene about %s,
                set in %s and depicting %s,
                featuring %s,
                with %s,
                under %s.
                No captions, no letters, no words, no logos, no watermarks.
                """,
                data.get("emotion"),
                data.get("theme"),
                data.get("setting"),
                data.get("relationship"),
                characterPart,
                data.get("actions"),
                data.get("lighting")
        );
    }

    // 밴드: 고정 포맷
    private String generateBandPrompt(PromptRequest input) {
        return String.format(
                "A moody alternative rock live performance scene by %s,\n" +
                        "featuring a powerful set with emotional lyrics,\n" +
                        "set during autumn,\n" +
                        "at %s on %s,\n" +
                        "with a stage design inspired by %s's concert visuals,\n" +
                        "including deep blue and purple lighting, fog machines and backlights,\n" +
                        "without showing any characters or text.\n" +
                        "No captions, no letters, no words, no logos, no watermarks.",
                input.getTitle(), input.getLocation(), input.getDate(), input.getTitle()
        );
    }
}
