// ApiKeyReCordApplication: Spring Boot 애플리케이션의 진입점이며, OpenAI API 키가 정상적으로 로딩되는지 확인합니다.

package com.example.record;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 자동 설정 + 컴포넌트 스캔을 활성화하는 애플리케이션 시작 어노테이션
public class ApiKeyReCordApplication {

    // application.properties에서 OpenAI API 키 주입
    @Value("${openai.api.key}")
    private String apiKey;

    // 애플리케이션 시작 메서드
    public static void main(String[] args) {
        SpringApplication.run(ApiKeyReCordApplication.class, args);
    }

    // 애플리케이션 실행 직후 호출됨 (API 키 로딩 여부 확인)
    @PostConstruct
    public void checkApiKey() {
        System.out.println("✅ Loaded API Key: " + apiKey);
    }
}
