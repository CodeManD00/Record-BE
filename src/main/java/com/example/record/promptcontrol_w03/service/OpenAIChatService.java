package com.example.record.promptcontrol_w03.service;
/*
역할: OpenAI Chat Completions 호출 공통 유틸.

핵심 기능
WebClient로 /chat/completions 호출, timeout(30s) + 백오프 retry(2회), 에러 시 응답 바디 포함해 예외 래핑
model, temperature, max_tokens(옵션), messages(system/user) 구성
첫 choice의 message.content를 반환(비어있으면 예외)
DTO: ChatRequest/Message/ChatResponse 내부 정적 클래스
 */
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

    private final WebClient openAIWebClient;

    @Value("${openai.url.chat}")
    private String chatUrl;

    @Value("${openai.model.chat}")
    private String chatModel;

    public String complete(String systemPrompt, String userPrompt) {
        var req = new ChatRequest();
        req.model = chatModel;
        req.temperature = 0.7;
        req.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        );

        try {
            ChatResponse res = openAIWebClient.post()
                    .uri(chatUrl)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("no body")
                                    .map(body -> new RuntimeException("OpenAI chat error " + r.statusCode() + " :: " + body)))
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block();

            if (res == null || res.choices == null || res.choices.isEmpty() || res.choices.get(0).message == null) {
                throw new RuntimeException("OpenAI chat: empty response");
            }
            return res.choices.get(0).message.content.trim();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI chat call failed: " + e.getMessage(), e);
        }
    }

    // ===== DTOs =====
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
        public String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
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
