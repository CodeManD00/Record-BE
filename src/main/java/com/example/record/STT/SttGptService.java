package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttGptService {

    // ✅ 프로퍼티 키 통일: openai.api.key
    @Value("${openai.api.key}")
    private String apiKey;

    private final TranscriptionRepository transcriptionRepository;

    // 재사용 가능한 WebClient
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    public String summarize(String transcript) {
        String prompt = "다음은 공연 관람 후 음성 기록입니다. 핵심 내용을 3~5문장으로 간결하게 요약해주세요.\n\n" + transcript;
        return callChatGpt(prompt);
    }

    @SuppressWarnings("unchecked")
    private String callChatGpt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "gpt-4o-mini",  // 비용/속도 균형. 필요 시 변경
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.4
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        try {
            return ((Map<?, ?>) ((Map<?, ?>) ((List<?>) response.get("choices")).get(0)).get("message"))
                    .get("content").toString().trim();
        } catch (Exception e) {
            return "GPT 응답 처리 실패: " + e.getMessage();
        }
    }
}
