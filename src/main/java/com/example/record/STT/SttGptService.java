package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttGptService {

    @Value("${openai.api-key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();

    public String summarize(String transcript) {
        String prompt = "다음은 공연 관람 후 음성 기록입니다. 핵심 내용을 요약해주세요:\n" + transcript;
        return callChatGpt(prompt);
    }

    public String generateQuestion(String transcript) {
        String prompt = "다음은 공연 관람 후 기록입니다. 관객이 후기 작성에 참고할 만한 질문을 하나 만들어주세요:\n" + transcript;
        return callChatGpt(prompt);
    }

    private String callChatGpt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        try {
            return ((Map)((Map)((List<?>) response.get("choices")).get(0)).get("message")).get("content").toString().trim();
        } catch (Exception e) {
            return "GPT 응답 처리 실패: " + e.getMessage();
        }
    }
}
