package com.example.record.STT;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                    .build())
            .build();

    public String summarize(String transcript) {
        String prompt = """
                다음은 공연 관람 후 음성 기록입니다. 핵심 내용을 3~5문장으로 간결하고 자연스럽게 요약해 주세요.
                - 불필요한 중복 제거
                - 감상 포인트/인상 깊은 장면/배우·연출 특징 중심
                - 존댓말로 마무리 한 문장 포함

                원문:
                """ + transcript;

        return callChatGpt(prompt);
    }

    /** 후기 기반 질문 생성: JSON 배열(List<String>)만 반환하도록 유도 */
    public List<String> generateQuestions(String transcript) {
        String prompt = """
                다음은 한 관객의 공연 관람 후기 원문입니다. 이 후기를 바탕으로
                관람자에게 되돌려 물어볼 수 있는 '깊이 있는 후속 질문'을 한국어로 6~8개 생성하세요.

                출력 형식은 반드시 JSON 배열(List<String>)만으로 주세요. 설명/서론/번호/코드블록 금지.
                질문 톤: 공손하고 대화형(예: "~은 어떠셨나요?", "~가 인상 깊으셨다고 하셨는데, 구체적으로 어떤 점이었나요?").
                질문 범주(섞어서 생성):
                  - 인상 깊은 장면/대사/연출에 대한 구체적 탐색
                  - 배우의 연기·가창·호흡에서 느낀 점
                  - 음악/사운드/무대·조명/의상 등 요소가 감상에 미친 영향
                  - 주제/메시지 해석, 개인적 경험과의 연결
                  - 아쉬웠던 점과 개선 아이디어
                  - 재관람/추천 의향과 그 이유

                원문:
                """ + transcript;

        String content = callChatGptStrictJson(prompt);
        try {
            return objectMapper.readValue(content, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            try {
                int s = content.indexOf('[');
                int eIdx = content.lastIndexOf(']');
                if (s >= 0 && eIdx > s) {
                    String onlyArray = content.substring(s, eIdx + 1);
                    return objectMapper.readValue(onlyArray, new TypeReference<List<String>>() {});
                }
            } catch (Exception ignore) {}
            return List.of("질문 생성에 실패했습니다. 다시 시도해 주세요.");
        }
    }

    @SuppressWarnings("unchecked")
    private String callChatGpt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", chatModel,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.4
        );

        Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/chat/completions").build())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(apiKey))   // ✅ 여기서만 Bearer 설정
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(400)))
                .block();

        try {
            Object choices = response.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> mp) {
                    Object msg = mp.get("message");
                    if (msg instanceof Map<?, ?> m2) {
                        Object content = m2.get("content");
                        if (content != null) return content.toString().trim();
                    }
                }
            }
            return "GPT 응답이 비어 있습니다.";
        } catch (Exception e) {
            return "GPT 응답 처리 실패: " + e.getMessage();
        }
    }

    /** JSON 배열만 반환하도록 더 강하게 유도 */
    @SuppressWarnings("unchecked")
    private String callChatGptStrictJson(String prompt) {
        Map<String, Object> request = Map.of(
                "model", chatModel,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "당신은 JSON 응답 전용 보조자입니다. 오직 JSON 배열만 출력하세요. 설명/코드블록 금지."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/chat/completions").build())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(apiKey))   // ✅ 여기서만 Bearer 설정
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(400)))
                .block();

        try {
            Object choices = response.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> mp) {
                    Object msg = mp.get("message");
                    if (msg instanceof Map<?, ?> m2) {
                        Object content = m2.get("content");
                        if (content != null) return content.toString().trim();
                    }
                }
            }
            return "[]";
        } catch (Exception e) {
            return "[]";
        }
    }
}
