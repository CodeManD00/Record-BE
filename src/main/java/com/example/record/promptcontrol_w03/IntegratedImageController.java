// IntegratedImageController: 사용자의 요청으로부터 프롬프트를 생성하고, 해당 프롬프트 기반 이미지를 생성하여 응답하는 통합 컨트롤러입니다.

package com.example.record.promptcontrol_w03;

import com.example.record.promptcontrol_w03.dto.ImageResponse;
import com.example.record.promptcontrol_w03.dto.PromptRequest;
import com.example.record.promptcontrol_w03.dto.PromptResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate-image") // "/generate-image" 경로로 들어오는 요청 처리
public class IntegratedImageController {

    private final PromptService promptService;   // 프롬프트 생성 서비스
    private final Dalle3Service dalle3Service;   // 이미지 생성 서비스

    // 생성자 주입을 통한 의존성 주입
    public IntegratedImageController(PromptService promptService, Dalle3Service dalle3Service) {
        this.promptService = promptService;
        this.dalle3Service = dalle3Service;
    }

    // POST 요청을 받아 프롬프트 기반 이미지 생성 후 프롬프트+이미지 URL 반환
    @PostMapping
    public ResponseEntity<ImageResponse> generateImage(@RequestBody PromptRequest request) {
        // 프롬프트 문장 생성
        PromptResponse promptResponse = promptService.generatePrompt(request);
        String prompt = promptResponse.getPrompt();

        // 생성된 프롬프트를 기반으로 DALL·E 3 이미지 생성
        String imageUrl = dalle3Service.generateImageUrlOnly(prompt);

        // 응답 객체 구성
        ImageResponse response = new ImageResponse();
        response.setPrompt(prompt);
        response.setImageUrl(imageUrl);

        // 최종 응답 반환 (HTTP 200 OK)
        return ResponseEntity.ok(response);
    }
}
