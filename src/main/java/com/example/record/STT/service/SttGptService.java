
package com.example.record.STT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${openai.model.chat:gpt-4o-mini}")
    private String chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                    .build())
            .build();

    /** 공연 감상 요약 */
    public String summarize(String transcript) {
        String prompt = """
                다음은 공연 관람 직후의 음성 기록을 옮긴 원문입니다. 보기 좋게 '요약'만 만들어 주세요.
                - 중복/군더더기 제거
                - 인상 포인트, 장면, 배우/연출 특징 중심
                - 사용자의 어투는 과하지 않게 유지

                원문:
                """ + transcript;

        return callChatGpt(prompt);
    }

    /** 산만한 텍스트를 정돈된 후기(문어체)로 개선 */
    public String improveReview(String roughText) {
        String prompt = """
                다음은 공연 직후 녹음한 즉흥적인 감상입니다.
                이를 SNS나 블로그에 올릴 수 있는 정돈된 후기로 다듬어 주세요.
                - 원본 감정과 내용 유지
                - 문법/문장 구조 개선
                - 구어체를 자연스러운 문어체로

                원문:
                """ + roughText;

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
                .uri("/chat/completions")
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
