// PromptService: 장르(뮤지컬/밴드)에 따라 리뷰 기반 프롬프트를 생성하는 서비스 클래스입니다.

package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import com.example.record.promptcontrol_w03.ReviewAnalysisService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PromptService {

    private final ReviewAnalysisService reviewAnalysisService;

    // 생성자 주입을 통해 ReviewAnalysisService 사용
    public PromptService(ReviewAnalysisService reviewAnalysisService) {
        this.reviewAnalysisService = reviewAnalysisService;
    }

    // 사용자의 요청을 바탕으로 장르별 프롬프트를 생성해 반환
    public PromptResponse generatePrompt(PromptRequest input) {
        String genre = input.getGenre();
        String prompt;

        // 장르에 따라 프롬프트 생성 방식 분기
        if ("뮤지컬".equals(genre)) {
            prompt = generateMusicalPrompt(input);
        } else if ("밴드".equals(genre)) {
            prompt = generateBandPrompt(input);
        } else {
            throw new IllegalArgumentException("지원하지 않는 장르입니다: " + genre);
        }

        // 응답 객체 구성
        PromptResponse response = new PromptResponse();
        response.setPrompt(prompt);

        Map<String, Object> meta = new HashMap<>();
        meta.put("structure", genre);
        meta.put("style", "gothic");
        meta.put("tone", "emotional");
        meta.put("inferred_keywords", new String[]{"obsession", "conflict"});
        response.setMeta(meta);

        return response;
    }

    // 뮤지컬 장르용 프롬프트 생성 (후기 분석 기반)
    private String generateMusicalPrompt(PromptRequest input) {
        Map<String, Object> data = reviewAnalysisService.analyzeReview(input.getReview());

        // 등장 인물 텍스트 구성 (최소 2명 + 최대 5명)
        String characterPart = String.format("%s and %s",
                data.get("character1"),
                data.get("character2")
        );
        for (int i = 3; i <= 5; i++) {
            String key = "character" + i;
            if (data.containsKey(key)) {
                characterPart += ", and " + data.get(key);
            }
        }

        // 최종 프롬프트 생성
        return String.format("""
        A %s musical theater scene about %s,
        set in %s and depicting %s,
        featuring %s,
        with %s,
        under %s.
        No captions, no letters, no text in the image.
        """,
                data.get("emotion"),
                data.get("theme"),
                data.get("setting"),
                data.get("relationship"),
                data.get("actions"),
                characterPart,
                data.get("lighting")
        );
    }

    // 밴드 장르용 프롬프트 생성 (고정 포맷 기반)
    private String generateBandPrompt(PromptRequest input) {
        return String.format(
                "A moody alternative rock live performance scene by %s,\n" +
                        "featuring a powerful set with emotional lyrics,\n" +
                        "set during autumn,\n" +
                        "at %s on %s,\n" +
                        "with a stage design inspired by %s's concert visuals,\n" +
                        "including deep blue and purple lighting, fog machines and backlights,\n" +
                        "without showing any characters or text.\n" +
                        "No captions, no letters, no words in the image.",
                input.getTitle(), input.getLocation(), input.getDate(), input.getTitle()
        );
    }

    // (예정 기능) 후기 기반 주제 추출 (현재는 고정된 예시 반환)
    private String extractTheme(String title, String review) {
        return "obsession and destructive love"; // TODO: 향후 GPT 또는 크롤링 기반으로 대체
    }
}
