package com.example.record.promptcontrol_w03;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ReviewAnalysisService {

    private final WebClient openAi;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReviewAnalysisService(WebClient openAiWebClient) {
        this.openAi = openAiWebClient;
    }

    /** 모델이 코드펜스/설명을 섞어도 "순수 JSON"만 추출 */
    private static String extractJson(String content) {
        if (content == null || content.isEmpty()) return "{}";

        // 1) 코드펜스 제거
        if (content.startsWith("```")) {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) return content.substring(start, end + 1);
        }
        // 2) 선행/후행 텍스트 제거 (가장 바깥 {…} 구간 추출)
        int s = content.indexOf('{');
        int e = content.lastIndexOf('}');
        return (s >= 0 && e > s) ? content.substring(s, e + 1) : content;
    }

    // 공연 후기를 GPT에게 분석 요청하고, 결과를 JSON → Map<String, Object> 형태로 반환
    // 변경 사항: 분석 결과를 영어로 반환하도록 프롬프트 수정
    // 이유: DALL-E 3는 영어 프롬프트만 지원하므로, 프롬프트 생성 시 한글을 영어로 변환하는 과정을 간소화하기 위함
    public Map<String, Object> analyzeReview(String reviewText) {
        String prompt = """
            Analyze the following performance review and return ONLY JSON (no explanations, no code blocks).
            Keys: emotion, theme, setting, relationship, actions, character1, character2, (character3, character4 if available), lighting
            IMPORTANT: Return all values in ENGLISH only. Translate Korean words/phrases to English.
            Review: %s
        """.formatted(reviewText);

        Map<String, Object> body = Map.of(
                "model", "gpt-4", // 필요시 gpt-4o-mini 등으로 조정
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7
        );

        String response = openAi.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .block();

        try {
            JsonNode json = mapper.readTree(response);
            String content = json.at("/choices/0/message/content").asText();
            String jsonOnly = extractJson(content);
            return mapper.readValue(jsonOnly, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("error", "JSON 파싱 실패", "raw", response);
        }
    }
}
