package com.example.record.STT;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WhisperService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.url.transcription:https://api.openai.com/v1/audio/transcriptions}")
    private String transcriptionUrl;

    @Value("${openai.model.transcription:whisper-1}")
    private String model;

    @Value("${openai.limits.whisperMaxFileMB:25}")
    private long maxFileMB;

    private final WebClient webClient = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                    .build())
            .build();

    public String transcribe(byte[] audioBytes, String filename, String language) {
        long limitBytes = maxFileMB * 1024L * 1024L;
        if (audioBytes.length > limitBytes) {
            double sz = Math.round(audioBytes.length / 1024.0 / 1024.0 * 100) / 100.0;
            throw new IllegalArgumentException("파일이 너무 큽니다. (" + sz + "MB) 제한: " + maxFileMB + "MB");
        }

        ByteArrayResource filePart = new ByteArrayResource(audioBytes) {
            @Override public String getFilename() {
                return (filename == null || filename.isBlank()) ? "audio.m4a" : filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);
        body.add("model", model);
        if (language != null && !language.isBlank()) body.add("language", language);
        // body.add("response_format", "json"); // 필요 시 활성화

        return webClient.post()
                .uri(transcriptionUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WhisperResponse.class)
                .timeout(Duration.ofSeconds(120))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .map(WhisperResponse::text)
                .onErrorResume(e -> Mono.error(new RuntimeException("Whisper 요청 실패: " + e.getMessage(), e)))
                .block();
    }

    public record WhisperResponse(String text) {}
}
