package com.example.record.promptcontrol_w03.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIChatService {

    private final WebClient openAiWebClient;

    @Value("${openai.url.chat}")
    private String chatUrl;

    @Value("${openai.model.chat}")
    private String model;

    public String complete(String systemPrompt, String userPrompt) {

        ChatRequest req = new ChatRequest();
        req.model = model;
        req.temperature = 0.7;
        req.max_tokens = 200;
        req.messages = List.of(
                Message.text("system", systemPrompt),
                Message.text("user", userPrompt)
        );

        try {
            ChatResponse res = openAiWebClient.post()
                    .uri(chatUrl)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("OpenAI chat error: " + body))
                    )
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block();

            if (res == null || res.choices == null || res.choices.isEmpty()) {
                throw new RuntimeException("Empty OpenAI response");
            }

            return res.choices.get(0).message.content.trim(); // ★ 여기서 content는 문자열

        } catch (Exception e) {
            throw new RuntimeException("OpenAI chat call failed: " + e.getMessage(), e);
        }
    }

    // ========================
    // DTO
    // ========================
    @Data
    static class ChatRequest {
        public String model;
        public List<Message> messages;
        public Double temperature;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Integer max_tokens;
    }

    @Data
    static class Message {
        public String role;
        public String content; // ★ 변경됨

        public static Message text(String role, String text) {
            Message m = new Message();
            m.role = role;
            m.content = text; // ★ 리스트 대신 문자열
            return m;
        }
    }

    @Data
    static class ChatResponse {
        public List<Choice> choices;

        @Data
        static class Choice {
            public Message message;
        }
    }
}
