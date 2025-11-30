package com.example.record.promptcontrol_w03.service;

import com.example.record.AWS.S3Service;
import com.example.record.review.entity.GeneratedImageUrl;
import com.example.record.review.entity.Review;
import com.example.record.review.repository.GeneratedImageUrlRepository;
import com.example.record.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ReviewRepository reviewRepository;
    private final GeneratedImageUrlRepository generatedImageUrlRepository;
    private final S3Service s3Service;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model.image:dall-e-3}")
    private String imageModel;   // ★ 기본값 dall-e-3

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .exchangeStrategies(
                    ExchangeStrategies.builder()
                            .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                            .build()
            )
            .build();

    /**
     * DALL-E 3 이미지 생성 및 DB 저장
     * @param prompt 이미지 생성 프롬프트
     * @param reviewId 리뷰 ID (리뷰와 연결하여 DB에 저장)
     * @return 생성된 이미지 URL
     */
    @Transactional
    public String generateImage(String prompt, Long reviewId) {

        Map<String, Object> body = Map.of(
                "model", imageModel,          // dall-e-3
                "prompt", prompt,
                "size", "1024x1024",          // DALL-E 3 지원 사이즈: 1024x1024, 1792x1024, 1024x1792
                "quality", "standard",        // standard 또는 hd
                "n", 1,                       // DALL-E 3는 항상 1장만 생성
                "response_format", "b64_json" // base64 인코딩된 이미지 반환 (URL 다운로드 문제 방지)
        );

        System.out.println("📤 BODY => " + body);

        Map<?, ?> response = webClient.post()
                .uri("/images/generations")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(bodyStr -> {
                                    System.err.println("❌ OpenAI Error Response:");
                                    System.err.println(bodyStr);
                                    return new RuntimeException("OpenAI error: " + bodyStr);
                                })
                )
                .bodyToMono(Map.class)
                .block();

        System.out.println("📥 RESPONSE => " + response);

        var dataList = (java.util.List<?>) response.get("data");
        var first = (Map<?, ?>) dataList.get(0);

        // b64_json이 있으면 사용, 없으면 url 사용
        byte[] imageBytes = null;
        if (first.containsKey("b64_json")) {
            // base64 인코딩된 이미지 사용
            String b64Json = first.get("b64_json").toString();
            imageBytes = Base64.getDecoder().decode(b64Json);
            System.out.println("✅ Base64 이미지 사용");
        } else if (first.containsKey("url")) {
            // URL에서 이미지 다운로드 시도
            String originalImageUrl = first.get("url").toString();
            try {
                imageBytes = downloadImageFromUrl(originalImageUrl);
                System.out.println("✅ URL에서 이미지 다운로드 성공");
            } catch (Exception e) {
                System.err.println("⚠️ URL 다운로드 실패, 원본 URL 반환: " + e.getMessage());
                // URL 다운로드 실패 시 원본 URL 반환
                return originalImageUrl;
            }
        } else {
            throw new RuntimeException("이미지 데이터를 찾을 수 없습니다 (url 또는 b64_json 필요)");
        }
        
        // 이미지 크롭 및 저장
        String imageUrl = cropAndSaveImage(imageBytes);
        
        // 리뷰와 연결하여 DB에 저장
        saveImageToDatabase(imageUrl, reviewId);
        
        return imageUrl;
    }

    /**
     * DALL-E 3 이미지 생성 (reviewId 없이, 테스트용)
     * @param prompt 이미지 생성 프롬프트
     * @return 생성된 이미지 URL
     */
    public String generateImageWithoutReview(String prompt) {
        Map<String, Object> body = Map.of(
                "model", imageModel,
                "prompt", prompt,
                "size", "1024x1024",
                "quality", "standard",
                "n", 1,
                "response_format", "b64_json"
        );

        System.out.println("📤 BODY => " + body);

        Map<?, ?> response = webClient.post()
                .uri("/images/generations")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(bodyStr -> {
                                    System.err.println("❌ OpenAI Error Response:");
                                    System.err.println(bodyStr);
                                    return new RuntimeException("OpenAI error: " + bodyStr);
                                })
                )
                .bodyToMono(Map.class)
                .block();

        System.out.println("📥 RESPONSE => " + response);

        var dataList = (java.util.List<?>) response.get("data");
        var first = (Map<?, ?>) dataList.get(0);

        byte[] imageBytes = null;
        if (first.containsKey("b64_json")) {
            String b64Json = first.get("b64_json").toString();
            imageBytes = Base64.getDecoder().decode(b64Json);
            System.out.println("✅ Base64 이미지 사용");
        } else if (first.containsKey("url")) {
            String originalImageUrl = first.get("url").toString();
            try {
                imageBytes = downloadImageFromUrl(originalImageUrl);
                System.out.println("✅ URL에서 이미지 다운로드 성공");
            } catch (Exception e) {
                System.err.println("⚠️ URL 다운로드 실패, 원본 URL 반환: " + e.getMessage());
                return originalImageUrl;
            }
        } else {
            throw new RuntimeException("이미지 데이터를 찾을 수 없습니다 (url 또는 b64_json 필요)");
        }
        
        // 이미지 크롭 및 저장 (DB 저장 없음)
        return cropAndSaveImage(imageBytes);
    }

    /**
     * 생성된 이미지를 리뷰와 연결하여 DB에 저장
     * @param imageUrl 생성된 이미지 URL
     * @param reviewId 리뷰 ID
     */
    private void saveImageToDatabase(String imageUrl, Long reviewId) {
        // 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: id=" + reviewId));

        // GeneratedImageUrl 엔티티 생성
        GeneratedImageUrl generatedImage = GeneratedImageUrl.builder()
                .review(review)
                .imageUrl(imageUrl)
                .isSelected(false)
                .build();

        // DB에 저장
        generatedImageUrlRepository.save(generatedImage);
        
        System.out.println("✅ 이미지 DB 저장 완료: reviewId=" + reviewId + ", imageUrl=" + imageUrl);
    }

    /**
     * URL에서 이미지 다운로드 (User-Agent 헤더 포함)
     */
    private byte[] downloadImageFromUrl(String imageUrl) {
        return webClient.get()
                .uri(imageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    /**
     * 이미지를 4:5 비율로 크롭한 후 S3에 저장
     * @param imageBytes 원본 이미지 바이트 배열
     * @return 크롭된 이미지의 S3 URL
     */
    private String cropAndSaveImage(byte[] imageBytes) {
        try {
            // 1. BufferedImage로 변환
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new RuntimeException("이미지 파싱 실패");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 2. 4:5 비율로 크롭 계산
            // 4:5 = 가로:세로 = 4:5
            // 가로를 기준으로 세로를 계산: 세로 = 가로 * 5/4
            // 세로를 기준으로 가로를 계산: 가로 = 세로 * 4/5
            int targetWidth, targetHeight;
            int x = 0, y = 0;

            if (originalWidth * 5 <= originalHeight * 4) {
                // 원본이 더 세로로 길면 → 가로를 기준으로 크롭
                targetWidth = originalWidth;
                targetHeight = originalWidth * 5 / 4;
                y = (originalHeight - targetHeight) / 2; // 상하 중앙 정렬
            } else {
                // 원본이 더 가로로 길면 → 세로를 기준으로 크롭
                targetHeight = originalHeight;
                targetWidth = originalHeight * 4 / 5;
                x = (originalWidth - targetWidth) / 2; // 좌우 중앙 정렬
            }

            // 3. 크롭 실행
            BufferedImage croppedImage = originalImage.getSubimage(x, y, targetWidth, targetHeight);

            // 4. 크롭된 이미지를 PNG로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(croppedImage, "png", baos);
            byte[] croppedBytes = baos.toByteArray();

            // 5. S3에 저장
            String url = s3Service.saveGeneratedImage(croppedBytes);
            System.out.println("✅ 이미지 크롭 및 S3 업로드 완료: " + url + " (원본: " + originalWidth + "x" + originalHeight + " → 크롭: " + targetWidth + "x" + targetHeight + ")");
            return url;

        } catch (IOException e) {
            System.err.println("❌ 이미지 크롭 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("이미지 크롭 실패: " + e.getMessage(), e);
        }
    }
}
