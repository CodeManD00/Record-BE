package com.example.record.promptcontrol_w03.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model.image:gpt-image-1}")
    private String imageModel;   // ★ 기본값 gpt-image-1

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .exchangeStrategies(
                    ExchangeStrategies.builder()
                            .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                            .build()
            )
            .build();

    /**
     * GPT-Image-1 이미지 생성
     */
    public String generateImage(String prompt) {

        Map<String, Object> body = Map.of(
                "model", imageModel,          // gpt-image-1
                "prompt", prompt,
                "size", "1080x1350"           // 커스텀 사이즈 허용됨!
        );

        System.out.println("📤 BODY => " + body);

        Map<?, ?> response = webClient.post()
                .uri("/images/generations")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(bodyStr -> {
                                    System.err.println("❌ OpenAI Error Response:");
                                    System.err.println(bodyStr);
                                    return new RuntimeException("OpenAI error: " + bodyStr);
                                })
                )
                .bodyToMono(Map.class)
                .block();

        System.out.println("📥 RESPONSE => " + response);

        var dataList = (java.util.List<?>) response.get("data");
        var first = (Map<?, ?>) dataList.get(0);

        return first.get("url").toString();
    }
}
