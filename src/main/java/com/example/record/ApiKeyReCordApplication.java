package com.example.record;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ApiKeyReCordApplication {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final Environment env;

    public ApiKeyReCordApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiKeyReCordApplication.class, args);
    }

    @PostConstruct
    public void checkApiKey() {
        String activeProfile = (env.getActiveProfiles().length > 0)
                ? env.getActiveProfiles()[0]
                : "default";

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️ OpenAI API Key not set");
            return;
        }

        if ("dev".equals(activeProfile)) {
            // 개발 환경 → 마스킹 출력
            String masked = apiKey.length() > 8
                    ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                    : "***";
            System.out.println("✅ [DEV] OpenAI API Key loaded: " + masked);
        } else {
            // 운영 환경 → 출력 안 함
            System.out.println("✅ [PROD] OpenAI API Key loaded (masked)");
        }
    }
}
