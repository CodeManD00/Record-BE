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

    /** 인스타그램 4:5 비율에 가장 가까운 DALL-E 3 지원 크기 */
    private static final String FIXED_SIZE = "1024x1792";  // DALL-E 3 지원 크기: 1024x1024, 1792x1024, 1024x1792

    @Value("${openai.limits.imagePromptMaxChars:900}")
    private int imagePromptMaxChars;

    @Value("${openai.url.image}")
    private String imagesUrl;

    private final WebClient openAiWebClient;
    private final ObjectMapper om;

    /** DALL-E 3로 단일 이미지 생성 → URL 반환 (b64 수신 시 예외 처리/후속 구현 지점 명시) */
    public String generateSingleImageUrl(String prompt) {
        try {
            String safePrompt = prompt == null ? "" :
                    (prompt.length() <= imagePromptMaxChars ? prompt : prompt.substring(0, imagePromptMaxChars));

            // OpenAI DALL-E 3 API 요청 형식
            // 참고: 
            // - DALL-E 3는 model 파라미터를 사용하며, size는 "1024x1024", "1792x1024", "1024x1792"만 지원
            // - DALL-E 2는 model 파라미터를 사용하지 않으며, size는 "256x256", "512x512", "1024x1024" 지원
            // - DALL-E 3는 n 파라미터를 지원하지 않음 (항상 1개만 생성)
            // - 여기서는 DALL-E 3를 사용하되, 1080x1350은 지원되지 않으므로 가장 가까운 "1024x1792" 사용
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", "dall-e-3");  // DALL-E 3 모델 사용 (gpt-image-1은 존재하지 않음)
            requestBody.put("prompt", safePrompt);
            requestBody.put("size", FIXED_SIZE);  // DALL-E 3 지원 크기: 1024x1024, 1792x1024, 1024x1792 (4:5 비율에 가장 가까움)
            requestBody.put("quality", "standard");  // "standard" 또는 "hd" (고해상도, 선택사항)
            // n 파라미터는 DALL-E 3에서 지원하지 않으므로 제외 (항상 1개만 생성)
            
            String res = openAiWebClient.post()
                    .uri(imagesUrl)
                    .bodyValue(requestBody)
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
