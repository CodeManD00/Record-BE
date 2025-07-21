// Dalle3Service: OpenAI의 DALL·E 3 API를 호출해 이미지를 생성하고, 해당 이미지의 URL을 반환하는 서비스 클래스입니다.

package com.example.record.promptcontrol_w03;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service // 이 클래스를 Spring의 서비스 컴포넌트로 등록
public class Dalle3Service {

    private final WebClient webClient;       // 외부 API 요청을 위한 HTTP 클라이언트
    private final ObjectMapper objectMapper; // JSON 파싱을 위한 도구
    private final String apiKey;             // OpenAI API 키

    // 생성자에서 WebClient 및 ObjectMapper 초기화
    public Dalle3Service(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1") // OpenAI API base URL
                .defaultHeader("Authorization", "Bearer " + apiKey) // 인증 헤더
                .defaultHeader("Content-Type", "application/json") // JSON 전송 명시
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // 프롬프트를 기반으로 이미지 생성 요청을 보내고, 결과로 받은 이미지 URL을 반환
    public String generateImageUrlOnly(String prompt) {
        try {
            // POST 요청 전송 및 응답 수신 (동기 방식으로 block)
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

            // 응답 JSON에서 이미지 URL 추출
            JsonNode root = objectMapper.readTree(response);
            return root.get("data").get(0).get("url").asText();

        } catch (Exception e) {
            // 예외 발생 시 로그 출력 및 에러 메시지 반환
            e.printStackTrace();
            return "Error generating image URL";
        }
    }
}
