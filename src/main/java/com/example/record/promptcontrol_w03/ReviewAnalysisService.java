// ReviewAnalysisService: 공연 후기를 GPT-4 API에 보내 분석하고, 프롬프트 생성을 위한 요소들을 JSON 형태로 추출하는 서비스입니다.

package com.example.record.promptcontrol_w03;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ReviewAnalysisService {

    private final WebClient webClient;

    // 생성자에서 WebClient를 초기화하며 OpenAI API 키를 Authorization 헤더에 설정
    public ReviewAnalysisService(@Value("${openai.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // 공연 후기를 GPT-4에게 분석 요청하고, 결과를 JSON → Map<String, Object> 형태로 반환
    public Map<String, Object> analyzeReview(String reviewText) {
        // 후기 분석 요청용 프롬프트 구성
        String prompt = String.format("""
        다음 공연 후기를 분석하여 다음 항목을 JSON 형식으로 추출해줘.
        - emotion
        - theme
        - setting
        - relationship
        - actions
        - character1
        - character2
        - (가능하면 character3, character4 도 포함)
        - lighting

            후기: %s
        """, reviewText);

        // GPT-4 API에 전달할 요청 본문 구성
        Map<String, Object> body = Map.of(
                "model", "gpt-4",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7
        );

        // POST 요청 보내고 응답 문자열 받기
        String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        ObjectMapper mapper = new ObjectMapper();

        try {
            // 응답에서 content(프롬프트 결과 텍스트) 추출
            JsonNode json = mapper.readTree(response);
            String content = json.at("/choices/0/message/content").asText();

            // content 문자열은 JSON 형식이므로 다시 파싱하여 Map으로 반환
            return mapper.readValue(content, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Map.of("error", "JSON 파싱 실패");
        }
    }
}
