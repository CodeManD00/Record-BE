package com.example.record.promptcontrol_w03.controlbuild;
/*
역할: 이미지 생성 API 엔드포인트.

엔드포인트

POST /generate-image : 실제 생성

POST /generate-image/test : 더미(placeholder) 이미지 응답 (API 키/연동 이슈 시 테스트용)

로직

PromptService로 최종 프롬프트 생성

Gpt1PicService.generateSingleImageUrl() 호출로 4:5 단일 이미지 생성

ImageResponse(prompt, imageUrl) 반환

특징: 프롬프트/길이 디버그 출력, 예외 시 400/500 처리
 */
import com.example.record.promptcontrol_w03.dto.ImageResponse;
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import com.example.record.promptcontrol_w03.service.Gpt1PicService;
import com.example.record.promptcontrol_w03.service.PromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate-image")
public class ImageController {

    private final PromptService promptService;
    private final Gpt1PicService gpt1PicService;

    public ImageController(PromptService promptService, Gpt1PicService gpt1PicService) {
        this.promptService = promptService;
        this.gpt1PicService = gpt1PicService;
    }

    @PostMapping
    public ResponseEntity<ImageResponse> generateImage(@RequestBody PromptRequest request) {
        try {
            // (선택) 장르 검증: 뮤지컬/밴드 아니면 400
            if (request.getGenre() != null &&
                    !(request.getGenre().equals("뮤지컬") || request.getGenre().equals("밴드"))) {
                return ResponseEntity.badRequest().build();
            }

            // 1) 프롬프트 생성
            PromptResponse promptResponse = promptService.generatePrompt(request);
            String prompt = promptResponse.getPrompt();

            // 디버깅: 최종 프롬프트 로그 출력
            System.out.println("🔍 [DEBUG] 최종 프롬프트 (GPT-1로 전송 전):");
            System.out.println(prompt);
            System.out.println("🔍 [DEBUG] 프롬프트 길이: " + prompt.length() + " 문자");

            // 2) 단일 이미지 생성 (항상 1장, 4:5)
            String imageUrl = gpt1PicService.generateSingleImageUrl(prompt);

            // 3) 응답
            ImageResponse response = new ImageResponse();
            response.setPrompt(prompt);
            response.setImageUrl(imageUrl);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // 🧪 임시 테스트 엔드포인트 (API 키 문제 해결 전까지 사용)
    @PostMapping("/test")
    public ResponseEntity<ImageResponse> generateImageTest(@RequestBody PromptRequest request) {
        try {
            // 더미 응답 반환 (실제 API 호출 없이)
            ImageResponse response = new ImageResponse();
            response.setPrompt("테스트 프롬프트: " + request.getTitle() + " - " + request.getReview());
            response.setImageUrl("https://via.placeholder.com/1080x1350/FF6B6B/FFFFFF?text=Test+Image");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}