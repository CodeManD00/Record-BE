// GptClient: OpenAI GPT API를 호출하여 공연 정보를 JSON 형식으로 추출하는 기능을 담당하는 컴포넌트입니다.

package com.example.record.OCR;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GptClient {

    // WebClient: GPT API 호출을 위한 비동기 HTTP 클라이언트
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1/chat/completions")
            .build();

    // application.yml 또는 .properties 파일에서 주입되는 OpenAI API 키
    @Value("${openai.api.key}")
    private String apiKey;

    // 프롬프트를 기반으로 GPT에 요청을 보내고 응답(JSON 문자열)을 반환
    public String getStructuredJsonFromPrompt(String prompt) {
        // GPT 요청에 사용될 JSON 본문 생성
        String requestBody = """
        {
          "model": "gpt-4",
          "messages": [{"role": "user", "content": "%s"}],
          "temperature": 0.3
        }
        """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        // WebClient로 POST 요청 전송 → 응답을 문자열로 반환
        return webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
