package com.example.record.promptcontrol_w03;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
public class Dalle3Service {

    private static final String FIXED_SIZE = "1024x1024"; // ✅ 1:1 고정
    private final WebClient openAi;
    private final ObjectMapper om = new ObjectMapper();

    public Dalle3Service(WebClient openAiWebClient) {
        this.openAi = openAiWebClient;
    }

    /** ✅ 단일 이미지만 생성 (항상 1장, 1:1 사이즈 고정) */
    public String generateSingleImageUrl(String prompt) {
        try {
            String res = openAi.post()
                    .uri("/images/generations")
                    .bodyValue(Map.of(
                            "model", "dall-e-3",
                            "prompt", prompt,
                            "size", FIXED_SIZE
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block();

            JsonNode root = om.readTree(res);
            return root.path("data").path(0).path("url").asText();
        } catch (Exception e) {
            throw new RuntimeException("Image generation failed", e);
        }
    }

    /** (하위 호환) 기존 사용처가 있으면 내부 위임 */
    public String generateImageUrlOnly(String prompt) {
        return generateSingleImageUrl(prompt);
    }
}
