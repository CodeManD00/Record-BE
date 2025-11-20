
package com.example.record.STTorText.stt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
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

        // 파일명 처리 및 확장자 확인
        String finalFilename;
        if (filename == null || filename.isBlank()) {
            finalFilename = "audio.m4a";
        } else {
            // 파일명에서 확장자 추출
            String extension = "";
            int lastDot = filename.lastIndexOf('.');
            if (lastDot > 0 && lastDot < filename.length() - 1) {
                extension = filename.substring(lastDot + 1).toLowerCase();
            }
            
            // 지원되는 형식 확인
            String[] supportedFormats = {"flac", "m4a", "mp3", "mp4", "mpeg", "mpga", "oga", "ogg", "wav", "webm"};
            boolean isSupported = false;
            for (String format : supportedFormats) {
                if (format.equals(extension)) {
                    isSupported = true;
                    break;
                }
            }
            
            // 확장자가 없거나 지원되지 않는 형식이면 m4a로 변경
            if (!isSupported) {
                System.out.println("⚠ 파일 확장자가 지원되지 않거나 없습니다. 원본: " + filename + ", 확장자: " + extension);
                if (lastDot > 0) {
                    finalFilename = filename.substring(0, lastDot) + ".m4a";
                } else {
                    finalFilename = filename + ".m4a";
                }
                System.out.println("→ 파일명을 " + finalFilename + "로 변경합니다.");
            } else {
                finalFilename = filename;
            }
        }
        
        System.out.println("=== 파일 정보 ===");
        System.out.println("원본 파일명: " + filename);
        System.out.println("최종 파일명: " + finalFilename);
        System.out.println("파일 크기: " + audioBytes.length + " bytes");

        final String finalFilenameForLambda = finalFilename;
        
        // Content-Type 결정
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        String extension = "";
        int lastDot = finalFilenameForLambda.lastIndexOf('.');
        if (lastDot > 0 && lastDot < finalFilenameForLambda.length() - 1) {
            extension = finalFilenameForLambda.substring(lastDot + 1).toLowerCase();
            switch (extension) {
                case "m4a":
                case "mp4":
                    contentType = MediaType.parseMediaType("audio/mp4");
                    break;
                case "mp3":
                case "mpga":
                    contentType = MediaType.parseMediaType("audio/mpeg");
                    break;
                case "wav":
                    contentType = MediaType.parseMediaType("audio/wav");
                    break;
                case "ogg":
                case "oga":
                    contentType = MediaType.parseMediaType("audio/ogg");
                    break;
                case "webm":
                    contentType = MediaType.parseMediaType("audio/webm");
                    break;
                case "flac":
                    contentType = MediaType.parseMediaType("audio/flac");
                    break;
            }
        }
        
        System.out.println("Content-Type: " + contentType);
        
        ByteArrayResource filePart = new ByteArrayResource(audioBytes) {
            @Override public String getFilename() {
                return finalFilenameForLambda;
            }
        };

        // MultipartBodyBuilder를 사용하여 명확하게 multipart 요청 구성
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", filePart)
                .header("Content-Disposition", "form-data; name=\"file\"; filename=\"" + finalFilenameForLambda + "\"")
                .contentType(contentType);
        bodyBuilder.part("model", model);
        if (language != null && !language.isBlank()) {
            bodyBuilder.part("language", language);
        }

        // API 키가 비어있는지 확인 및 trim 처리 (디버깅용)
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API Key가 설정되지 않았습니다. 환경변수 OPENAI_API_KEY를 확인하세요.");
        }
        
        // API 키 앞뒤 공백 제거 (환경변수 설정 시 공백이 포함될 수 있음)
        apiKey = apiKey.trim();
        
        // 디버깅: API 키 일부만 로그 출력 (보안을 위해 앞 4자리와 뒤 4자리만)
        String maskedKey = apiKey.length() > 8 
                ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                : "***";
        System.out.println("=== Whisper API 호출 ===");
        System.out.println("API Key (마스킹): " + maskedKey);
        System.out.println("API Key 길이: " + apiKey.length());
        System.out.println("URL: " + transcriptionUrl);
        
        return webClient.post()
                .uri(transcriptionUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    System.err.println("=== Whisper API HTTP 오류 ===");
                    System.err.println("HTTP 상태 코드: " + response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(errorBody -> {
                                System.err.println("오류 응답 본문: " + errorBody);
                            })
                            .map(errorBody -> new RuntimeException(
                                    "Whisper API 오류 [" + response.statusCode() + "]: " + errorBody
                            ));
                })
                .bodyToMono(WhisperResponse.class)
                .timeout(Duration.ofSeconds(120))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .doBeforeRetry(retrySignal -> {
                            System.out.println("재시도 중... (시도 횟수: " + retrySignal.totalRetries() + ")");
                        }))
                .map(WhisperResponse::text)
                .onErrorResume(e -> {
                    // 더 자세한 오류 정보를 포함하여 예외 발생
                    System.err.println("=== Whisper API 호출 오류 ===");
                    System.err.println("오류 타입: " + e.getClass().getName());
                    System.err.println("오류 메시지: " + e.getMessage());
                    if (e.getCause() != null) {
                        System.err.println("원인: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();
                    return Mono.error(new RuntimeException("Whisper 요청 실패: " + e.getMessage(), e));
                })
                .block();
    }

    public record WhisperResponse(String text) {}
}
