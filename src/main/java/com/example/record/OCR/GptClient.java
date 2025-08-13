package com.example.record.OCR;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
@RequiredArgsConstructor
public class GptClient {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // WebClient 재사용 + Authorization 동적 주입
    private final WebClient openAi = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
            .defaultHeader("Content-Type", "application/json")
            .filter((request, next) -> {
                ClientRequest mutated = ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(apiKey))
                        .build();
                return next.exchange(mutated);
            })
            .build();

    /**
     * 프롬프트를 보내고 chat.completions 응답의 choices[0].message.content 텍스트를 반환
     * 파싱 실패 시 원본 응답 문자열을 그대로 반환
     */
    public String getStructuredJsonFromPrompt(String prompt) {
        String body = """
        {
          "model": "gpt-4",
          "messages": [{"role": "user", "content": %s}],
          "temperature": 0.3
        }
        """.formatted(quote(prompt));

        String raw = openAi.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content.isTextual()) return content.asText();
            }
            return raw; // 예상 구조가 아닐 때 원문 반환
        } catch (Exception e) {
            return raw; // 파싱 실패 시 원문 반환
        }
    }

    // JSON 안전 문자열로 감싸기
    private static String quote(String s) {
        if (s == null) return "\"\"";
        // 쌍따옴표/역슬래시/개행 등 이스케이프
        String escaped = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
