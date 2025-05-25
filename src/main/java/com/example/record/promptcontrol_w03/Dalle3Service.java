package com.example.record.promptcontrol_w03;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class Dalle3Service {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public Dalle3Service(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // 이미지 URL만 반환하는 메서드
    public String generateImageUrlOnly(String prompt) {
        try {
            String response = webClient.post()
                    .uri("/images/generations")
                    .bodyValue(Map.of(
                            "model", "dall-e-3",
                            "prompt", prompt,
                            "n", 1,
                            "size", "1024x1024"
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.get("data").get(0).get("url").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating image URL";
        }
    }
}
