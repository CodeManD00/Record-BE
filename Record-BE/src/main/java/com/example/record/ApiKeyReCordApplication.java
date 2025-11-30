package com.example.record;

import com.example.record.AWS.S3Service;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ApiKeyReCordApplication {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final Environment env;
    
    @Autowired(required = false)
    private S3Service s3Service;

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

        // 데이터베이스 연결 정보 확인 (디버깅용)
        String dbUrl = env.getProperty("spring.datasource.url", "NOT_SET");
        System.out.println("=== 데이터베이스 연결 정보 ===");
        System.out.println("DB URL: " + dbUrl);
        System.out.println("DB User: " + env.getProperty("spring.datasource.username", "NOT_SET"));
        if (dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")) {
            System.out.println("⚠️ 경고: 로컬 DB에 연결되어 있습니다!");
        } else if (dbUrl.contains("rds.amazonaws.com")) {
            System.out.println("✅ RDS DB에 연결되어 있습니다.");
        }
        System.out.println("===========================");

        // S3 연결 정보 확인
        System.out.println("=== AWS S3 연결 정보 ===");
        if (s3Service != null) {
            System.out.println("S3 설정: " + s3Service.getConfigInfo());
            boolean s3Connected = s3Service.testConnection();
            if (s3Connected) {
                System.out.println("✅ S3 연결 성공");
            } else {
                System.out.println("❌ S3 연결 실패 - 환경 변수를 확인해주세요.");
            }
        } else {
            System.out.println("⚠️ S3Service가 주입되지 않았습니다.");
        }
        System.out.println("===========================");

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
