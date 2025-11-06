package com.example.record.promptcontrol_w03.service;
/*
역할: OpenAI 이미지 생성 호출 전담.
핵심 기능
단일 이미지(1장) & 4:5 비율 고정 생성 (1080x1350로 추정)
WebClient로 /images/generations 호출, timeout(60s) + 백오프 retry(2회) 적용
응답에서 data[0].url 우선 사용, 없으면 b64_json 처리 분기
TODO: b64_json 반환 시 파일 저장/S3 업로드 후 공개 URL 반환 구현 미완(예외 던짐)
호환 메서드: generateImageUrlOnly()는 위 메서드의 alias
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Gpt1PicService {

    /** 인스타그램 4:5 고정 */
    private static final String FIXED_SIZE = "1080x1350";

    @Value("${openai.limits.imagePromptMaxChars:900}")
    private int imagePromptMaxChars;

    @Value("${openai.url.images}")
    private String imagesUrl;

    private final WebClient openAiWebClient;
    private final ObjectMapper om;

    /** gpt-image-1로 단일 이미지 생성 → URL 반환 (b64 수신 시 예외 처리/후속 구현 지점 명시) */
    public String generateSingleImageUrl(String prompt) {
        try {
            String safePrompt = prompt == null ? "" :
                    (prompt.length() <= imagePromptMaxChars ? prompt : prompt.substring(0, imagePromptMaxChars));

            String res = openAiWebClient.post()
                    .uri(imagesUrl)
                    .bodyValue(Map.of(
                            "model", "gpt-image-1",
                            "prompt", safePrompt,
                            "size", FIXED_SIZE,
                            "n", 1
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block();

            JsonNode root = om.readTree(res);
            JsonNode first = root.path("data").path(0);

            String url = first.path("url").asText(null);
            if (url != null && !url.isBlank()) return url;

            String b64 = first.path("b64_json").asText(null);
            if (b64 != null && !b64.isBlank()) {
                // TODO: b64 → 파일 저장 또는 S3 업로드 후 공개 URL 반환
                throw new UnsupportedOperationException("b64_json returned; implement upload & return public URL.");
            }

            throw new RuntimeException("Image generation failed: no url/b64_json in response");
        } catch (Exception e) {
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    /** (호환) 기존 사용처 대체 */
    public String generateImageUrlOnly(String prompt) {
        return generateSingleImageUrl(prompt);
    }
}
