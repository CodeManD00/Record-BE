package com.example.record.promptcontrol_w03.dto;
/*
역할: OpenAI 호출용 WebClient Bean 제공.

구성

baseUrl(예: https://api.openai.com/v1), Authorization: Bearer ${OPENAI_API_KEY}, Content-Type: application/json

대용량 응답 대비 maxInMemorySize 상향(16MB)
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAIClientConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * ObjectMapper는 JacksonConfig에서 @Primary로 등록된 빈을 사용합니다.
     * 이 클래스에서는 WebClient만 제공합니다.
     */

    @Bean
    public WebClient openAiWebClient() {
        // 이미지 응답 등 대용량 대비 메모리 상향 (필요시)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .exchangeStrategies(strategies)
                .build();
    }
}
