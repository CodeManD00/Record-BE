// SttGptService: STT 결과를 기반으로 GPT에게 요약만 요청하고, 결과를 DB에 저장하는 서비스 클래스입니다.

package com.example.record.STT;

import com.example.record.DB.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttGptService {

    @Value("${openai.api-key}")
    private String apiKey;

    private final TranscriptionRepository transcriptionRepository;

    // WebClient: GPT API 호출용 클라이언트
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    // STT 결과 텍스트를 기반으로 GPT에게 요약을 요청하는 메서드
    public String summarize(String transcript) {
        String prompt = "다음은 공연 관람 후 음성 기록입니다. 핵심 내용을 요약해주세요:\n" + transcript;
        return callChatGpt(prompt);
    }

    // GPT API 호출 메서드 (요약 요청에 사용됨)
    private String callChatGpt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        try {
            return ((Map)((Map)((List<?>) response.get("choices")).get(0)).get("message"))
                    .get("content").toString().trim();
        } catch (Exception e) {
            return "GPT 응답 처리 실패: " + e.getMessage();
        }
    }

    // 요약 결과를 Transcription 엔티티에 저장하고 클라이언트용 응답을 반환
    public GptResponse generateAndSave(String transcript, String fileName, User user) {
        String summary = summarize(transcript);

        Transcription transcription = Transcription.builder()
                .fileName(fileName)
                .resultText(transcript)
                .summary(summary)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        transcriptionRepository.save(transcription);
        return new GptResponse(summary);
    }
}
