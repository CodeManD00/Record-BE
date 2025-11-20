package com.example.record.promptcontrol_w03.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Gpt1PicService {

    @Value("${openai.url.image}")
    private String imageUrl;

    @Value("${openai.limits.imagePromptMaxChars:900}")
    private int maxChars;

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    /**
     * gpt-image-1 모델로 단일 이미지 생성(URL 반환)
     */
    public String generateSingleImageUrl(String prompt) {
        try {
            String safePrompt = prompt.length() <= maxChars
                    ? prompt
                    : prompt.substring(0, maxChars);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-image-1");  // gpt-Image 최신 모델
            body.put("prompt", safePrompt);

            // gpt-image-1은 size / n 없음

            String response = openAiWebClient.post()
                    .uri(imageUrl)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode first = root.path("data").path(0);

            // URL 우선
            String url = first.path("url").asText(null);
            if (url != null && !url.isBlank()) {
                return url;
            }

            // b64 처리 (S3 업로드 필요)
            String b64 = first.path("b64_json").asText(null);
            if (b64 != null && !b64.isBlank()) {
                throw new UnsupportedOperationException(
                        "b64_json returned — implement S3 upload and return public URL."
                );
            }

            throw new RuntimeException("Image generation failed: missing url/b64_json");

        } catch (Exception e) {
            throw new RuntimeException("gpt-image-1 generation failed: " + e.getMessage(), e);
        }
    }

    public String generateImageUrlOnly(String prompt) {
        return generateSingleImageUrl(prompt);
    }
}
