package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    // 큰 응답도 수용 가능하도록 buffer 조정 + 타임아웃/리트라이
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer ")
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024)) // 8MB
                    .build())
            .build();

    public String summarize(String transcript) {
        String prompt = """
                다음은 공연 관람 후 음성 기록입니다. 핵심 내용을 3~5문장으로 간결하고 자연스럽게 요약해 주세요.
                - 불필요한 중복 제거
                - 감상 포인트/인상 깊은 장면/배우·연출 특징 중심
                - 존댓말로 마무리 한 문장 포함
                
                원문:
                """ + transcript;

        return callChatGpt(prompt);
    }

    @SuppressWarnings("unchecked")
    private String callChatGpt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", chatModel,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.4
        );

        Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/chat/completions").build())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(apiKey))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(400)))
                .block();

        try {
            Object choices = response.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> mp) {
                    Object msg = mp.get("message");
                    if (msg instanceof Map<?, ?> m2) {
                        Object content = m2.get("content");
                        if (content != null) return content.toString().trim();
                    }
                }
            }
            return "GPT 응답이 비어 있습니다.";
        } catch (Exception e) {
            return "GPT 응답 처리 실패: " + e.getMessage();
        }
    }
}
