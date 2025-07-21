// SttGptService: 공연 관람 후 음성 텍스트를 GPT에게 보내 요약 및 질문 생성을 수행하는 서비스 클래스입니다.

package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttGptService {

    @Value("${openai.api-key}")
    private String apiKey;

    // WebClient 초기화 (baseUrl 및 Authorization 헤더 설정)
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();

    // STT 텍스트를 요약 요청하는 메서드
    public String summarize(String transcript) {
        String prompt = "다음은 공연 관람 후 음성 기록입니다. 핵심 내용을 요약해주세요:\n" + transcript;
        return callChatGpt(prompt);
    }

    // STT 텍스트 기반으로 후기 작성을 위한 질문을 생성 요청하는 메서드
    public String generateQuestion(String transcript) {
        String prompt = "다음은 공연 관람 후 기록입니다. 관객이 후기 작성에 참고할 만한 질문을 하나 만들어주세요:\n" + transcript;
        return callChatGpt(prompt);
    }

    // GPT API 호출 공통 메서드 (요약 및 질문 요청에 사용)
    private String callChatGpt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // 동기 방식

        try {
            // 응답에서 message → content 추출
            return ((Map)((Map)((List<?>) response.get("choices")).get(0)).get("message"))
                    .get("content").toString().trim();
        } catch (Exception e) {
            return "GPT 응답 처리 실패: " + e.getMessage();
        }
    }
}
