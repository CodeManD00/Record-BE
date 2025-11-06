package com.example.record.promptcontrol_w03.dto;
/*
OpenAI API 통신용 WebClient를 설정하는 Spring @Configuration 클래스

baseUrl (예: https://api.openai.com/v1)과 Authorization 헤더(Bearer ${OPENAI_API_KEY})를 미리 세팅

이후 @Autowired 또는 생성자 주입 방식으로 WebClient를 재사용 가능하게 함.
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

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

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
